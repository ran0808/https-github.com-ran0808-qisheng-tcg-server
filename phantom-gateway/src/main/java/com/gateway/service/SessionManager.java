package com.gateway.service;

import com.game.common.security.JwtTokenProvider;
import com.gateway.network.util.SendMessage;
import io.jsonwebtoken.Claims;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Data
public class SessionManager {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private RedisTemplate<String,String > redisTemplate;

    private final Map<String, Channel> tokenChannelMap = new ConcurrentHashMap<>();
    private final Map<Channel,String > channelTokenMap = new ConcurrentHashMap<>();

    private static final String REDIS_PLAYER_TOKEN_PREFIX = "player:token:";  // playerId -> token
    private static final String REDIS_TOKEN_PLAYER_PREFIX = "token:player:";  // token -> playerId
    public void registerSession(String token, Channel channel){
        try {
            Claims claims = jwtTokenProvider.parseToken(token);
            String playerId = claims.getId();
            long tokenExpireTime = claims.getExpiration().getTime()-System.currentTimeMillis();

            // 更新内存Map
            tokenChannelMap.put(token, channel);
            channelTokenMap.put(channel, token);

            // 注册到SendMessage
            SendMessage.registerChannelToken(channel, token);

            // 持久化到Redis
            redisTemplate.opsForValue().set(
                    REDIS_PLAYER_TOKEN_PREFIX + playerId,
                    token,
                    tokenExpireTime,
                    TimeUnit.MILLISECONDS
            );
            redisTemplate.opsForValue().set(
                    REDIS_TOKEN_PLAYER_PREFIX + token,
                    playerId,
                    tokenExpireTime,
                    TimeUnit.MILLISECONDS
            );

            log.info("会话注册成功：playerId={}, token={}, channel={}",
                    playerId, token, channel.id());

            // 验证注册是否成功
            String verifyToken = channelTokenMap.get(channel);
            log.info("注册后验证 - 频道 {} 的Token: {}", channel.id(), verifyToken);

            // 记录当前映射状态
            log.info("当前tokenChannelMap大小: {}", tokenChannelMap.size());
            log.info("当前channelTokenMap大小: {}", channelTokenMap.size());

        } catch (Exception e) {
            log.warn("无法从Token中解析玩家ID", e);
        }
    }
    public void removeSession(Channel channel) {
        try {
            String token = channelTokenMap.get(channel);
            if (token != null) {
                channelTokenMap.remove(channel);
                tokenChannelMap.remove(token);

                // 从SendMessage中移除
                SendMessage.unregisterChannelToken(channel);

                log.info("已移除通道 {} 的会话, token: {}", channel.id(), token);

                // 记录移除后的映射状态
                log.info("移除后tokenChannelMap大小: {}", tokenChannelMap.size());
                log.info("移除后channelTokenMap大小: {}", channelTokenMap.size());
            } else {
                log.warn("尝试移除通道 {} 的会话，但未找到对应的token", channel.id());
            }
        } catch (Exception e) {
            log.error("移除会话时发生异常: {}", e.getMessage(), e);
        }
    }
    public String getTokenByPlayerIdFromRedis(String playerId) {
        return redisTemplate.opsForValue().get(REDIS_PLAYER_TOKEN_PREFIX + playerId);
    }
    public String getPlayerIdByTokenFromRedis(String token) {
        return redisTemplate.opsForValue().get(REDIS_TOKEN_PLAYER_PREFIX + token);
    }
    public Channel getChannelByToken(String token) {
        return tokenChannelMap.get(token);
    }

    public String getTokenByChannel(Channel channel) {
        return channelTokenMap.get(channel);
    }
    public String getPlayerIdByChannel(Channel channel) {
        try {
            String token = channelTokenMap.get(channel);
            if (token != null) {
                Claims claims = jwtTokenProvider.parseToken(token);
                return claims.getId();
            }
            return null;
        } catch (Exception e) {
            log.error("获取玩家ID时发生异常: {}", e.getMessage());
            return null;
        }
    }
}
