package com.teammatch.m4.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teammatch.m4.dto.ProjectCreateDTO;
import com.teammatch.m4.entity.Project;
import com.teammatch.m4.entity.ProjectSkill;
import com.teammatch.m4.entity.TeamMember;
import com.teammatch.m4.entity.TeamRequest;
import com.teammatch.m4.mapper.M4ProjectMapper;
import com.teammatch.m4.service.ProjectService;
import com.teammatch.m4.service.ProjectSkillService;
import com.teammatch.m4.service.TeamMemberService;
import com.teammatch.m4.service.TeamRequestService;
import com.teammatch.m6.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<M4ProjectMapper, Project> implements ProjectService {

    private final ProjectSkillService projectSkillService;
    private final TeamMemberService teamMemberService;
    private final BoardService boardService;

    @Autowired
    @Lazy
    private TeamRequestService teamRequestService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Project createProject(ProjectCreateDTO dto) {
        // 0. 校验 boardId 对应的板块必须存在且状态为 active
        if (dto.getBoardId() == null || !boardService.existsActiveBoard(dto.getBoardId())) {
            throw new RuntimeException("板块不存在或已禁用");
        }
        // 1. 创建 Project 主表记录 (默认为 recruiting)
        Project project = new Project();
        BeanUtils.copyProperties(dto, project);
        project.setStatus("recruiting"); // Schema Default
        this.save(project);

        // 2. 若存在技能要求，则保存 ProjectSkill 关联表
        if (dto.getSkillTagIds() != null && !dto.getSkillTagIds().isEmpty()) {
            List<ProjectSkill> projectSkills = new ArrayList<>();
            for (Long skillId : dto.getSkillTagIds()) {
                ProjectSkill skill = new ProjectSkill();
                skill.setProjectId(project.getId());
                skill.setSkillTagId(skillId);
                projectSkills.add(skill);
            }
            projectSkillService.saveBatch(projectSkills);
        }

        // 3. 将创始人（队长）添加至 TeamMember 表
        TeamMember captain = new TeamMember();
        captain.setProjectId(project.getId());
        captain.setUserId(dto.getCreatorId());
        captain.setRole("leader");
        captain.setStatus("active");
        teamMemberService.save(captain);

        return project;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startProject(Long projectId, Long operatorId) {
        Project project = this.getById(projectId);
        if (project == null) {
            throw new RuntimeException("该项目不存在");
        }
        if (!project.getCreatorId().equals(operatorId)) {
            throw new RuntimeException("只有队长可以更改项目状态");
        }
        if (!"recruiting".equals(project.getStatus())) {
            throw new RuntimeException("当前状态不可进入组队中");
        }

        // 修改状态为处理中并计算 evalDeadline (+7天等业务逻辑如果需要的话，按照Schema是 ended_at + 7，先设状态)
        project.setStatus("in_progress");
        this.updateById(project);

        // T-133: 将该项目所有 pending 请求置为 expired
        teamRequestService.update(new LambdaUpdateWrapper<TeamRequest>()
                .eq(TeamRequest::getProjectId, projectId)
                .eq(TeamRequest::getStatus, "pending")
                .set(TeamRequest::getStatus, "expired")
                .set(TeamRequest::getHandledAt, LocalDateTime.now()));
    }

    /**
     * T-118: 结束项目，in_progress -> ended，写 endedAt，evalDeadline = endedAt + 7天
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void endProject(Long projectId, Long operatorId) {
        Project project = this.getById(projectId);
        if (project == null) {
            throw new RuntimeException("该项目不存在");
        }
        if (!project.getCreatorId().equals(operatorId)) {
            throw new RuntimeException("只有队长可以结束项目");
        }
        if (!"in_progress".equals(project.getStatus())) {
            throw new RuntimeException("只有进行中的项目才能结束");
        }

        LocalDateTime now = LocalDateTime.now();
        project.setStatus("ended");
        project.setEndedAt(now);
        project.setEvalDeadline(now.plusDays(7));
        this.updateById(project);
    }

    /**
     * T-119: 互评窗口懒检查 — 如果 evalDeadline 已过期则将状态切为 eval_closed，返回最新状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String checkAndCloseEvalWindow(Long projectId) {
        Project project = this.getById(projectId);
        if (project == null) {
            throw new RuntimeException("该项目不存在");
        }
        if ("ended".equals(project.getStatus())
                && project.getEvalDeadline() != null
                && LocalDateTime.now().isAfter(project.getEvalDeadline())) {
            project.setStatus("eval_closed");
            this.updateById(project);
        }
        return project.getStatus();
    }
}

