package com.teammatch.m6.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端：板块下的项目摘要
 */
@Data
public class BoardProjectSummaryVO {

    private Long id;
    private Long creatorId;
    private String title;
    private String status;
    private Integer maxMembers;
    private LocalDateTime createdAt;
}
