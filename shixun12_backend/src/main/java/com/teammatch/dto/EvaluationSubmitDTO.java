package com.teammatch.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 互评提交 DTO
 * 用于 M5-2 互评提交内容基础合法性校验
 */
public class EvaluationSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评价人 ID
     */
    private Long evaluatorId;

    /**
     * 被评价人 ID
     */
    private Long targetId;

    /**
     * 项目 ID
     */
    private Long projectId;

    /**
     * 沟通评分，1-5 分
     */
    private Integer communicationScore;

    /**
     * 任务完成评分，1-5 分
     */
    private Integer taskScore;

    /**
     * 技术能力评分，1-5 分
     */
    private Integer skillScore;

    /**
     * 责任心评分，1-5 分
     */
    private Integer responsibilityScore;

    /**
     * 评价评论
     */
    private String comment;

    /**
     * 正向标签列表
     */
    private List<String> positiveTags;

    /**
     * 负向标签列表
     */
    private List<String> negativeTags;

    // ==================== 构造方法 ====================

    public EvaluationSubmitDTO() {
    }

    public EvaluationSubmitDTO(Long evaluatorId, Long targetId, Long projectId,
                               Integer communicationScore, Integer taskScore,
                               Integer skillScore, Integer responsibilityScore,
                               String comment, List<String> positiveTags, List<String> negativeTags) {
        this.evaluatorId = evaluatorId;
        this.targetId = targetId;
        this.projectId = projectId;
        this.communicationScore = communicationScore;
        this.taskScore = taskScore;
        this.skillScore = skillScore;
        this.responsibilityScore = responsibilityScore;
        this.comment = comment;
        this.positiveTags = positiveTags;
        this.negativeTags = negativeTags;
    }

    // ==================== Getter/Setter ====================

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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getPositiveTags() {
        return positiveTags;
    }

    public void setPositiveTags(List<String> positiveTags) {
        this.positiveTags = positiveTags;
    }

    public List<String> getNegativeTags() {
        return negativeTags;
    }

    public void setNegativeTags(List<String> negativeTags) {
        this.negativeTags = negativeTags;
    }

    @Override
    public String toString() {
        return "EvaluationSubmitDTO{" +
                "evaluatorId=" + evaluatorId +
                ", targetId=" + targetId +
                ", projectId=" + projectId +
                ", communicationScore=" + communicationScore +
                ", taskScore=" + taskScore +
                ", skillScore=" + skillScore +
                ", responsibilityScore=" + responsibilityScore +
                ", comment='" + comment + '\'' +
                ", positiveTags=" + positiveTags +
                ", negativeTags=" + negativeTags +
                '}';
    }
}
