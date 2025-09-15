package com.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class PasswordService {
    private final Pbkdf2PasswordEncoder passwordEncoder = new Pbkdf2PasswordEncoder(
            "",
            16,
            310000,
            Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256
    );
    private final Cache<String, Boolean> passwordCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    public boolean verifyPassword(String password, String hashedPassword) {
        String cacheKey = password + "|" + hashedPassword;
        return passwordCache.get(cacheKey, k -> passwordEncoder.matches(password, hashedPassword));
    }
}