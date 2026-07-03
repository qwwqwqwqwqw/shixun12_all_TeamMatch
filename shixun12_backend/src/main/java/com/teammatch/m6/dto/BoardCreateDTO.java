package com.teammatch.m6.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建板块请求 DTO
 *
 * 根据详细设计文档 7.2 节定义
 */
@Data
public class BoardCreateDTO {

    /**
     * 板块名称
     * 约束：必填，唯一，长度 1-64
     */
    @NotBlank(message = "板块名称不能为空")
    @Size(max = 64, message = "板块名称长度不能超过64")
    private String name;

    /**
     * 板块描述
     * 约束：可选，长度 0-255
     */
    @Size(max = 255, message = "板块描述长度不能超过255")
    private String description;

    /**
     * 显示排序
     * 约束：可选，默认 0
     */
    private Integer sortOrder = 0;
}
