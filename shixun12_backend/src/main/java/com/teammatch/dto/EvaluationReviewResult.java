package com.teammatch.dto;

import java.io.Serializable;

/**
 * 评价复核结果 DTO
 * 用于 M5-6 评价复核 Service 的结构化返回值
 */
public class EvaluationReviewResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long evaluationId;

    private String oldStatus;

    private String newStatus;

    private Long targetId;

    private Integer creditDelta;

    private Boolean creditEffectiveChanged;

    public Long getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Long evaluationId) {
        this.evaluationId = evaluationId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Integer getCreditDelta() {
        return creditDelta;
    }

    public void setCreditDelta(Integer creditDelta) {
        this.creditDelta = creditDelta;
    }

    public Boolean getCreditEffectiveChanged() {
        return creditEffectiveChanged;
    }

    public void setCreditEffectiveChanged(Boolean creditEffectiveChanged) {
        this.creditEffectiveChanged = creditEffectiveChanged;
    }
}
