package com.teammatch.m4.dto;

import lombok.Data;

/**
 * T-139: 提交投票请求体
 */
@Data
public class ExitVoteSubmitDTO {
    /** 投票人 userId */
    private Long voterId;
    /** agree / disagree */
    private String choice;
}
