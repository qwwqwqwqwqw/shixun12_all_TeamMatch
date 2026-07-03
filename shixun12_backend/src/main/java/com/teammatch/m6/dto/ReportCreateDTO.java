package com.teammatch.m6.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 提交举报请求 DTO
 *
 * 根据详细设计文档 7.2 节定义
 */
@Data
public class ReportCreateDTO {

    /**
     * 举报对象类型
     * 约束：必填，user 或 project
     */
    @NotBlank(message = "举报对象类型不能为空")
    @Pattern(regexp = "user|project", message = "举报对象类型必须是 user 或 project")
    private String targetType;

    /**
     * 举报对象ID
     * 约束：必填
     */
    @NotNull(message = "举报对象ID不能为空")
    private Long targetId;

    /**
     * 举报原因
     * 约束：必填，长度 1-500
     */
    @NotBlank(message = "举报原因不能为空")
    @Size(min = 1, max = 500, message = "举报原因长度必须在1-500之间")
    private String reason;

    /**
     * 证据图片 URL 列表（先调用 POST /api/files/upload?category=report_evidence）
     */
    @Size(max = 5, message = "证据图片最多5张")
    private List<String> evidenceUrls;
}
