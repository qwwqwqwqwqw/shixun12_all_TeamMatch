package com.teammatch.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收到的互评响应 VO（用户端，匿名）
 *
 * M5-8A B5 使用。
 * 关键约束：
 * - 不包含 evaluatorId，不暴露评价者身份（V2.1 §6.8）
 * - status 原样透传（voided 已在 Controller 层过滤）
 * - 时间字段使用 LocalDateTime，与项目 Entity 风格一致
 */
public class ReceivedEvaluationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 互评记录 ID */
    private Long evaluationId;

    /** 项目 ID */
    private Long projectId;

    /** 沟通评分 */
    private Integer communicationScore;

    /** 任务完成评分 */
    private Integer taskScore;

    /** 技术能力评分 */
    private Integer skillScore;

    /** 责任心评分 */
    private Integer responsibilityScore;

    /** 四维均分 */
    private BigDecimal averageScore;

    /** 评价评论 */
    private String comment;

    /** 评价状态：normal / pending_review / kept_no_credit */
    private String status;

    /** 评价时间 */
    private LocalDateTime createdAt;

    // ==================== 构造方法 ====================

    public ReceivedEvaluationVO() {
    }

    // ==================== Getter/Setter ====================

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
