package com.match.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class GameSessionService {

    private static final String GAME_SESSION_PREFIX = "game:session:";
    private static final String PLAYER_SESSION_PREFIX = "player:session:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public String createGameSession(String player1, String player2) {
        // 生成唯一的会话ID
        String sessionId = UUID.randomUUID().toString();

        // 存储会话信息到Redis
        redisTemplate.opsForHash().put(GAME_SESSION_PREFIX + sessionId, "player1", player1);
        redisTemplate.opsForHash().put(GAME_SESSION_PREFIX + sessionId, "player2", player2);
        redisTemplate.opsForHash().put(GAME_SESSION_PREFIX + sessionId, "status", "created");
        redisTemplate.opsForHash().put(GAME_SESSION_PREFIX + sessionId, "createdAt", System.currentTimeMillis());

        // 关联玩家和会话
        redisTemplate.opsForValue().set(PLAYER_SESSION_PREFIX + player1, sessionId);
        redisTemplate.opsForValue().set(PLAYER_SESSION_PREFIX + player2, sessionId);

        return sessionId;
    }

}