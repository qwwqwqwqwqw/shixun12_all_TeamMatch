package com.teammatch.m4.dto;

import lombok.Data;

@Data
public class TeamRequestDTO {
    private Long projectId; // 项目ID
    private Long fromUserId; // 发起人（队长）ID
    private Long toUserId; // 被邀请人ID
    private String message; // 邀请/申请留言
}
