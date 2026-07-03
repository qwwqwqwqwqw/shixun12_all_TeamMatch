package com.teammatch.m6.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 板块实体类
 * 对应数据库表：board
 *
 * 根据详细设计文档 4.5 节、7.5 节定义
 * M6 负责板块管理，为 M4 项目创建提供分类能力
 */
@Data
@TableName("board")
public class Board {

    /**
     * 板块ID
     * 对应表：board.id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 板块名称
     * 对应表：board.name
     * 约束：唯一，不能为空
     */
    private String name;

    /**
     * 板块描述
     * 对应表：board.description
     */
    private String description;

    /**
     * 板块状态
     * 对应表：board.status
     * 可选值：active（启用）/ inactive（禁用）
     */
    private String status;

    /**
     * 显示排序
     * 对应表：board.sort_order
     * 数值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 创建时间
     * 对应表：board.created_at
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     * 对应表：board.updated_at
     */
    private LocalDateTime updatedAt;
}
