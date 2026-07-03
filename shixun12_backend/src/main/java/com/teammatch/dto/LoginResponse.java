package com.teammatch.dto;

import lombok.Data;

/**
 * 登录响应 DTO
 * 
 * 【实习要点2】为什么不直接返回 User 实体？
 * 1. 数据安全：避免暴露 passwordHash、role、status 等敏感字段
 * 2. 接口稳定性：User 实体可能频繁变化，但 DTO 保持稳定
 * 3. 最小权限：只返回前端真正需要的字段
 * 
 * 面试时可以这样说：
 * "我采用了 DTO 模式来隔离数据层和表现层，确保 API 返回的数据是安全且稳定的。
 *  这符合 SOLID 原则中的依赖倒置原则，也体现了我对数据安全的重视。"
 */
@Data
public class LoginResponse {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 邮箱是否已验证
     */
    private Boolean emailVerified;

    /**
     * 正式档案是否已完成
     */
    private Boolean formalProfileCompleted;

    /**
     * 信用分
     */
    private Integer creditScore;

    /**
     * JWT Token
     */
    private String token;
}
