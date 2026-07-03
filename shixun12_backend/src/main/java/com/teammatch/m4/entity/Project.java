package com.teammatch.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.teammatch.entity.SkillTag;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("project")
public class Project {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long creatorId;
    private Long boardId;
    private String title;
    private String description;
    private Integer maxMembers;
    private String status;
    private LocalDateTime deadline;
    private LocalDateTime evalDeadline;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 技能标签列表（非表字段，详情接口动态填充） */
    @TableField(exist = false)
    private List<SkillTag> skills;
}
