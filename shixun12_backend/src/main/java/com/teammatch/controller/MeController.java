package com.teammatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teammatch.common.Result;
import com.teammatch.dto.UserBadgesVO;
import com.teammatch.entity.Project;
import com.teammatch.entity.Evaluation;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.ProjectMapper;
import com.teammatch.mapper.TeamMemberMapper;
import com.teammatch.m4.entity.ExitVote;
import com.teammatch.m4.entity.ExitVoteRecord;
import com.teammatch.entity.TeamMember;
import com.teammatch.m4.entity.TeamRequest;
import com.teammatch.mapper.UserMapper;
import com.teammatch.m4.mapper.M4ExitVoteMapper;
import com.teammatch.m4.mapper.M4ExitVoteRecordMapper;
import com.teammatch.m4.mapper.M4TeamRequestMapper;
import com.teammatch.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * M3 用户中心控制器
 * 聚合展示当前用户维度的待处理事项（角标/队列）
 */
@RestController
public class MeController {

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private M4TeamRequestMapper teamRequestMapper;

    @Autowired
    private M4ExitVoteMapper exitVoteMapper;

    @Autowired
    private M4ExitVoteRecordMapper exitVoteRecordMapper;

    @Autowired
    private EvaluationMapper evaluationMapper;

    @Autowired
    private TeamMemberMapper teamMemberMapper;

    @Autowired
    private ProjectMapper projectMapper;

    /**
     * 获取当前用户的角标聚合
     * GET /api/me/badges
     *
     * 统计待处理的：组队请求、退出投票、互评
     */
    @GetMapping("/me/badges")
    public Result<UserBadgesVO> getBadges(@RequestHeader("Authorization") String token) {
        Long userId = authUtil.requireUserId(token);

        // 1. 统计待处理的组队请求（邀请/申请）
        LambdaQueryWrapper<TeamRequest> inviteWrapper = new LambdaQueryWrapper<>();
        inviteWrapper.eq(TeamRequest::getToUserId, userId)
                .eq(TeamRequest::getStatus, "pending");
        long pendingInvites = teamRequestMapper.selectCount(inviteWrapper);

        // 2. 统计待处理的退出投票
        // 先查出用户活跃的项目
        LambdaQueryWrapper<TeamMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(TeamMember::getUserId, userId)
                .eq(TeamMember::getStatus, "active");
        List<TeamMember> activeMemberships = teamMemberMapper.selectList(memberWrapper);
        List<Long> projectIds = activeMemberships.stream()
                .map(TeamMember::getProjectId)
                .collect(Collectors.toList());

        int pendingVotes = 0;
        if (!projectIds.isEmpty()) {
            LambdaQueryWrapper<ExitVote> voteWrapper = new LambdaQueryWrapper<>();
            voteWrapper.in(ExitVote::getProjectId, projectIds)
                    .eq(ExitVote::getStatus, "voting");
            List<ExitVote> activeVotes = exitVoteMapper.selectList(voteWrapper);

            for (ExitVote vote : activeVotes) {
                // 检查当前用户是否已经投过票
                LambdaQueryWrapper<ExitVoteRecord> recordWrapper = new LambdaQueryWrapper<>();
                recordWrapper.eq(ExitVoteRecord::getVoteId, vote.getId())
                        .eq(ExitVoteRecord::getVoterId, userId);
                Long votedCount = exitVoteRecordMapper.selectCount(recordWrapper);
                if (votedCount == 0) {
                    pendingVotes++;
                }
            }
        }

        // 3. 统计待完成的互评：过滤出 ended 且尚未截止的项目，批量拉取数据在内存中计算差集
        int pendingEvaluations = 0;
        if (!projectIds.isEmpty()) {
            // ① 批量查出用户所有 active 的项目详情
            List<Project> activeProjects = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                    .in(Project::getId, projectIds));

            // ② 内存过滤：筛选出 status='ended' 且互评窗口尚未过期的项目 ID
            // 注意：此处的 now 是后端时间；用于兜底拦截“已过期且懒更新未触发”的项目，避免误报红点。
            LocalDateTime now = LocalDateTime.now();
            List<Long> endedProjectIds = activeProjects.stream()
                    .filter(p -> "ended".equals(p.getStatus()))
                    .filter(p -> p.getEvalDeadline() != null && !now.isAfter(p.getEvalDeadline()))
                    .map(Project::getId)
                    .collect(Collectors.toList());

            if (!endedProjectIds.isEmpty()) {
                // ③ 批量拉取当前用户在这些 ended 项目下已提交的所有评价（避免 N+1 查询）
                List<Evaluation> evalsGiven = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getEvaluatorId, userId)
                        .in(Evaluation::getProjectId, endedProjectIds));
                
                // 映射成联合键 projectId:targetId
                Set<String> evaluatedKeys = evalsGiven.stream()
                        .map(e -> e.getProjectId() + ":" + e.getTargetId())
                        .collect(Collectors.toSet());

                // ④ 批量查出这些项目中的所有其他 active 成员（避免嵌套循环内的 N+1 查询）
                List<TeamMember> otherMembers = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                        .in(TeamMember::getProjectId, endedProjectIds)
                        .eq(TeamMember::getStatus, "active")
                        .ne(TeamMember::getUserId, userId));

                // ⑤ 内存计算差集：累计未评价的成员数
                for (TeamMember member : otherMembers) {
                    String key = member.getProjectId() + ":" + member.getUserId();
                    if (!evaluatedKeys.contains(key)) {
                        pendingEvaluations++;
                    }
                }
            }
        }

        return Result.success(new UserBadgesVO((int) pendingInvites, pendingVotes, pendingEvaluations));
    }
}
