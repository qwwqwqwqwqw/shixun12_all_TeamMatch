package com.teammatch.entity;

import java.time.LocalDateTime;

/**
 * 评价标签实体类
 * 对应数据库表：evaluation_tag
 *
 * M5 模块负责维护此表
 * 用于存储互评时选择的正面/负面标签，例如"沟通积极"、"任务拖延"等
 */
public class EvaluationTag {
    /** 评价标签ID */
    private Long id;

    /** 关联的互评记录ID */
    private Long evaluationId;

    /** 标签名称 */
    private String tagName;

    /** 标签类型：positive 正面标签，negative 负面标签 */
    private String tagType;

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

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagType() {
        return tagType;
    }

    public void setTagType(String tagType) {
        this.tagType = tagType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
