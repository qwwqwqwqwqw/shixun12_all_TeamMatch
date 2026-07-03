package com.teammatch.m6.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新板块请求 DTO
 *
 * 根据详细设计文档 7.2 节定义
 */
@Data
public class BoardUpdateDTO {

    /**
     * 板块名称
     * 约束：可选，长度 1-64
     */
    @Size(max = 64, message = "板块名称长度不能超过64")
    private String name;

    /**
     * 板块描述
     * 约束：可选，长度 0-255
     */
    @Size(max = 255, message = "板块描述长度不能超过255")
    private String description;

    /**
     * 板块状态
     * 约束：可选，active / inactive
     */
    @Pattern(regexp = "active|inactive", message = "状态必须是 active 或 inactive")
    private String status;

    /**
     * 显示排序
     * 约束：可选
     */
    private Integer sortOrder;
}
