package com.teammatch.m4.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teammatch.m4.entity.TeamMember;
import java.util.List;

public interface TeamMemberService extends IService<TeamMember> {

    /**
     * T-132 获取项目成员列表，区分 active/exited
     */
    List<TeamMember> getProjectMembers(Long projectId);
}
