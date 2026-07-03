package com.teammatch.entity;

import java.time.LocalDateTime;

/**
 * 项目成员实体类
 * 对应数据库表：team_member
 * 用于 M5 互评资格判断中的成员关系、成员状态等核心判断
 */
public class TeamMember {
    private Long id;
    private Long projectId;
    private Long userId;
    private String role;
    private String status;
    private String exitMode;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 获取成员记录 ID
     * 对应表：team_member.id
     * 作用：成员记录唯一标识
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置成员记录 ID
     * 对应表：team_member.id
     * 作用：设置成员记录唯一标识
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取项目 ID
     * 对应表：team_member.project_id
     * 作用：关联 project 表，标识成员所属项目
     */
    public Long getProjectId() {
        return projectId;
    }

    /**
     * 设置项目 ID
     * 对应表：team_member.project_id
     * 作用：设置成员所属项目
     */
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    /**
     * 获取用户 ID
     * 对应表：team_member.user_id
     * 作用：关联 user 表，标识成员身份，用于判断互评资格和生成待评价列表
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户 ID
     * 对应表：team_member.user_id
     * 作用：设置成员身份
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取成员角色
     * 对应表：team_member.role
     * 作用：标识成员角色
     * 可能的值：leader（组长）, member（普通成员）
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置成员角色
     * 对应表：team_member.role
     * 作用：设置成员角色
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取成员状态
     * 对应表：team_member.status
     * 作用：M5 互评资格判断的核心字段，只有 active 状态的成员可以参与互评
     * 可能的值：active（在项目中）, exited（已退出）
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置成员状态
     * 对应表：team_member.status
     * 作用：设置成员状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取退出方式
     * 对应表：team_member.exit_mode
     * 作用：M5 互评资格判断的核心字段，影响成员是否可被评价
     * 可能的值：self_exit（主动退出）, exit_vote（被投票踢出）, null（未退出）
     */
    public String getExitMode() {
        return exitMode;
    }

    /**
     * 设置退出方式
     * 对应表：team_member.exit_mode
     * 作用：设置成员退出方式
     */
    public void setExitMode(String exitMode) {
        this.exitMode = exitMode;
    }

    /**
     * 获取加入项目时间
     * 对应表：team_member.joined_at
     * 作用：记录成员加入项目的时间
     */
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    /**
     * 设置加入项目时间
     * 对应表：team_member.joined_at
     * 作用：设置成员加入项目的时间
     */
    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    /**
     * 获取离开项目时间
     * 对应表：team_member.left_at
     * 作用：记录成员离开项目的时间，未退出时为 null
     */
    public LocalDateTime getLeftAt() {
        return leftAt;
    }

    /**
     * 设置离开项目时间
     * 对应表：team_member.left_at
     * 作用：设置成员离开项目的时间
     */
    public void setLeftAt(LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }

    /**
     * 获取创建时间
     * 对应表：team_member.created_at
     * 作用：记录成员记录创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间
     * 对应表：team_member.created_at
     * 作用：设置成员记录创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间
     * 对应表：team_member.updated_at
     * 作用：记录成员记录最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间
     * 对应表：team_member.updated_at
     * 作用：设置成员记录最后更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
