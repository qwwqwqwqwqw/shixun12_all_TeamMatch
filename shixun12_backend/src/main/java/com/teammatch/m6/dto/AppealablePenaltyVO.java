package com.teammatch.m6.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 申诉页：当前用户可发起申诉的生效中处罚项
 */
@Data
public class AppealablePenaltyVO {

    private Long penaltyId;
    private String type;
    private Integer creditDeductValue;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
}
