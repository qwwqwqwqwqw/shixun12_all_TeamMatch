package com.teammatch.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * T-134: exit_vote 退出投票主表
 * status: voting / closed
 * result: pass / reject / null
 * penaltyLevel: negotiated / malicious
 */
@Data
@TableName("exit_vote")
public class ExitVote {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    /** 被踢目标成员 userId */
    private Long targetUserId;
    /** 发起人（队长）*/
    private Long initiatorId;
    /** voting / closed */
    private String status;
    /** 处罚级别：negotiated / malicious */
    private String penaltyLevel;
    /** 投票结果：pass / reject；投票中为 null */
    private String result;
    /** 发起原因 */
    private String reason;
    /** 参与投票总人数（快照，不含目标成员） */
    private Integer totalVoters;
    /** 赞成票数 */
    private Integer agreeCount;
    /** 反对票数 */
    private Integer disagreeCount;
    /** 投票截止时间，创建时 +24h */
    private LocalDateTime deadlineAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
