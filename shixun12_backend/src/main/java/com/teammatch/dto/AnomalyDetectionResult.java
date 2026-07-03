package com.teammatch.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 异常评价检测结果
 * 用于 M5-4 覆盖全部队友后的项目级异常检测结果输出
 */
public class AnomalyDetectionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String REASON_NONE = "NONE";
    public static final String REASON_INCOMPLETE_COVERAGE = "INCOMPLETE_COVERAGE";
    public static final String REASON_EXTREME_LOW = "EXTREME_LOW";
    public static final String REASON_EXTREME_PERFECT = "EXTREME_PERFECT";

    /**
     * 是否已覆盖全部应评价队友
     */
    private boolean coverageComplete;

    /**
     * 是否触发异常挂起规则
     */
    private boolean triggered;

    /**
     * 异常原因：NONE / INCOMPLETE_COVERAGE / EXTREME_LOW / EXTREME_PERFECT
     */
    private String reason;

    /**
     * 命中异常时需要批量标记的评价 ID
     */
    private List<Long> affectedEvaluationIds;

    public AnomalyDetectionResult() {
        this.affectedEvaluationIds = Collections.emptyList();
    }

    public AnomalyDetectionResult(boolean coverageComplete, boolean triggered,
                                  String reason, List<Long> affectedEvaluationIds) {
        this.coverageComplete = coverageComplete;
        this.triggered = triggered;
        this.reason = reason;
        this.affectedEvaluationIds = copyIds(affectedEvaluationIds);
    }

    public static AnomalyDetectionResult normal() {
        return new AnomalyDetectionResult(true, false, REASON_NONE, Collections.emptyList());
    }

    public static AnomalyDetectionResult incompleteCoverage() {
        return new AnomalyDetectionResult(false, false, REASON_INCOMPLETE_COVERAGE, Collections.emptyList());
    }

    public static AnomalyDetectionResult extremeLow(List<Long> affectedEvaluationIds) {
        return new AnomalyDetectionResult(true, true, REASON_EXTREME_LOW, affectedEvaluationIds);
    }

    public static AnomalyDetectionResult extremePerfect(List<Long> affectedEvaluationIds) {
        return new AnomalyDetectionResult(true, true, REASON_EXTREME_PERFECT, affectedEvaluationIds);
    }

    public boolean isCoverageComplete() {
        return coverageComplete;
    }

    public void setCoverageComplete(boolean coverageComplete) {
        this.coverageComplete = coverageComplete;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void setTriggered(boolean triggered) {
        this.triggered = triggered;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<Long> getAffectedEvaluationIds() {
        return affectedEvaluationIds;
    }

    public void setAffectedEvaluationIds(List<Long> affectedEvaluationIds) {
        this.affectedEvaluationIds = copyIds(affectedEvaluationIds);
    }

    private static List<Long> copyIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(ids);
    }
}
