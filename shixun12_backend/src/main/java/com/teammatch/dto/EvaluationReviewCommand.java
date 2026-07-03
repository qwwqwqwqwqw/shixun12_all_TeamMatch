package com.teammatch.dto;

import java.io.Serializable;

/**
 * 评价复核命令 DTO
 * 用于 M5-6 评价复核 Service 的输入参数
 */
public class EvaluationReviewCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long evaluationId;

    private Long reviewerId;

    private String action;

    private String reviewNote;

    public EvaluationReviewCommand() {
    }

    public EvaluationReviewCommand(Long evaluationId, Long reviewerId, String action, String reviewNote) {
        this.evaluationId = evaluationId;
        this.reviewerId = reviewerId;
        this.action = action;
        this.reviewNote = reviewNote;
    }

    public Long getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Long evaluationId) {
        this.evaluationId = evaluationId;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }
}
