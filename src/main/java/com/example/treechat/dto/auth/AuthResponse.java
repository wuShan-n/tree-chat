package com.example.treechat.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 认证响应DTO
 */
@Data
@AllArgsConstructor
public class AuthResponse {

    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;
}
