package com.teammatch.m6.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建处罚请求 DTO
 *
 * 根据详细设计文档 7.2 节定义
 * 供管理员创建处罚使用
 */
@Data
public class PenaltyCreateDTO {

    /**
     * 被处罚用户ID
     * 约束：必填
     */
    @NotNull(message = "被处罚用户ID不能为空")
    private Long userId;

    /**
     * 处罚类型
     * 约束：必填，credit_deduct（扣信誉分）或 function_limit（功能限制）
     */
    @NotBlank(message = "处罚类型不能为空")
    @Pattern(regexp = "credit_deduct|function_limit", message = "处罚类型必须是 credit_deduct 或 function_limit")
    private String type;

    /**
     * 扣分值
     * 约束：credit_deduct类型时必填，必须为正数
     */
    @Positive(message = "扣分值必须为正数")
    private Integer creditDeductValue;

    /**
     * 处罚理由
     * 约束：必填，长度 1-500
     */
    @NotBlank(message = "处罚理由不能为空")
    @Size(min = 1, max = 500, message = "处罚理由长度必须在1-500之间")
    private String reason;

    /**
     * 关联举报记录ID（可选）
     */
    private Long relatedReportId;
}
