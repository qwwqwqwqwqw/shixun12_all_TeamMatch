package com.teammatch.entity;

import java.time.LocalDateTime;

/**
 * 信誉分变化流水实体类
 * 对应数据库表：credit_change
 *
 * M5 模块负责维护此表
 * 用于记录所有信誉分变化流水，包括互评加分、退出扣分、处罚扣分、申诉恢复等
 * 用户的信誉分 = SUM(effective=1 的 change_value)
 */
public class CreditChange {
    /** 信誉分变化记录ID */
    private Long id;

    /** 信誉分变化的用户ID */
    private Long userId;

    /** 关联的项目ID，如果适用 */
    private Long projectId;

    /** 变化类型：evaluation 互评，exit_vote 投票退出，self_exit 自主退出，penalty 处罚，penalty_restore 处罚扣分撤销 , appeal_restore 申诉恢复 */
    private String changeType;

    /** 变化值，正数表示加分，负数表示扣分 */
    private Integer changeValue;

    /** 是否有效：1 有效计入信誉分，0 挂起或不计入 */
    private Boolean effective;

    /** 来源类型：evaluation 互评表，exit_vote 退出投票表，team_member 成员表，penalty 处罚表，appeal 申诉表 */
    private String sourceType;

    /** 来源记录ID */
    private Long sourceId;

    /** 人类可读的描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Integer getChangeValue() {
        return changeValue;
    }

    public void setChangeValue(Integer changeValue) {
        this.changeValue = changeValue;
    }

    public Boolean getEffective() {
        return effective;
    }

    public void setEffective(Boolean effective) {
        this.effective = effective;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
