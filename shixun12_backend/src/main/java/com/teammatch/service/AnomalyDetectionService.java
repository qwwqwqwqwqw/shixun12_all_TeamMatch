package com.teammatch.service;

import com.teammatch.dto.AnomalyDetectionResult;
import com.teammatch.entity.Evaluation;
import com.teammatch.entity.TeamMember;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.TeamMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 异常评价检测服务
 * 实现 M5-4 覆盖全部队友后的项目级异常检测
 */
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private static final double EXTREME_LOW_AVERAGE_THRESHOLD = 2.0;
    private static final double PERFECT_AVERAGE_SCORE = 5.0;
    private static final int SCORE_DIMENSION_COUNT = 4;

    private final EvaluationMapper evaluationMapper;

    private final TeamMemberMapper teamMemberMapper;

    /**
     * 按评价者和项目检测异常评价。
     *
     * @param evaluatorId 评价者用户 ID
     * @param projectId 项目 ID
     * @return 异常检测结果
     */
    public AnomalyDetectionResult detect(Long evaluatorId, Long projectId) {
        List<TeamMember> activeMembers = safeList(teamMemberMapper.findActiveMembers(projectId));
        List<Long> expectedTargetIds = activeMembers.stream()
                .map(TeamMember::getUserId)
                .filter(Objects::nonNull)
                .filter(userId -> !Objects.equals(userId, evaluatorId))
                .distinct()
                .toList();

        if (expectedTargetIds.isEmpty()) {
            return AnomalyDetectionResult.normal();
        }

        List<Evaluation> submittedEvaluations =
                safeList(evaluationMapper.findByEvaluatorAndProject(evaluatorId, projectId));
        Set<Long> submittedTargetIds = submittedEvaluations.stream()
                .map(Evaluation::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!submittedTargetIds.containsAll(expectedTargetIds)) {
            return AnomalyDetectionResult.incompleteCoverage();
        }

        List<Long> affectedEvaluationIds = submittedEvaluations.stream()
                .map(Evaluation::getId)
                .filter(Objects::nonNull)
                .toList();

        if (submittedEvaluations.stream().allMatch(this::isExtremeLow)) {
            return AnomalyDetectionResult.extremeLow(affectedEvaluationIds);
        }

        if (submittedEvaluations.stream().allMatch(this::isExtremePerfect)) {
            return AnomalyDetectionResult.extremePerfect(affectedEvaluationIds);
        }

        return AnomalyDetectionResult.normal();
    }

    private boolean isExtremeLow(Evaluation evaluation) {
        return averageScore(evaluation) <= EXTREME_LOW_AVERAGE_THRESHOLD;
    }

    private boolean isExtremePerfect(Evaluation evaluation) {
        return Double.compare(averageScore(evaluation), PERFECT_AVERAGE_SCORE) == 0;
    }

    private double averageScore(Evaluation evaluation) {
        int total = evaluation.getCommunicationScore()
                + evaluation.getTaskScore()
                + evaluation.getSkillScore()
                + evaluation.getResponsibilityScore();
        return total / (double) SCORE_DIMENSION_COUNT;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
