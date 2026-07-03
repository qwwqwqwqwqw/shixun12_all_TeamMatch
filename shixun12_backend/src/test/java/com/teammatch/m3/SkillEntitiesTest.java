package com.teammatch.m3;

import com.teammatch.entity.SkillTag;
import com.teammatch.entity.UserSkill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M3 模块 SkillTag 和 UserSkill 实体单元测试
 */
@DisplayName("M3 技能相关实体测试")
class SkillEntitiesTest {

    @Test
    @DisplayName("SkillTag getter/setter - 所有字段应能正确设置和获取")
    void skillTagGettersSetters_shouldWorkCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        SkillTag tag = new SkillTag();
        tag.setId(1L);
        tag.setName("Java");
        tag.setCategory("language");
        tag.setStatus("active");
        tag.setCreatedAt(now);
        tag.setUpdatedAt(now);

        assertThat(tag.getId()).isEqualTo(1L);
        assertThat(tag.getName()).isEqualTo("Java");
        assertThat(tag.getCategory()).isEqualTo("language");
        assertThat(tag.getStatus()).isEqualTo("active");
        assertThat(tag.getCreatedAt()).isEqualTo(now);
        assertThat(tag.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("UserSkill getter/setter - 所有字段应能正确设置和获取")
    void userSkillGettersSetters_shouldWorkCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        UserSkill userSkill = new UserSkill();
        userSkill.setId(1L);
        userSkill.setUserId(100L);
        userSkill.setSkillTagId(200L);
        userSkill.setCreatedAt(now);

        assertThat(userSkill.getId()).isEqualTo(1L);
        assertThat(userSkill.getUserId()).isEqualTo(100L);
        assertThat(userSkill.getSkillTagId()).isEqualTo(200L);
        assertThat(userSkill.getCreatedAt()).isEqualTo(now);
    }
}
