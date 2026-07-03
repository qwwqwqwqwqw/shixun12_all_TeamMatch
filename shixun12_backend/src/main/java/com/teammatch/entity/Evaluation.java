package com.teammatch.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 互评实体类
 * 对应数据库表：evaluation
 *
 * M5 模块负责维护此表
 * 用于存储项目结束后的四维互评数据：沟通、任务、技能、责任
 */
@TableName("evaluation")
public class Evaluation {
    public static final String STATUS_NORMAL = "normal";
    public static final String STATUS_PENDING_REVIEW = "pending_review";
    public static final String STATUS_VOIDED = "voided";
    public static final String STATUS_KEPT_NO_CREDIT = "kept_no_credit";

    /** 互评记录ID */
    private Long id;

    /** 项目ID */
    private Long projectId;

    /** 评价人用户ID */
    private Long evaluatorId;

    /** 被评价人用户ID */
    private Long targetId;

    /** 沟通评分，1-5 分 */
    private Integer communicationScore;

    /** 任务完成评分，1-5 分 */
    private Integer taskScore;

    /** 技术能力评分，1-5 分 */
    private Integer skillScore;

    /** 责任心评分，1-5 分 */
    private Integer responsibilityScore;

    /** 四维评分平均值，缓存字段 */
    private BigDecimal averageScore;

    /** 评价评论 */
    private String comment;

    /** 评价状态：normal 正常，pending_review 待复核，voided 已作废，kept_no_credit 保留但不计分 */
    private String status;

    /** 复核管理员ID */
    private Long reviewerId;

    /** 复核备注 */
    private String reviewNote;

    /** 复核时间 */
    private LocalDateTime reviewedAt;

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

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getEvaluatorId() {
        return evaluatorId;
    }

    public void setEvaluatorId(Long evaluatorId) {
        this.evaluatorId = evaluatorId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Integer getCommunicationScore() {
        return communicationScore;
    }

    public void setCommunicationScore(Integer communicationScore) {
        this.communicationScore = communicationScore;
    }

    public Integer getTaskScore() {
        return taskScore;
    }

    public void setTaskScore(Integer taskScore) {
        this.taskScore = taskScore;
    }

    public Integer getSkillScore() {
        return skillScore;
    }

    public void setSkillScore(Integer skillScore) {
        this.skillScore = skillScore;
    }

    public Integer getResponsibilityScore() {
        return responsibilityScore;
    }

    public void setResponsibilityScore(Integer responsibilityScore) {
        this.responsibilityScore = responsibilityScore;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
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
