package com.teammatch.util;

import com.teammatch.common.ReasonCode;
import com.teammatch.entity.User;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.exception.AuthorizationException;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 认证工具类
 * 
 * 主要功能：
 * 1. requireUserId - 必须登录，会校验账号状态（被封/禁用即时拦截）
 * 2. getCurrentUserId - 可选登录，失败返回 null
 * 3. requireAdmin - 先验登录再验 admin，分别 M3000 / M3009
 */
@Slf4j
@Component
public class AuthUtil {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取当前用户 ID，token 无效时返回 null（可选登录）
     * 
     * 适用场景：
     * - 需要优雅处理 token 无效的情况
     * - 不想使用 try-catch
     * 
     * @param token JWT Token（可带 "Bearer " 前缀）
     * @return 用户 ID，token 无效时返回 null
     */
    public Long getCurrentUserId(String token) {
        try {
            return authService.getUserIdFromToken(token);
        } catch (RuntimeException e) {
            // Token 无效时记录警告日志并返回 null
            log.warn("Token 解析失败，视为未登录: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 必须登录，token 无效时抛出 AuthenticationException(M3000)
     * 
     * 适用场景：
     * - 必须确保用户已认证
     * - token 无效时需要立即中断流程
     * 
     * @param token JWT Token（可带 "Bearer " 前缀）
     * @return 用户 ID
     * @throws AuthenticationException token 无效时抛出 M3000
     */
    public Long requireUserId(String token) {
        Long userId;
        try {
            userId = authService.getUserIdFromToken(token);
        } catch (RuntimeException e) {
            log.warn("认证失败，token 无效: {}", e.getMessage());
            throw new AuthenticationException(ReasonCode.UNAUTHORIZED);
        }
        // token 有效，进一步校验账号状态（被封/禁用即时拦截）
        checkUserStatus(userId);
        return userId;
    }

    /**
     * 校验用户账号状态，非 active 时直接拒绝访问。
     * 这样被封禁的账号在 token 有效期内也会被即时拦截，无需等待 token 过期。
     */
    private void checkUserStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("Token 有效但用户 {} 不存在", userId);
            throw new AuthenticationException(ReasonCode.UNAUTHORIZED);
        }
        if (!"active".equals(user.getStatus())) {
            log.warn("用户 {} 账号状态异常（{}），拒绝访问", userId, user.getStatus());
            throw new AuthenticationException(ReasonCode.ACCOUNT_BANNED);
        }
    }

    /**
     * 检查是否是管理员，区分「没登录」和「不是管理员」
     * 
     * 适用场景：
     * - 管理员接口权限校验
     * - 需要区分未登录和无权限
     * 
     * @param token JWT Token（可带 "Bearer " 前缀）
     * @throws AuthenticationException token 无效时抛出 M3000
     * @throws AuthorizationException 已登录但非管理员时抛出 M3009
     */
    public void requireAdmin(String token) {
        // 先验证登录状态
        Long userId = requireUserId(token);
        
        // 再验证管理员权限
        boolean isAdmin = authService.isAdmin(token);
        if (!isAdmin) {
            log.warn("用户 {} 尝试访问管理员接口，但无权限", userId);
            throw new AuthorizationException(ReasonCode.ADMIN_REQUIRED);
        }
    }
}
