package com.auth.controller;

import com.auth.dto.ErrorResponse;
import com.auth.dto.LoginRequest;
import com.auth.dto.LoginResponse;
import com.auth.exception.AuthException;
import com.auth.service.AuthService;

import com.game.common.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) throws AuthException {
        try {
            LoginResponse response = authService.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (AuthException e) {
            ErrorResponse error = new ErrorResponse("AUTH_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (AuthException e) {
            ErrorResponse error = new ErrorResponse("AUTH_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    @PostMapping("/validate")
    public ResponseEntity<LoginResponse> validateToken(@RequestBody String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.badRequest().body(new LoginResponse(null, null, "Token无效", 0));
        }
        try {
            var claims = jwtTokenProvider.parseToken(token);
            return ResponseEntity.ok(new LoginResponse(
                    claims.getId(),
                    token,
                    "Token有效",
                    claims.getExpiration().getTime()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new LoginResponse(null, null, "Token解析失败", 0));
        }
    }
}