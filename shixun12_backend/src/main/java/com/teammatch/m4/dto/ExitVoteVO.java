package com.teammatch.m4.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 退出投票视图对象（不直接暴露 Entity）
 */
@Data
public class ExitVoteVO {
    private Long id;
    private Long projectId;
    private Long targetUserId;
    private Long initiatorId;
    private String status;
    private String penaltyLevel;
    private String result;
    private String reason;
    private Integer totalVoters;
    private Integer agreeCount;
    private Integer disagreeCount;
    private LocalDateTime deadlineAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
}
