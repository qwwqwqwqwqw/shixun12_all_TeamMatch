package com.teammatch.m4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teammatch.m4.entity.TeamMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface M4TeamMemberMapper extends BaseMapper<TeamMember> {
}
