package com.teammatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户技能关联实体类
 * 对应数据库表：user_skill
 */
@Data
@TableName("user_skill")
public class UserSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long skillTagId;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSkillTagId() { return skillTagId; }
    public void setSkillTagId(Long skillTagId) { this.skillTagId = skillTagId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
