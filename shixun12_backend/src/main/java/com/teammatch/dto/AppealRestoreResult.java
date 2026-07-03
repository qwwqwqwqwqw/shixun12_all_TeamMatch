package com.teammatch.dto;

import java.io.Serializable;

/**
 * 申诉恢复结果 DTO
 * M5-7 申诉恢复 Service 出参
 */
public class AppealRestoreResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long appealId;

    private Long evaluationId;

    private Long targetUserId;

    /** 原评价流水变化值 */
    private Integer originalChangeValue;

    /** 恢复值（原值取反） */
    private Integer restoreValue;

    /** 原流水 effective=0 时跳过恢复 */
    private Boolean skipped = false;

    /** 跳过原因（仅 skipped=true 时有值） */
    private String skipReason;

    /** 幂等：同一恢复已执行过 */
    private Boolean alreadyRestored = false;

    public Long getAppealId() {
        return appealId;
    }

    public void setAppealId(Long appealId) {
        this.appealId = appealId;
    }

    public Long getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Long evaluationId) {
        this.evaluationId = evaluationId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Integer getOriginalChangeValue() {
        return originalChangeValue;
    }

    public void setOriginalChangeValue(Integer originalChangeValue) {
        this.originalChangeValue = originalChangeValue;
    }

    public Integer getRestoreValue() {
        return restoreValue;
    }

    public void setRestoreValue(Integer restoreValue) {
        this.restoreValue = restoreValue;
    }

    public Boolean getSkipped() {
        return skipped;
    }

    public void setSkipped(Boolean skipped) {
        this.skipped = skipped;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public void setSkipReason(String skipReason) {
        this.skipReason = skipReason;
    }

    public Boolean getAlreadyRestored() {
        return alreadyRestored;
    }

    public void setAlreadyRestored(Boolean alreadyRestored) {
        this.alreadyRestored = alreadyRestored;
    }
}
