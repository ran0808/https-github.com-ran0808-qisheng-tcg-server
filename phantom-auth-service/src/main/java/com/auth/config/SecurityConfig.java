package com.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    public SecurityConfig() {
        log.info("SecurityConfig初始化完成，启用自定义安全配置");
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // 明确禁用CSRF
                .authorizeHttpRequests(authz -> authz
                        // 精确匹配所有/auth路径（包括子路径）允许匿名访问
                        .requestMatchers("/auth/login", "/auth/register", "/auth/validate").permitAll()
                        .anyRequest().authenticated() // 其他接口需要认证
                )
                .httpBasic(AbstractHttpConfigurer::disable) // 明确禁用HTTP Basic
                .formLogin(AbstractHttpConfigurer::disable); // 明确禁用表单登录

        return http.build();
    }
}