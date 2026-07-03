package com.teammatch.entity;

import java.time.LocalDateTime;

/**
 * 项目实体类
 * 对应数据库表：project
 * 用于 M5 互评资格判断中的项目状态、互评窗口等核心判断
 */
public class Project {
    private Long id;
    private Long creatorId;
    private Long boardId;
    private String title;
    private String description;
    private Integer maxMembers;
    private String status;
    private LocalDateTime deadline;
    private LocalDateTime evalDeadline;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 获取项目 ID
     * 对应表：project.id
     * 作用：项目唯一标识，用于关联 team_member 和 evaluation 表
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置项目 ID
     * 对应表：project.id
     * 作用：设置项目唯一标识
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取项目创建者 ID
     * 对应表：project.creator_id
     * 作用：标识项目创建者/队长
     */
    public Long getCreatorId() {
        return creatorId;
    }

    /**
     * 设置项目创建者 ID
     * 对应表：project.creator_id
     * 作用：设置项目创建者/队长
     */
    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    /**
     * 获取板块 ID
     * 对应表：project.board_id
     * 作用：标识项目所属板块/分类
     */
    public Long getBoardId() {
        return boardId;
    }

    /**
     * 设置板块 ID
     * 对应表：project.board_id
     * 作用：设置项目所属板块/分类
     */
    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }

    /**
     * 获取项目标题
     * 对应表：project.title
     * 作用：项目名称展示
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置项目标题
     * 对应表：project.title
     * 作用：设置项目名称
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取项目描述
     * 对应表：project.description
     * 作用：项目详细说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置项目描述
     * 对应表：project.description
     * 作用：设置项目详细说明
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取最大成员数
     * 对应表：project.max_members
     * 作用：限制项目成员上限
     */
    public Integer getMaxMembers() {
        return maxMembers;
    }

    /**
     * 设置最大成员数
     * 对应表：project.max_members
     * 作用：设置项目成员上限
     */
    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }

    /**
     * 获取项目状态
     * 对应表：project.status
     * 作用：M5 互评要求项目状态必须为 "ended"
     * 可能的值：recruiting, in_progress, ended, eval_closed
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置项目状态
     * 对应表：project.status
     * 作用：设置项目状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取招募截止时间
     * 对应表：project.deadline
     * 作用：项目招募阶段的截止时间
     */
    public LocalDateTime getDeadline() {
        return deadline;
    }

    /**
     * 设置招募截止时间
     * 对应表：project.deadline
     * 作用：设置项目招募阶段的截止时间
     */
    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    /**
     * 获取互评截止时间
     * 对应表：project.eval_deadline
     * 作用：判断当前时间是否在互评窗口内，通常为项目结束后 7 天
     */
    public LocalDateTime getEvalDeadline() {
        return evalDeadline;
    }

    /**
     * 设置互评截止时间
     * 对应表：project.eval_deadline
     * 作用：设置互评窗口的截止时间
     */
    public void setEvalDeadline(LocalDateTime evalDeadline) {
        this.evalDeadline = evalDeadline;
    }

    /**
     * 获取项目结束时间
     * 对应表：project.ended_at
     * 作用：标记项目实际结束时间，用于计算互评窗口起点
     */
    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    /**
     * 设置项目结束时间
     * 对应表：project.ended_at
     * 作用：设置项目实际结束时间
     */
    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    /**
     * 获取创建时间
     * 对应表：project.created_at
     * 作用：记录项目创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间
     * 对应表：project.created_at
     * 作用：设置项目创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间
     * 对应表：project.updated_at
     * 作用：记录项目最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间
     * 对应表：project.updated_at
     * 作用：设置项目最后更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
