package com.teammatch.m4.dto;

import lombok.Data;

/**
 * T-137: 发起退出投票请求体
 */
@Data
public class ExitVoteCreateDTO {
    /** 操作人（队长）userId */
    private Long initiatorId;
    /** 被踢目标成员 userId */
    private Long targetUserId;
    private String reason;
    /** 处罚级别：negotiated / malicious，V2.1 队长必选，缺失或非法均拒绝 */
    private String penaltyLevel;
}
