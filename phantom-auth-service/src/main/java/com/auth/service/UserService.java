package com.auth.service;

import com.auth.dto.User;
import com.auth.dto.UserStatus;
import com.auth.exception.AuthException;
import com.auth.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String USER_CACHE_PREFIX = "user:";
    private static final String USERNAME_CACHE_PREFIX = "user:username:";
    private static final Duration USER_CACHE_TTL = Duration.ofHours(1);

    public User verifyCredentials(String username, String password) throws AuthException {

        User user = getUserFromCacheByUsername(username);
        if (user==null){
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new AuthException("用户不存在"));
            cacheUser(user);
        }

        if (!passwordService.verifyPassword(password, user.getPasswordHash())) {
            throw new AuthException("密码错误");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException("账户已被锁定");
        }

        return user;
    }

    public User createUser(String username, String password) throws AuthException {
        if (userRepository.existsByUsername(username)) {
            throw new AuthException("用户名已存在");
        }
        String passwordHash = passwordService.hashPassword(password);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        cacheUser(savedUser);
        return savedUser;
    }

    public User getUserById(String playerId) throws AuthException {
        User user = getUserFromCacheById(playerId);
        if (user==null){
            user = userRepository.findById(playerId)
                    .orElseThrow(() -> new AuthException("用户不存在"));
            cacheUser(user);
        }
        return user;
    }
    private User getUserFromCacheByUsername(String username) {
        String key = USERNAME_CACHE_PREFIX + username;
        return (User) redisTemplate.opsForValue().get(key);
    }

    private User getUserFromCacheById(String userId) {
        String key = USER_CACHE_PREFIX + userId;
        return (User) redisTemplate.opsForValue().get(key);
    }
    private void cacheUser(User user) {
        // 缓存用户ID到对象的映射
        String userKey = USER_CACHE_PREFIX + user.getId();
        redisTemplate.opsForValue().set(userKey, user, USER_CACHE_TTL);
        // 缓存用户名到用户对象的映射
        String usernameKey = USERNAME_CACHE_PREFIX + user.getUsername();
        redisTemplate.opsForValue().set(usernameKey, user, USER_CACHE_TTL);
    }
    public void evictUserFromCache(String userId) {
        // 获取用户信息，以便清除用户名缓存
        User user = getUserFromCacheById(userId);
        if (user != null) {
            String usernameKey = USERNAME_CACHE_PREFIX + user.getUsername();
            redisTemplate.delete(usernameKey);
        }
        // 清除用户ID缓存
        String userKey = USER_CACHE_PREFIX + userId;
        redisTemplate.delete(userKey);
    }
    public void updateUser(User user) {
        User updatedUser = userRepository.save(user);
        // 更新缓存
        cacheUser(updatedUser);
    }
}
