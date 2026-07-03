package com.teammatch.m6.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 撤销处罚请求 DTO
 *
 * 根据详细设计文档 7.2 节定义
 * 供管理员撤销处罚（申诉通过时使用）
 */
@Data
public class PenaltyRevokeDTO {

    /**
     * 撤销原因
     * 约束：必填，长度 1-500
     */
    @NotBlank(message = "撤销原因不能为空")
    @Size(min = 1, max = 500, message = "撤销原因长度必须在1-500之间")
    private String reason;
}
