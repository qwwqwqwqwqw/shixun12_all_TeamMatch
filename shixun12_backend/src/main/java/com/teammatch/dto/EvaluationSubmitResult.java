package com.teammatch.dto;

import java.io.Serializable;

/**
 * 互评提交结果 DTO
 * 用于 M5-5 事务提交后的结构化返回值
 */
public class EvaluationSubmitResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long evaluationId;

    private String evaluationStatus;

    private Boolean effective;

    private CreditCalculationResult creditResult;

    private AnomalyDetectionResult anomalyResult;

    public Long getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Long evaluationId) {
        this.evaluationId = evaluationId;
    }

    public String getEvaluationStatus() {
        return evaluationStatus;
    }

    public void setEvaluationStatus(String evaluationStatus) {
        this.evaluationStatus = evaluationStatus;
    }

    public Boolean getEffective() {
        return effective;
    }

    public void setEffective(Boolean effective) {
        this.effective = effective;
    }

    public CreditCalculationResult getCreditResult() {
        return creditResult;
    }

    public void setCreditResult(CreditCalculationResult creditResult) {
        this.creditResult = creditResult;
    }

    public AnomalyDetectionResult getAnomalyResult() {
        return anomalyResult;
    }

    public void setAnomalyResult(AnomalyDetectionResult anomalyResult) {
        this.anomalyResult = anomalyResult;
    }
}
