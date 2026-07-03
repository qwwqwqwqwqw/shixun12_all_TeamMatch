package com.teammatch.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * T-135: exit_vote_record 单条投票记录
 */
@Data
@TableName("exit_vote_record")
public class ExitVoteRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 对应 exit_vote.id，列名 vote_id */
    @TableField("vote_id")
    private Long voteId;
    private Long voterId;
    /** agree / disagree */
    private String choice;
    private LocalDateTime createdAt;
}
