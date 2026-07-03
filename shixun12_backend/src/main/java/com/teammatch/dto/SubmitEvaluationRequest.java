package com.teammatch.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 互评提交请求 DTO（用户端，不含 evaluatorId）
 *
 * M5-8A 使用。与 EvaluationSubmitDTO 的区别：
 * - 不含 evaluatorId 字段（评价人身份由 Controller 从 token 注入）
 * - 面向前端请求体，Security 层面不可伪造评价人身份
 */
public class SubmitEvaluationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 项目 ID */
    private Long projectId;

    /** 被评价人 ID */
    private Long targetId;

    /** 沟通评分，1-5 分 */
    private Integer communicationScore;

    /** 任务完成评分，1-5 分 */
    private Integer taskScore;

    /** 技术能力评分，1-5 分 */
    private Integer skillScore;

    /** 责任心评分，1-5 分 */
    private Integer responsibilityScore;

    /** 评价评论 */
    private String comment;

    /** 正向标签列表 */
    private List<String> positiveTags;

    /** 负向标签列表 */
    private List<String> negativeTags;

    // ==================== 构造方法 ====================

    public SubmitEvaluationRequest() {
    }

    // ==================== Getter/Setter ====================

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
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
}
