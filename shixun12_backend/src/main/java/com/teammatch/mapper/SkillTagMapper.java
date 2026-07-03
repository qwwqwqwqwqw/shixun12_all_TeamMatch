package com.teammatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teammatch.entity.SkillTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 技能标签数据访问层
 */
@Mapper
public interface SkillTagMapper extends BaseMapper<SkillTag> {
}
