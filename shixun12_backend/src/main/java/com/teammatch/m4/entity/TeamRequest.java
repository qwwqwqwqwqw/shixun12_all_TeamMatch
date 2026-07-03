package com.teammatch.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("team_request")
public class TeamRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long fromUserId;
    private Long toUserId;
    private String requestType; // invite/apply
    private String status; // pending/accepted/rejected/cancelled/expired
    private String message;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
