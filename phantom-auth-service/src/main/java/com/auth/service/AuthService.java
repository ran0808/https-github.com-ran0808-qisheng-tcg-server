package com.auth.service;

import com.auth.dto.LoginRequest;
import com.auth.dto.LoginResponse;
import com.auth.dto.User;
import com.auth.exception.AuthException;
import com.game.common.security.JwtTokenProvider;
import com.game.common.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private TokenService tokenService;

    public LoginResponse authenticate(LoginRequest request) throws AuthException {
        try {
            validationService.validateLoginRequest(request);
            //检查登录尝试限制
            if (loginAttemptService.isBlocked(request.getUsername())){
                throw new AuthException("账户已锁定，请5分钟后再试");
            }
            // 1. 验证用户凭证
            User user = userService.verifyCredentials(request.getUsername(), request.getPassword());
            //2.生成JWT令牌
            String token = jwtTokenProvider.createToken(user.getId(),user.getUsername());
            tokenService.storeToken(user.getId(), token);
            long expiration = System.currentTimeMillis() + jwtTokenProvider.getExpirationInMs();
            // 3. 记录成功登录
            loginAttemptService.loginSucceeded(request.getUsername());
            return new LoginResponse(
                    user.getId(),
                    token,
                    "登录成功",
                    expiration
            );
        }catch (AuthException e) {
            // 记录失败登录
            loginAttemptService.loginFailed(request.getUsername());
            throw e;
        }
    }
    public LoginResponse register(LoginRequest request) throws AuthException {
        validationService.validateRegisterRequest(request);
        // 1. 创建新用户,保存到数据库
        User newUser = userService.createUser(request.getUsername(), request.getPassword());
        // 2. 生成JWT令牌
        String token = jwtTokenProvider.createToken(newUser.getId(), newUser.getUsername());
        long expiration = System.currentTimeMillis() + jwtTokenProvider.getExpirationInMs();
        return new LoginResponse(
                newUser.getId(),
                token,
                "注册成功",
                expiration
        );
    }
}
