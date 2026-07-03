package com.teammatch.m4.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.m4.dto.ProjectCreateDTO;
import com.teammatch.m4.entity.Project;
import com.teammatch.m4.entity.ProjectSkill;
import com.teammatch.m4.entity.TeamMember;
import com.teammatch.m4.service.ProjectService;
import com.teammatch.m4.service.ProjectSkillService;
import com.teammatch.m4.service.TeamMemberService;
import com.teammatch.entity.SkillTag;
import com.teammatch.mapper.SkillTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/m4/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final TeamMemberService teamMemberService;
    private final ProjectSkillService projectSkillService;
    private final SkillTagMapper skillTagMapper;

    /** T-113 创建项目 */
    @PostMapping
    public Result<Project> createProject(@RequestBody ProjectCreateDTO dto) {
        try {
            Project project = projectService.createProject(dto);
            return Result.success(project);
        } catch (Exception e) {
            return Result.fail(ReasonCode.PARAM_ERROR);
        }
    }

    /** T-114 项目列表（分页 + 状态筛选） */
    @GetMapping
    public Result<Page<Project>> getProjectList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Project::getStatus, status);
        }
        wrapper.orderByDesc(Project::getCreatedAt);
        Page<Project> page = projectService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(page);
    }

    /** T-115 项目详情 */
    @GetMapping("/{id}")
    public Result<Project> getProjectInfo(@PathVariable Long id) {
        Project project = projectService.getById(id);
        if (project == null) {
            return Result.fail(ReasonCode.M4_PROJECT_NOT_FOUND);
        }
        // 填充技能标签
        List<Long> skillTagIds = projectSkillService.list(
                new LambdaQueryWrapper<ProjectSkill>().eq(ProjectSkill::getProjectId, id))
                .stream().map(ProjectSkill::getSkillTagId).collect(Collectors.toList());
        if (!skillTagIds.isEmpty()) {
            project.setSkills(skillTagMapper.selectBatchIds(skillTagIds));
        }
        return Result.success(project);
    }

    /** T-116 更新项目信息 */
    @PutMapping("/{id}")
    public Result<Void> updateProjectInfo(@PathVariable Long id, @RequestBody Project updateData) {
        updateData.setId(id);
        boolean success = projectService.updateById(updateData);
        return success ? Result.success() : Result.fail(ReasonCode.M4_PROJECT_NOT_FOUND);
    }

    /** T-117 开始项目 (recruiting -> in_progress) */
    @PostMapping("/{id}/start")
    public Result<Void> startProject(@PathVariable Long id, @RequestParam Long operatorId) {
        try {
            projectService.startProject(id, operatorId);
            return Result.success();
        } catch (Exception e) {
            return Result.fail(ReasonCode.M4_PROJECT_STATUS_INVALID);
        }
    }

    /** T-118 结束项目 (in_progress -> ended) */
    @PostMapping("/{id}/end")
    public Result<Void> endProject(@PathVariable Long id, @RequestParam Long operatorId) {
        try {
            projectService.endProject(id, operatorId);
            return Result.success();
        } catch (Exception e) {
            return Result.fail(ReasonCode.M4_PROJECT_STATUS_INVALID);
        }
    }

    /** T-119 互评窗口懒检查 */
    @GetMapping("/{id}/eval-status")
    public Result<String> checkEvalStatus(@PathVariable Long id) {
        try {
            String status = projectService.checkAndCloseEvalWindow(id);
            return Result.success(status);
        } catch (Exception e) {
            return Result.fail(ReasonCode.M4_PROJECT_NOT_FOUND);
        }
    }

    /** T-132 项目成员列表 */
    @GetMapping("/{id}/members")
    public Result<?> getMembers(@PathVariable Long id) {
        return Result.success(teamMemberService.getProjectMembers(id));
    }
}

