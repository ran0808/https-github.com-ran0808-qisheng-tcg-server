package com.game.common.security;


import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final String TOKEN_CACHE_PREFIX = "token:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    public void storeToken(String userId, String token) {
        String key = TOKEN_CACHE_PREFIX + userId;
        long expiration = jwtTokenProvider.getExpirationInMs() / 1000; // 转换为秒

        redisTemplate.opsForValue().set(key, token, expiration, TimeUnit.SECONDS);
    }

    public boolean isTokenValid(String userId, String token) {
        // 检查令牌是否在黑名单中
        if (isTokenBlacklisted(token)) {
            return false;
        }

        // 检查令牌是否与存储的一致
        String key = TOKEN_CACHE_PREFIX + userId;
        String storedToken = (String) redisTemplate.opsForValue().get(key);

        return token.equals(storedToken) && jwtTokenProvider.validateToken(token);
    }

    public void invalidateToken(String userId, String token) {
        // 将令牌加入黑名单
        addToBlacklist(token);

        // 移除用户的令牌
        String key = TOKEN_CACHE_PREFIX + userId;
        redisTemplate.delete(key);
    }

    private void addToBlacklist(String token) {
        String key = TOKEN_BLACKLIST_PREFIX + token;
        long expiration = getTokenRemainingTime(token);

        if (expiration > 0) {
            redisTemplate.opsForValue().set(key, "blacklisted", expiration, TimeUnit.SECONDS);
        }
    }

    private boolean isTokenBlacklisted(String token) {
        String key = TOKEN_BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(key);
    }

    private long getTokenRemainingTime(String token) {
        try {
            Claims claims = jwtTokenProvider.parseToken(token);
            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();

            return (expiration.getTime() - now) / 1000; // 返回剩余秒数
        } catch (Exception e) {
            return 0;
        }
    }
}