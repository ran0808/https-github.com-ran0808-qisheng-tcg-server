package com.game.client.service;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClientValidationService {
    private static final Set<String> RESERVED_USERNAMES = new HashSet<>(
            Arrays.asList("admin", "system", "root", "administrator", "null", "undefined")
    );
    /**
     * 验证用户名格式
     */
    public String validateUsername(String username) {
        if (username==null||username.trim().isEmpty()){
            return "用户名不能为空";
        }
        String trimmedUsername = username.trim();
        if (trimmedUsername.length() < 4 || trimmedUsername.length() > 20) {
            return "用户名长度必须为4-20个字符";
        }

        if (!trimmedUsername.matches("^[a-zA-Z][a-zA-Z0-9_]{3,19}$")) {
            return "用户名必须以字母开头，只能包含字母、数字和下划线";
        }

        if (RESERVED_USERNAMES.contains(trimmedUsername.toLowerCase())) {
            return "该用户名不可使用，请选择其他用户名";
        }

        return null;
    }

    /**
     * 验证密码强度
     */
    public String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }

        if (password.length() < 12) {
            return "密码长度至少12位";
        }

        int diversity = 0;
        if (password.matches(".*[a-z].*")) diversity++;
        if (password.matches(".*[A-Z].*")) diversity++;
        if (password.matches(".*\\d.*")) diversity++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) diversity++;

        if (diversity < 3) {
            return "密码必须包含大小写字母、数字和特殊字符中的至少三类";
        }

        return null;
    }
}
