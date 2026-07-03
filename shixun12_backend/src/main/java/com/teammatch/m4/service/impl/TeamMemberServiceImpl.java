package com.teammatch.m4.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teammatch.m4.entity.TeamMember;
import com.teammatch.m4.mapper.M4TeamMemberMapper;
import com.teammatch.m4.service.TeamMemberService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamMemberServiceImpl extends ServiceImpl<M4TeamMemberMapper, TeamMember> implements TeamMemberService {

    /**
     * T-132: 获取项目成员列表（含 active 和 exited），按加入时间排序
     */
    @Override
    public List<TeamMember> getProjectMembers(Long projectId) {
        return this.list(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getProjectId, projectId)
                .orderByAsc(TeamMember::getJoinedAt));
    }
}

