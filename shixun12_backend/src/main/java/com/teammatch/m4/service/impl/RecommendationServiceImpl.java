package com.teammatch.m4.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teammatch.dto.EndorsementPoint;
import com.teammatch.dto.RecommendationItem;
import com.teammatch.entity.SkillTag;
import com.teammatch.entity.TechProfile;
import com.teammatch.entity.User;
import com.teammatch.entity.UserSkill;
import com.teammatch.m4.entity.Project;
import com.teammatch.m4.entity.ProjectSkill;
import com.teammatch.m4.entity.TeamMember;
import com.teammatch.m4.entity.TeamRequest;
import com.teammatch.m4.service.*;
import com.teammatch.mapper.*;
import com.teammatch.service.SkillAuthorityService;
import com.teammatch.service.storage.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * M4 推荐服务实现
 *
 * <p>三阶段级联过滤:
 * <ol>
 *   <li>阶段一(SQL 层硬约束):credit_score ≥ 80、status=active、formal_profile_completed、
 *       排除队长本人、排除已在项目中的 active 成员、排除已有 pending 邀请的目标人、
 *       Jaccard &lt; 0.15 过滤</li>
 *   <li>阶段二(粗排):按 Jaccard 技能相似度排序</li>
 *   <li>阶段三(精排):finalScore = Jaccard × 技术能力可信度 × 合作可信度</li>
 * </ol>
 *
 * <p>N+1 防控:关联表全部批量预取,单次推荐 SQL 总数固定,不随候选人数增长。
 */
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final int CREDIT_HARD_MIN = 80;
    private static final double JACCARD_HARD_MIN = 0.15;
    private static final double MAX_TECH_SCORE = 2000.0;

    private final ProjectService projectService;
    private final ProjectSkillService projectSkillService;
    private final TeamMemberService teamMemberService;
    private final TeamRequestService teamRequestService;

    private final UserMapper userMapper;
    private final TechProfileMapper techProfileMapper;
    private final UserSkillMapper userSkillMapper;
    private final SkillTagMapper skillTagMapper;
    private final EvaluationMapper evaluationMapper;

    private final SkillAuthorityService skillAuthorityService;

    private final OssService ossService;

    @Override
    public List<RecommendationItem> recommend(Long projectId, int limit) {
        // ---------- 0. 项目存在性 + 技能列表 ----------
        Project project = projectService.getById(projectId);
        if (project == null) return Collections.emptyList();

        List<ProjectSkill> projectSkillRows = projectSkillService.list(
                new LambdaQueryWrapper<ProjectSkill>().eq(ProjectSkill::getProjectId, projectId));
        if (projectSkillRows.isEmpty()) return Collections.emptyList();

        Set<Long> projectSkillIds = projectSkillRows.stream()
                .map(ProjectSkill::getSkillTagId)
                .collect(Collectors.toSet());

        // ---------- 1. 阶段一:SQL 硬约束过滤 ----------
        List<Long> activeMemberIds = teamMemberService.list(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getProjectId, projectId)
                        .eq(TeamMember::getStatus, "active"))
                .stream().map(TeamMember::getUserId).toList();

        List<Long> pendingInviteTargetIds = teamRequestService.list(
                new LambdaQueryWrapper<TeamRequest>()
                        .eq(TeamRequest::getProjectId, projectId)
                        .eq(TeamRequest::getRequestType, "invite")
                        .eq(TeamRequest::getStatus, "pending"))
                .stream().map(TeamRequest::getToUserId).toList();

        LambdaQueryWrapper<User> candidateWrapper = new LambdaQueryWrapper<User>()
                .ge(User::getCreditScore, CREDIT_HARD_MIN)
                .eq(User::getStatus, "active")
                .eq(User::getFormalProfileCompleted, Boolean.TRUE)
                .ne(User::getId, project.getCreatorId());
        if (!activeMemberIds.isEmpty()) {
            candidateWrapper.notIn(User::getId, activeMemberIds);
        }
        if (!pendingInviteTargetIds.isEmpty()) {
            candidateWrapper.notIn(User::getId, pendingInviteTargetIds);
        }
        List<User> candidates = userMapper.selectList(candidateWrapper);
        if (candidates.isEmpty()) return Collections.emptyList();

        List<Long> candidateIds = candidates.stream().map(User::getId).toList();

        // ---------- 2. 批量预取(消除 N+1) ----------

        // (a) tech_profile:按 user.techProfileId 批量查(比 claimedByUserId 更稳)
        List<Long> techProfileIds = candidates.stream()
                .map(User::getTechProfileId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, TechProfile> techProfileById = techProfileIds.isEmpty()
                ? Collections.emptyMap()
                : techProfileMapper.selectBatchIds(techProfileIds).stream()
                        .collect(Collectors.toMap(TechProfile::getId, tp -> tp, (a, b) -> a));
        Map<Long, TechProfile> techProfileByUser = new HashMap<>();
        for (User u : candidates) {
            if (u.getTechProfileId() != null) {
                TechProfile tp = techProfileById.get(u.getTechProfileId());
                if (tp != null) techProfileByUser.put(u.getId(), tp);
            }
        }

        // (b) user_skill:按 userId 批量查
        Map<Long, Set<Long>> skillsByUser = userSkillMapper.selectList(
                new LambdaQueryWrapper<UserSkill>().in(UserSkill::getUserId, candidateIds))
                .stream()
                .collect(Collectors.groupingBy(
                        UserSkill::getUserId,
                        Collectors.mapping(UserSkill::getSkillTagId, Collectors.toSet())));

        // (c) evaluation 互评技术分:批量查所有候选人的
        Map<Long, List<EndorsementPoint>> pointsByUser = evaluationMapper
                .findSkillScoresByTargets(candidateIds)
                .stream()
                .collect(Collectors.groupingBy(EndorsementPoint::getTargetUserId));

        // (d) skill_tag 名称:一次性把涉及到的所有 skill id 拿出来
        Set<Long> allSkillIds = new HashSet<>(projectSkillIds);
        skillsByUser.values().forEach(allSkillIds::addAll);
        Map<Long, String> skillNameMap = allSkillIds.isEmpty()
                ? Collections.emptyMap()
                : skillTagMapper.selectBatchIds(allSkillIds).stream()
                        .collect(Collectors.toMap(SkillTag::getId, SkillTag::getName));

        // ---------- 3. 阶段二 + 阶段三:遍历计算 finalScore ----------
        List<RecommendationItem> items = new ArrayList<>();
        for (User u : candidates) {
            Set<Long> userSkillIds = skillsByUser.getOrDefault(u.getId(), Collections.emptySet());

            Set<Long> overlapSet = new HashSet<>(projectSkillIds);
            overlapSet.retainAll(userSkillIds);
            double jaccard = computeJaccard(projectSkillIds, userSkillIds, overlapSet);

            // Jaccard 硬约束
            if (jaccard < JACCARD_HARD_MIN) continue;

            List<Long> overlapping = new ArrayList<>(overlapSet);

            // techScore 先验
            TechProfile tp = techProfileByUser.get(u.getId());
            Double techPrior = (tp != null && tp.getTechScore() != null && tp.getTechScore() > 0)
                    ? Math.min(tp.getTechScore() / MAX_TECH_SCORE, 1.0)
                    : null;

            List<EndorsementPoint> points = pointsByUser.getOrDefault(u.getId(), Collections.emptyList());
            double authority = skillAuthorityService.computeAuthority(points, overlapping, techPrior);

            double trustFactor = Math.min(u.getCreditScore() / 100.0, 1.0);
            double finalScore = jaccard * authority * trustFactor;

            String breakdown = buildBreakdown(points);

            List<String> matchedNames = overlapping.stream()
                    .map(skillNameMap::get)
                    .filter(Objects::nonNull)
                    .toList();

            RecommendationItem item = new RecommendationItem();
            item.setUserId(u.getId());
            item.setNickname(u.getNickname());
            item.setAvatarUrl(ossService.resolveAvatarUrl(u.getAvatarUrl()));
            item.setCreditScore(u.getCreditScore());
            item.setTechScore(tp != null && tp.getTechScore() != null ? tp.getTechScore() : 0);
            item.setJaccardSimilarity(round2(jaccard));
            item.setTechAuthority(round2(authority));
            item.setTrustFactor(round2(trustFactor));
            item.setFinalScore(round2(finalScore));
            item.setMatchedSkills(matchedNames);
            item.setPassedHardFilter(Boolean.TRUE);
            item.setAuthorityBreakdown(breakdown);
            items.add(item);
        }

        // ---------- 4. 排序 + 截断 ----------
        items.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));
        int end = Math.min(limit, items.size());
        return items.subList(0, end);
    }

    // ==================== 工具方法 ====================

    private double computeJaccard(Set<Long> projectSkills, Set<Long> userSkills, Set<Long> overlap) {
        if (projectSkills.isEmpty()) return 1.0;
        Set<Long> union = new HashSet<>(projectSkills);
        union.addAll(userSkills);
        if (union.isEmpty()) return 0.0;
        return (double) overlap.size() / union.size();
    }

    private String buildBreakdown(List<EndorsementPoint> points) {
        if (points == null || points.isEmpty()) return "暂无互评数据";
        long validCount = points.stream()
                .filter(p -> p.getEvaluatorCreditScore() != null && p.getEvaluatorCreditScore() >= 30)
                .count();
        if (validCount == 0) return "互评者信誉不足,未计入";
        double avgCredit = points.stream()
                .filter(p -> p.getEvaluatorCreditScore() != null && p.getEvaluatorCreditScore() >= 30)
                .mapToInt(EndorsementPoint::getEvaluatorCreditScore)
                .average().orElse(0);
        return validCount + "次互评认证,加权信誉" + Math.round(avgCredit);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
