package com.teammatch.m6.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 处理举报请求 DTO
 *
 * 根据详细设计文档 7.2 节定义
 * 供管理员处理举报使用
 */
@Data
public class ReportHandleDTO {

    /**
     * 处理结果
     * 约束：必填，resolved（已处理）或 dismissed（已驳回）
     */
    @NotBlank(message = "处理结果不能为空")
    @Pattern(regexp = "resolved|dismissed", message = "处理结果必须是 resolved 或 dismissed")
    private String status;

    /**
     * 处理结果说明
     * 约束：可选，长度 0-500
     */
    @Size(max = 500, message = "处理结果说明长度不能超过500")
    private String handleResult;

    /**
     * 是否创建处罚（可选，默认false）
     * 当 status=resolved 且需要处罚时，设为 true
     */
    private Boolean createPenalty = false;

    /**
     * 处罚类型（可选，createPenalty=true 时必填）
     * credit_deduct / function_limit
     */
    private String penaltyType;

    /**
     * 扣分值（可选，penaltyType=credit_deduct 时必填）
     */
    private Integer creditDeductValue;

    /**
     * 处罚原因（可选，createPenalty=true 时使用，默认使用举报原因）
     */
    private String penaltyReason;
}
