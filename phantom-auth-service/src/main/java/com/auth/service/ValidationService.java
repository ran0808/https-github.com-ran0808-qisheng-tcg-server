
package com.auth.service;

import com.auth.dto.LoginRequest;
import com.auth.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class ValidationService {

    private static final Set<String> RESERVED_USERNAMES = new HashSet<>(
            Arrays.asList("admin", "system", "root", "administrator", "null", "undefined")
    );
    /**
     * 验证用户名格式
     */
    public void validateUsername(String username) throws AuthException {
        if (!StringUtils.hasText(username)) {
            throw new AuthException("用户名不能为空");
        }

        if (username.length() < 4 || username.length() > 20) {
            throw new AuthException("用户名长度必须为4-20个字符");
        }

        if (!username.matches("^[a-zA-Z][a-zA-Z0-9_]{3,19}$")) {
            throw new AuthException("用户名必须以字母开头，只能包含字母、数字和下划线");
        }

        if (RESERVED_USERNAMES.contains(username.toLowerCase())) {
            throw new AuthException("该用户名不可使用，请选择其他用户名");
        }
    }

    /**
     * 验证密码强度
     */
    public void validatePassword(String password) throws AuthException {
        if (!StringUtils.hasText(password)) {
            throw new AuthException("密码不能为空");
        }

        if (password.length() < 12) {
            throw new AuthException("密码长度至少12位");
        }

        int diversity = 0;
        if (password.matches(".*[a-z].*")) diversity++;
        if (password.matches(".*[A-Z].*")) diversity++;
        if (password.matches(".*\\d.*")) diversity++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) diversity++;

        if (diversity < 3) {
            throw new AuthException("密码必须包含大小写字母、数字和特殊字符中的至少三类");
        }
    }

    /**
     * 验证登录请求
     */
    public void validateLoginRequest(LoginRequest request) throws AuthException {
        validateUsername(request.getUsername());
        // 登录时不需要验证密码格式，只需要验证非空
        if (!StringUtils.hasText(request.getPassword())) {
            throw new AuthException("密码不能为空");
        }
    }

    /**
     * 验证注册请求
     */
    public void validateRegisterRequest(LoginRequest request) throws AuthException {
        validateUsername(request.getUsername());
        validatePassword(request.getPassword());
    }
}