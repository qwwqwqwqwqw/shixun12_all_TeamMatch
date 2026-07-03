package com.teammatch.m6.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申诉页：当前用户可发起申诉的评价项
 */
@Data
public class AppealableEvaluationVO {

    private Long evaluationId;
    private Long projectId;
    private String projectTitle;
    private Integer communicationScore;
    private Integer taskScore;
    private Integer skillScore;
    private Integer responsibilityScore;
    private BigDecimal averageScore;
    private String comment;
    private LocalDateTime createdAt;
}
