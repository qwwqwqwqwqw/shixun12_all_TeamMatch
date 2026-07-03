package com.teammatch.m4.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectCreateDTO {
    private Long creatorId; // 发起人ID（实际场景建议从Token获取）
    private Long boardId; // 所属板块
    private String title; // 项目标题
    private String description; // 项目描述
    private Integer maxMembers; // 最大人数
    private LocalDateTime deadline; // 招募截止时间
    private List<Long> skillTagIds; // 关联技能标签列表
}
