package com.teammatch.service;

import com.teammatch.dto.AnomalyDetectionResult;
import com.teammatch.entity.Evaluation;
import com.teammatch.entity.TeamMember;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.TeamMemberMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AnomalyDetectionService 单元测试
 * 测试 M5-4 覆盖全部队友后的项目级异常评价检测
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("异常评价检测服务测试")
class AnomalyDetectionServiceTest {

    private static final Long EVALUATOR_ID = 1L;
    private static final Long PROJECT_ID = 100L;

    @Mock
    private EvaluationMapper evaluationMapper;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @InjectMocks
    private AnomalyDetectionService service;

    @Test
    @DisplayName("未覆盖全部队友时返回 INCOMPLETE_COVERAGE")
    void detect_incompleteCoverage_returnsIncompleteCoverage() {
        when(teamMemberMapper.findActiveMembers(PROJECT_ID))
                .thenReturn(members(EVALUATOR_ID, 2L, 3L));
        when(evaluationMapper.findByEvaluatorAndProject(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(List.of(evaluation(11L, 2L, 1, 1, 1, 1)));

        AnomalyDetectionResult result = service.detect(EVALUATOR_ID, PROJECT_ID);

        assertThat(result.isCoverageComplete()).isFalse();
        assertThat(result.isTriggered()).isFalse();
        assertThat(result.getReason()).isEqualTo(AnomalyDetectionResult.REASON_INCOMPLETE_COVERAGE);
        assertThat(result.getAffectedEvaluationIds()).isEmpty();
    }

    @Test
    @DisplayName("覆盖全部队友且每条评价均分都小于等于2时返回 EXTREME_LOW")
    void detect_allCoveredAndAllAverageAtMostTwo_returnsExtremeLow() {
        when(teamMemberMapper.findActiveMembers(PROJECT_ID))
                .thenReturn(members(EVALUATOR_ID, 2L, 3L));
        when(evaluationMapper.findByEvaluatorAndProject(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(Arrays.asList(
                        evaluation(11L, 2L, 1, 1, 1, 1),
                        evaluation(12L, 3L, 2, 2, 2, 2)
                ));

        AnomalyDetectionResult result = service.detect(EVALUATOR_ID, PROJECT_ID);

        assertThat(result.isCoverageComplete()).isTrue();
        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getReason()).isEqualTo(AnomalyDetectionResult.REASON_EXTREME_LOW);
        assertThat(result.getAffectedEvaluationIds()).containsExactly(11L, 12L);
    }

    @Test
    @DisplayName("覆盖全部队友且每条评价均分都等于5时返回 EXTREME_PERFECT")
    void detect_allCoveredAndAllAverageEqualsFive_returnsExtremePerfect() {
        when(teamMemberMapper.findActiveMembers(PROJECT_ID))
                .thenReturn(members(EVALUATOR_ID, 2L, 3L));
        when(evaluationMapper.findByEvaluatorAndProject(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(Arrays.asList(
                        evaluation(11L, 2L, 5, 5, 5, 5),
                        evaluation(12L, 3L, 5, 5, 5, 5)
                ));

        AnomalyDetectionResult result = service.detect(EVALUATOR_ID, PROJECT_ID);

        assertThat(result.isCoverageComplete()).isTrue();
        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getReason()).isEqualTo(AnomalyDetectionResult.REASON_EXTREME_PERFECT);
        assertThat(result.getAffectedEvaluationIds()).containsExactly(11L, 12L);
    }

    @Test
    @DisplayName("覆盖全部队友但部分评价均分不满足全低分时返回 NORMAL")
    void detect_allCoveredAndMixedAverage_returnsNormal() {
        when(teamMemberMapper.findActiveMembers(PROJECT_ID))
                .thenReturn(members(EVALUATOR_ID, 2L, 3L));
        when(evaluationMapper.findByEvaluatorAndProject(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(Arrays.asList(
                        evaluation(11L, 2L, 1, 1, 1, 1),
                        evaluation(12L, 3L, 3, 3, 3, 3)
                ));

        AnomalyDetectionResult result = service.detect(EVALUATOR_ID, PROJECT_ID);

        assertThat(result.isCoverageComplete()).isTrue();
        assertThat(result.isTriggered()).isFalse();
        assertThat(result.getReason()).isEqualTo(AnomalyDetectionResult.REASON_NONE);
        assertThat(result.getAffectedEvaluationIds()).isEmpty();
    }

    @Test
    @DisplayName("均分恰好2.0时触发 EXTREME_LOW")
    void detect_averageExactlyTwo_returnsExtremeLow() {
        when(teamMemberMapper.findActiveMembers(PROJECT_ID))
                .thenReturn(members(EVALUATOR_ID, 2L, 3L));
        when(evaluationMapper.findByEvaluatorAndProject(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(Arrays.asList(
                        evaluation(11L, 2L, 1, 2, 2, 3),
                        evaluation(12L, 3L, 2, 2, 2, 2)
                ));

        AnomalyDetectionResult result = service.detect(EVALUATOR_ID, PROJECT_ID);

        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getReason()).isEqualTo(AnomalyDetectionResult.REASON_EXTREME_LOW);
        assertThat(result.getAffectedEvaluationIds()).containsExactly(11L, 12L);
    }

    @Test
    @DisplayName("均分恰好5.0时触发 EXTREME_PERFECT")
    void detect_averageExactlyFive_returnsExtremePerfect() {
        when(teamMemberMapper.findActiveMembers(PROJECT_ID))
                .thenReturn(members(EVALUATOR_ID, 2L, 3L));
        when(evaluationMapper.findByEvaluatorAndProject(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(Arrays.asList(
                        evaluation(11L, 2L, 5, 5, 5, 5),
                        evaluation(12L, 3L, 5, 5, 5, 5)
                ));

        AnomalyDetectionResult result = service.detect(EVALUATOR_ID, PROJECT_ID);

        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getReason()).isEqualTo(AnomalyDetectionResult.REASON_EXTREME_PERFECT);
        assertThat(result.getAffectedEvaluationIds()).containsExactly(11L, 12L);
    }

    @Test
    @DisplayName("仅两名项目成员时评价唯一队友即完成覆盖")
    void detect_twoProjectMembersAndOneEvaluation_completesCoverage() {
        when(teamMemberMapper.findActiveMembers(PROJECT_ID))
                .thenReturn(members(EVALUATOR_ID, 2L));
        when(evaluationMapper.findByEvaluatorAndProject(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(List.of(evaluation(11L, 2L, 2, 2, 2, 2)));

        AnomalyDetectionResult result = service.detect(EVALUATOR_ID, PROJECT_ID);

        assertThat(result.isCoverageComplete()).isTrue();
        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getReason()).isEqualTo(AnomalyDetectionResult.REASON_EXTREME_LOW);
        assertThat(result.getAffectedEvaluationIds()).containsExactly(11L);
    }

    @Test
    @DisplayName("没有应评价队友时返回 NORMAL 且不查询评价列表")
    void detect_noExpectedTargets_returnsNormalWithoutQueryingEvaluations() {
        when(teamMemberMapper.findActiveMembers(PROJECT_ID))
                .thenReturn(Collections.singletonList(member(EVALUATOR_ID)));

        AnomalyDetectionResult result = service.detect(EVALUATOR_ID, PROJECT_ID);

        assertThat(result.isCoverageComplete()).isTrue();
        assertThat(result.isTriggered()).isFalse();
        assertThat(result.getReason()).isEqualTo(AnomalyDetectionResult.REASON_NONE);
        assertThat(result.getAffectedEvaluationIds()).isEmpty();
        verify(evaluationMapper, never()).findByEvaluatorAndProject(EVALUATOR_ID, PROJECT_ID);
    }

    private static List<TeamMember> members(Long... userIds) {
        return Arrays.stream(userIds)
                .map(AnomalyDetectionServiceTest::member)
                .toList();
    }

    private static TeamMember member(Long userId) {
        TeamMember member = new TeamMember();
        member.setUserId(userId);
        member.setProjectId(PROJECT_ID);
        member.setStatus("active");
        return member;
    }

    private static Evaluation evaluation(Long id, Long targetId,
                                         int communicationScore, int taskScore,
                                         int skillScore, int responsibilityScore) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(id);
        evaluation.setProjectId(PROJECT_ID);
        evaluation.setEvaluatorId(EVALUATOR_ID);
        evaluation.setTargetId(targetId);
        evaluation.setCommunicationScore(communicationScore);
        evaluation.setTaskScore(taskScore);
        evaluation.setSkillScore(skillScore);
        evaluation.setResponsibilityScore(responsibilityScore);
        return evaluation;
    }
}
