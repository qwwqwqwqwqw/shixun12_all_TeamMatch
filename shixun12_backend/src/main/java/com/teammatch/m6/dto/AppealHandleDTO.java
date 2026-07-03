package com.teammatch.m6.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 申诉处理请求 DTO（管理端）
 */
@Data
public class AppealHandleDTO {

    /**
     * 处理结果: approved / rejected
     */
    @NotBlank(message = "处理结果不能为空")
    private String status;

    /**
     * 处理说明
     */
    private String handleResult;
}
