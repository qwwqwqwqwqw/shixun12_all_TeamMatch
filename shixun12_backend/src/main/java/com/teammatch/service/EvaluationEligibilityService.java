package com.teammatch.service;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.EvaluatableMemberDTO;
import com.teammatch.entity.Project;
import com.teammatch.entity.TeamMember;
import com.teammatch.entity.User;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.ProjectMapper;
import com.teammatch.mapper.TeamMemberMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.storage.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 互评资格判断服务
 * 实现 M5-1A/B/C 的核心业务逻辑
 */
@Service
@RequiredArgsConstructor
public class EvaluationEligibilityService {

    private final ProjectMapper projectMapper;

    private final TeamMemberMapper teamMemberMapper;

    private final EvaluationMapper evaluationMapper;

    private final UserMapper userMapper;

    private final OssService ossService;

    /**
     * M5-1A：项目级互评入口判断
     * 判断用户是否可以进入该项目的互评页面
     *
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return Result<Boolean> - 成功返回 true，失败返回对应错误码
     */
    public Result<Boolean> checkProjectEligibility(Long userId, Long projectId) {
        // 1. 检查项目是否存在
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return Result.fail(ReasonCode.PROJECT_NOT_FOUND);
        }

        // 2. 检查用户是否是项目的 active 成员
        TeamMember member = teamMemberMapper.findActiveMember(userId, projectId);
        if (member == null) {
            return Result.fail(ReasonCode.NOT_PROJECT_MEMBER);
        }

        String status = project.getStatus();
        // 3. 限制项目状态：必须是 ended（互评中）或 eval_closed（互评已截止）之一
        if (!"ended".equals(status) && !"eval_closed".equals(status)) {
            return Result.fail(ReasonCode.PROJECT_NOT_ENDED);
        }

        // 4. 状态短路校验：如果已经是 eval_closed 状态，直接短路返回“窗口已关闭”
        if ("eval_closed".equals(status)) {
            return Result.fail(ReasonCode.EVAL_WINDOW_CLOSED);
        }

        // 5. 互评时间窗口懒检查：若 eval_deadline 到期，则自动触发状态更新
        LocalDateTime evalDeadline = project.getEvalDeadline();
        if (evalDeadline == null) {
            return Result.fail(ReasonCode.EVAL_WINDOW_CLOSED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(evalDeadline)) {
            projectMapper.updateStatusToEvalClosed(projectId);
            return Result.fail(ReasonCode.EVAL_WINDOW_CLOSED);
        }

        // 5. 所有检查通过，允许进入互评
        return Result.success(true);
    }

    /**
     * M5-1B：待评价成员列表判断
     * 返回当前用户在该项目中可以评价的成员列表
     *
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return Result<List<EvaluatableMemberDTO>> - 成功返回成员列表，失败返回对应错误码
     */
    public Result<List<EvaluatableMemberDTO>> getEvaluatableMembers(Long userId, Long projectId) {
        // 1. 先检查项目级资格
        Result<Boolean> eligibility = checkProjectEligibility(userId, projectId);
        if (eligibility.isFail()) {
            // 直接根据错误码字符串查找对应的 ReasonCode 枚举
            ReasonCode reasonCode = ReasonCode.fromCode(eligibility.getCode());
            return Result.fail(reasonCode);
        }

        // 2. 查询项目的所有 active 成员
        List<TeamMember> activeMembers = teamMemberMapper.findActiveMembers(projectId);

        // 3. 查询当前用户已评价的目标用户 ID 列表
        List<Long> evaluatedTargetIds = evaluationMapper.findEvaluatedTargetIds(userId, projectId);
        Set<Long> evaluatedSet = evaluatedTargetIds.stream().collect(Collectors.toSet());

        // 4. 收集需要查询的用户 ID（排除自己）
        List<Long> userIds = activeMembers.stream()
                .map(TeamMember::getUserId)
                .filter(id -> !id.equals(userId))
                .collect(Collectors.toList());

        // 5. 批量查询用户信息
        Map<Long, User> userMap;
        if (userIds.isEmpty()) {
            userMap = Map.of();
        } else {
            List<User> users = userMapper.findByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, user -> user));
        }

        // 6. 构建可评价成员列表
        List<EvaluatableMemberDTO> result = new ArrayList<>();
        for (TeamMember member : activeMembers) {
            // 排除自己
            if (member.getUserId().equals(userId)) {
                continue;
            }

            // 构建 DTO
            User user = userMap.get(member.getUserId());
            EvaluatableMemberDTO dto = new EvaluatableMemberDTO();
            dto.setUserId(member.getUserId());
            dto.setNickname(user != null ? user.getNickname() : "未知用户");
            dto.setAvatarUrl(user != null ? ossService.resolveAvatarUrl(user.getAvatarUrl()) : null);
            dto.setEvaluated(evaluatedSet.contains(member.getUserId()));

            result.add(dto);
        }

        return Result.success(result);
    }

    /**
     * M5-1C：最终提交前资格兜底校验
     * 在用户提交评价前进行最后一道防线检查
     *
     * @param evaluatorId 评价人 ID
     * @param targetId 被评价人 ID
     * @param projectId 项目 ID
     * @return Result<Void> - 成功返回 success，失败返回对应错误码
     */
    public Result<Void> validateSubmission(Long evaluatorId, Long targetId, Long projectId) {
        // 1. 检查是否自评
        if (evaluatorId.equals(targetId)) {
            return Result.fail(ReasonCode.SELF_EVALUATION);
        }

        // 2. 检查是否重复评价
        if (evaluationMapper.findEvaluation(evaluatorId, targetId, projectId) != null) {
            return Result.fail(ReasonCode.ALREADY_EVALUATED);
        }

        // 3. 检查评价人是否是项目的 active 成员
        TeamMember evaluator = teamMemberMapper.findActiveMember(evaluatorId, projectId);
        if (evaluator == null) {
            return Result.fail(ReasonCode.NOT_PROJECT_MEMBER);
        }

        // 4. 检查被评价人是否是项目的 active 成员
        TeamMember target = teamMemberMapper.findActiveMember(targetId, projectId);
        if (target == null) {
            return Result.fail(ReasonCode.TARGET_NOT_PROJECT_MEMBER);
        }

        // 5. 检查项目级资格（项目状态、互评窗口）
        Result<Boolean> eligibility = checkProjectEligibility(evaluatorId, projectId);
        if (eligibility.isFail()) {
            // 直接根据错误码字符串查找对应的 ReasonCode 枚举
            ReasonCode reasonCode = ReasonCode.fromCode(eligibility.getCode());
            return Result.fail(reasonCode);
        }

        // 6. 所有检查通过
        return Result.success();
    }
}
