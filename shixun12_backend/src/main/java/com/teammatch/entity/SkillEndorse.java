package com.teammatch.entity;

import java.time.LocalDateTime;

/**
 * 技能背书实体类
 * 对应数据库表：skill_endorse
 *
 * M5 模块负责维护此表
 * 用于存储互评时对队友的技能背书记录，仅用于展示，不影响信誉分计算
 */
public class SkillEndorse {
    /** 技能背书ID */
    private Long id;

    /** 关联的互评记录ID */
    private Long evaluationId;

    /** 项目ID */
    private Long projectId;

    /** 背书人用户ID */
    private Long endorserId;

    /** 被背书人用户ID */
    private Long targetId;

    /** 被背书的技能标签ID */
    private Long skillTagId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Long evaluationId) {
        this.evaluationId = evaluationId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getEndorserId() {
        return endorserId;
    }

    public void setEndorserId(Long endorserId) {
        this.endorserId = endorserId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Long getSkillTagId() {
        return skillTagId;
    }

    public void setSkillTagId(Long skillTagId) {
        this.skillTagId = skillTagId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
