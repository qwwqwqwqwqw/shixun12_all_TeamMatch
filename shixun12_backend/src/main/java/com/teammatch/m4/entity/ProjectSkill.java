package com.teammatch.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("project_skill")
public class ProjectSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long skillTagId;
    private LocalDateTime createdAt;
}
