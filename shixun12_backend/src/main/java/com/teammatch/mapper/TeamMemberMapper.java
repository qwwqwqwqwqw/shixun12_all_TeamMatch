package com.teammatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teammatch.entity.TeamMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 项目成员数据访问层
 * 用于 M5-1A/B/C 互评资格判断
 */
@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {

    // 继承 BaseMapper 提供的基础 CRUD 方法

    /**
     * 检查用户是否是项目的 active 成员
     * 用于 M5-1A 项目级互评入口判断
     * 用于 M5-1C 最终提交前资格兜底校验
     *
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return 如果是 active 成员返回成员记录，否则返回 null
     */
    @Select("SELECT * FROM team_member WHERE user_id = #{userId} AND project_id = #{projectId} AND status = 'active'")
    TeamMember findActiveMember(@Param("userId") Long userId, @Param("projectId") Long projectId);

    /**
     * 查询项目的所有 active 成员
     * 用于 M5-1B 待评价成员列表判断
     *
     * @param projectId 项目 ID
     * @return active 成员列表
     */
    @Select("SELECT * FROM team_member WHERE project_id = #{projectId} AND status = 'active'")
    List<TeamMember> findActiveMembers(@Param("projectId") Long projectId);
}
