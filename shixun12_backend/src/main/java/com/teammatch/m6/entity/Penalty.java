package com.teammatch.m6.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 违规处罚记录实体类
 * 对应数据库表：penalty
 *
 * 根据详细设计文档 7.5 节定义
 * 支持两种处罚类型：credit_deduct（扣信誉分）、function_limit（功能限制）
 */
@Data
@TableName("penalty")
public class Penalty {

    /**
     * 处罚记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被处罚用户ID
     */
    private Long userId;

    /**
     * 处罚类型
     * 可选值：credit_deduct（扣信誉分）、function_limit（功能限制）
     */
    private String type;

    /**
     * 扣分值（仅credit_deduct类型有效）
     */
    private Integer creditDeductValue;

    /**
     * 处罚理由
     */
    private String reason;

    /**
     * 执行处罚的管理员ID
     */
    private Long adminId;

    /**
     * 关联举报记录ID（可为null）
     */
    private Long relatedReportId;

    /**
     * 处罚状态
     * 可选值：active（生效中）、revoked（已撤销）
     */
    private String status;

    /**
     * 撤销时间（申诉通过时）
     */
    private LocalDateTime revokedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
