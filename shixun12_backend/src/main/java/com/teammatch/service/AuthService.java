package com.teammatch.service;

import com.teammatch.dto.LoginRequest;
import com.teammatch.dto.PasswordLoginRequest;
import com.teammatch.dto.SendEmailCodeRequest;
import com.teammatch.dto.VerifyEmailCodeRequest;
import com.teammatch.dto.PasswordRequest;
import com.teammatch.dto.UsernameRequest;
import com.teammatch.entity.User;

/**
 * M3 认证服务接口
 */
public interface AuthService {

    /**
     * 微信登录
     * 通过 wx.login() 返回的 code 调用微信 code2session 接口，
     * 获取真实 openid 后进行用户查找或创建。
     */
    User wechatLogin(LoginRequest request);

    /**
     * Mock 登录（仅开发测试用）
     * 直接把 code 当作 openid 查找或创建用户，不调微信接口
     */
    User mockLogin(LoginRequest request);

    /**
     * 密码登录
     */
    User passwordLogin(PasswordLoginRequest request);

    /**
     * 发送邮箱验证码
     */
    void sendEmailCode(SendEmailCodeRequest request, String token);

    /**
     * 验证邮箱验证码
     */
    boolean verifyEmailCode(VerifyEmailCodeRequest request, String token);

    /**
     * 管理员创建用户密码
     * 需要校验当前用户是否为管理员
     */
    void adminCreatePassword(PasswordRequest request, String token);

    /**
     * 管理员修改用户密码
     * 需要校验当前用户是否为管理员
     */
    void adminUpdatePassword(PasswordRequest request, String token);

    /**
     * 用户自己修改密码
     */
    void changePassword(PasswordRequest request, String token);

    /**
     * 用户自己设置用户名（首次绑定）
     */
    void bindUsername(UsernameRequest request, String token);

    /**
     * 用户自己修改用户名
     */
    void updateUsername(UsernameRequest request, String token);

    /**
     * 校验当前用户是否为管理员
     * 
     * @param token JWT Token
     * @return true-是管理员，false-不是管理员
     * @throws RuntimeException Token 无效或用户不存在
     */
    boolean isAdmin(String token);

    /**
     * 生成 JWT Token 并存储到 Redis
     * 
     * @param user 用户实体
     * @return JWT Token 字符串
     */
    String generateToken(User user);

    /**
     * 从 Token 中解析用户 ID
     * 
     * @param token JWT Token（可带 "Bearer " 前缀）
     * @return 用户 ID
     * @throws RuntimeException Token 无效或过期
     */
    Long getUserIdFromToken(String token);
}
