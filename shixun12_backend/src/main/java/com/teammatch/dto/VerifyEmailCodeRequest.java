package com.teammatch.dto;

/**
 * 验证邮箱验证码请求 DTO
 */
public class VerifyEmailCodeRequest {
    private String email;
    private String code;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
