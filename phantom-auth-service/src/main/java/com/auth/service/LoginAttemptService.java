package com.auth.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {
    private final ConcurrentHashMap<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> blockCache = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_TIME_MS = TimeUnit.MINUTES.toMillis(5);

    public void loginFailed(String username) {
        int attempts = attemptsCache.getOrDefault(username, 0) + 1;
        attemptsCache.put(username, attempts);

        if (attempts >= MAX_ATTEMPTS) {
            blockCache.put(username, System.currentTimeMillis());
        }
    }

    public void loginSucceeded(String username) {
        attemptsCache.remove(username);
        blockCache.remove(username);
    }

    public boolean isBlocked(String username) {
        Long blockTime = blockCache.get(username);
        if (blockTime == null) {
            return false;
        }

        if (System.currentTimeMillis() - blockTime > BLOCK_TIME_MS) {
            blockCache.remove(username);
            attemptsCache.remove(username);
            return false;
        }

        return true;
    }
}