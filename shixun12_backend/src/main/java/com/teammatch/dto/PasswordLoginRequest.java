package com.teammatch.dto;

import lombok.Data;

/**
 * 密码登录请求 DTO
 */
@Data
public class PasswordLoginRequest {
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
}
