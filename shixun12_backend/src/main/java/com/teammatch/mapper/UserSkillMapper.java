package com.teammatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teammatch.entity.UserSkill;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户技能关联数据访问层
 */
@Mapper
public interface UserSkillMapper extends BaseMapper<UserSkill> {
}
