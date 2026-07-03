package com.teammatch.service;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.AnomalyDetectionResult;
import com.teammatch.dto.CreditCalculationResult;
import com.teammatch.dto.EvaluationSubmitDTO;
import com.teammatch.dto.EvaluationSubmitResult;
import com.teammatch.entity.CreditChange;
import com.teammatch.entity.Evaluation;
import com.teammatch.exception.BusinessException;
import com.teammatch.mapper.CreditChangeMapper;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.EvaluationTagMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.impl.EvaluationSubmitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EvaluationSubmitService 集成测试
 * 覆盖 M5-5 正常提交、异常挂起、校验失败、重复评价拦截等核心场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("互评提交事务编排服务测试")
class EvaluationSubmitServiceTest {

    private static final Long EVALUATOR_ID = 1L;
    private static final Long TARGET_ID = 2L;
    private static final Long PROJECT_ID = 100L;
    private static final Long EVALUATION_ID = 1000L;

    @Mock
    private EvaluationEligibilityService eligibilityService;

    @Mock
    private EvaluationValidationService validationService;

    @Mock
    private CreditCalculationService creditCalculationService;

    @Mock
    private AnomalyDetectionService anomalyDetectionService;

    @Mock
    private EvaluationMapper evaluationMapper;

    @Mock
    private EvaluationTagMapper evaluationTagMapper;

    @Mock
    private CreditChangeMapper creditChangeMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private EvaluationSubmitServiceImpl service;

    private EvaluationSubmitDTO validDto;
    private CreditCalculationResult creditResult;
    private AnomalyDetectionResult normalAnomalyResult;

    @BeforeEach
    void setUp() {
        validDto = new EvaluationSubmitDTO();
        validDto.setEvaluatorId(EVALUATOR_ID);
        validDto.setTargetId(TARGET_ID);
        validDto.setProjectId(PROJECT_ID);
        validDto.setCommunicationScore(4);
        validDto.setTaskScore(4);
        validDto.setSkillScore(4);
        validDto.setResponsibilityScore(4);
        validDto.setComment("合作愉快，技术能力强");
        validDto.setPositiveTags(Arrays.asList("沟通积极", "技术可靠"));
        validDto.setNegativeTags(Collections.emptyList());

        creditResult = new CreditCalculationResult();
        creditResult.setAverageScore(new BigDecimal("4.00"));
        creditResult.setRawDelta(3);
        creditResult.setExistingProjectDelta(0);
        creditResult.setCappedDelta(3);
        creditResult.setProjectTotalAfterApplied(3);
        creditResult.setCapped(false);

        normalAnomalyResult = AnomalyDetectionResult.normal();

        // Default: DB writes succeed (lenient for tests that short-circuit before DB)
        lenient().when(evaluationMapper.insert(any(Evaluation.class))).thenAnswer(inv -> {
            Evaluation e = inv.getArgument(0);
            e.setId(EVALUATION_ID);
            return 1;
        });
        lenient().when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(1);
        lenient().when(userMapper.updateCreditScore(anyLong(), anyInt())).thenReturn(1);
        lenient().when(evaluationTagMapper.insert(any())).thenReturn(1);
    }

    // ==================== 正常提交 ====================

    @Test
    @DisplayName("正常提交：4分x4，无异常，credit_change.effective=1，credit_score更新")
    void normalSubmit_allChecksPass() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(creditResult);
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(normalAnomalyResult);

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isSuccess()).isTrue();
        EvaluationSubmitResult data = result.getData();
        assertThat(data.getEvaluationId()).isEqualTo(EVALUATION_ID);
        assertThat(data.getEvaluationStatus()).isEqualTo("normal");
        assertThat(data.getEffective()).isTrue();
        assertThat(data.getCreditResult().getCappedDelta()).isEqualTo(3);

        verify(evaluationMapper).insert(any(Evaluation.class));
        verify(evaluationTagMapper, times(2)).insert(any()); // 2 positive tags
        verify(creditChangeMapper).insert(any(CreditChange.class));
        verify(userMapper).updateCreditScore(TARGET_ID, 3);
    }

    @Test
    @DisplayName("正常提交：credit_change 字段完整性")
    void normalSubmit_creditChangeFieldsCorrect() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(creditResult);
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(normalAnomalyResult);

        service.submit(validDto);

        verify(creditChangeMapper).insert(any(CreditChange.class));
        verify(creditChangeMapper).insert(argThat(cc ->
                cc.getProjectId().equals(PROJECT_ID) &&
                cc.getChangeType().equals("evaluation") &&
                cc.getSourceType().equals("evaluation") &&
                cc.getSourceId().equals(EVALUATION_ID) &&
                cc.getUserId().equals(TARGET_ID) &&
                cc.getEffective() == true &&
                cc.getChangeValue() == 3
        ));
    }

    @Test
    @DisplayName("正常提交：无标签时不插入")
    void normalSubmit_noTags() {
        validDto.setPositiveTags(null);
        validDto.setNegativeTags(null);

        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(creditResult);
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(normalAnomalyResult);

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isSuccess()).isTrue();
        verify(evaluationTagMapper, never()).insert(any());
    }

    @Test
    @DisplayName("INCOMPLETE_COVERAGE：不触发异常，走正常分支")
    void incompleteCoverage_goesNormal() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(creditResult);
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(AnomalyDetectionResult.incompleteCoverage());

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getEvaluationStatus()).isEqualTo("normal");
        assertThat(result.getData().getEffective()).isTrue();
        verify(userMapper).updateCreditScore(TARGET_ID, 3);
    }

    // ==================== 异常挂起—单条（无历史） ====================

    @Test
    @DisplayName("异常挂起—全低分单条：effective=false，不更新 credit_score")
    void anomalyExtremeLow_noHistory() {
        validDto.setCommunicationScore(1);
        validDto.setTaskScore(1);
        validDto.setSkillScore(1);
        validDto.setResponsibilityScore(1);
        validDto.setPositiveTags(Collections.emptyList());
        validDto.setNegativeTags(Arrays.asList("沟通差"));
        validDto.setComment("差差差差差差差差差差差差差差差差差差差差"); // 20 chars

        CreditCalculationResult lowCreditResult = new CreditCalculationResult();
        lowCreditResult.setAverageScore(new BigDecimal("1.00"));
        lowCreditResult.setCappedDelta(-5);

        AnomalyDetectionResult anomalyResult = AnomalyDetectionResult.extremeLow(
                List.of(EVALUATION_ID));

        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(lowCreditResult);
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(anomalyResult);
        when(creditChangeMapper.findBySourceTypeAndSourceIds(eq("evaluation"), anyList()))
                .thenReturn(Collections.emptyList());
        when(evaluationMapper.batchUpdateStatus(anyList(), eq("pending_review")))
                .thenReturn(1);

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getEvaluationStatus()).isEqualTo("pending_review");
        assertThat(result.getData().getEffective()).isFalse();

        verify(creditChangeMapper).insert(argThat(cc -> cc.getEffective() == false));
        verify(userMapper, never()).updateCreditScore(anyLong(), eq(-5));
        verify(evaluationMapper).batchUpdateStatus(List.of(EVALUATION_ID), "pending_review");
    }

    @Test
    @DisplayName("异常挂起—全满分单条：effective=false")
    void anomalyExtremePerfect_single() {
        validDto.setCommunicationScore(5);
        validDto.setTaskScore(5);
        validDto.setSkillScore(5);
        validDto.setResponsibilityScore(5);
        validDto.setNegativeTags(Collections.emptyList());
        validDto.setPositiveTags(Arrays.asList("沟通积极"));

        CreditCalculationResult highCreditResult = new CreditCalculationResult();
        highCreditResult.setAverageScore(new BigDecimal("5.00"));
        highCreditResult.setCappedDelta(5);

        AnomalyDetectionResult anomalyResult = AnomalyDetectionResult.extremePerfect(
                List.of(EVALUATION_ID));

        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(highCreditResult);
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(anomalyResult);
        when(creditChangeMapper.findBySourceTypeAndSourceIds(eq("evaluation"), anyList()))
                .thenReturn(Collections.emptyList());
        when(evaluationMapper.batchUpdateStatus(anyList(), eq("pending_review")))
                .thenReturn(1);

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getEffective()).isFalse();
        verify(userMapper, never()).updateCreditScore(anyLong(), eq(5));
    }

    // ==================== 异常挂起—含历史已生效流水 ====================

    @Test
    @DisplayName("异常挂起含历史：撤销之前已生效的 credit_change 并反向修正 credit_score")
    void anomalyWithHistory_reversesPreviousCredits() {
        Long oldEvalId = 999L;
        Long oldCcId = 888L;
        Long oldTargetUserId = 3L;

        // 历史已生效的 credit_change
        CreditChange oldCc = new CreditChange();
        oldCc.setId(oldCcId);
        oldCc.setUserId(oldTargetUserId);
        oldCc.setChangeValue(3);
        oldCc.setEffective(true);
        oldCc.setSourceType("evaluation");
        oldCc.setSourceId(oldEvalId);

        AnomalyDetectionResult anomalyResult = AnomalyDetectionResult.extremeLow(
                Arrays.asList(EVALUATION_ID, oldEvalId));

        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(creditResult);
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(anomalyResult);
        when(creditChangeMapper.findBySourceTypeAndSourceIds(eq("evaluation"), anyList()))
                .thenReturn(List.of(oldCc));
        when(creditChangeMapper.batchUpdateEffective(eq(List.of(oldCcId)), eq(false)))
                .thenReturn(1);
        when(evaluationMapper.batchUpdateStatus(anyList(), eq("pending_review")))
                .thenReturn(2); // 2 evals updated

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getEvaluationStatus()).isEqualTo("pending_review");
        assertThat(result.getData().getEffective()).isFalse();

        // 验证历史 credit_change 被撤销
        verify(creditChangeMapper).batchUpdateEffective(List.of(oldCcId), false);

        // 验证历史上被评价人的 credit_score 被反向修正 (-3)
        verify(userMapper).updateCreditScore(oldTargetUserId, -3);

        // 验证当前被评价人的 credit_score 未被更新
        verify(userMapper, never()).updateCreditScore(eq(TARGET_ID), anyInt());

        // 验证当前 credit_change 为 effective=false
        verify(creditChangeMapper).insert(argThat(cc ->
                cc.getSourceId().equals(EVALUATION_ID) && cc.getEffective() == false));
    }

    @Test
    @DisplayName("异常挂起含多个历史用户：分别反向修正各自 credit_score")
    void anomalyWithHistory_multipleUsers() {
        CreditChange cc1 = new CreditChange();
        cc1.setId(1L);
        cc1.setUserId(10L);
        cc1.setChangeValue(5);

        CreditChange cc2 = new CreditChange();
        cc2.setId(2L);
        cc2.setUserId(20L);
        cc2.setChangeValue(3);

        CreditChange cc3 = new CreditChange();
        cc3.setId(3L);
        cc3.setUserId(10L); // same user as cc1
        cc3.setChangeValue(-2);

        AnomalyDetectionResult anomalyResult = AnomalyDetectionResult.extremeLow(
                Arrays.asList(EVALUATION_ID, 101L, 102L, 103L));

        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(creditResult);
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(anomalyResult);
        when(creditChangeMapper.findBySourceTypeAndSourceIds(eq("evaluation"), anyList()))
                .thenReturn(Arrays.asList(cc1, cc2, cc3));
        when(creditChangeMapper.batchUpdateEffective(eq(Arrays.asList(1L, 2L, 3L)), eq(false)))
                .thenReturn(3);
        when(evaluationMapper.batchUpdateStatus(anyList(), eq("pending_review")))
                .thenReturn(4);

        service.submit(validDto);

        // cc1 + cc3 both belong to userId=10: reverse -5 + -(-2) = -3
        verify(userMapper).updateCreditScore(10L, -3);
        // cc2 belongs to userId=20: reverse -3
        verify(userMapper).updateCreditScore(20L, -3);
    }

    // ==================== 资格校验失败（Step 1，直接返回不落库） ====================

    @Test
    @DisplayName("资格失败—SELF_EVALUATION：直接返回失败，不落库")
    void eligibilityFail_selfEvaluation() {
        validDto.setTargetId(EVALUATOR_ID); // same as evaluator

        when(eligibilityService.validateSubmission(EVALUATOR_ID, EVALUATOR_ID, PROJECT_ID))
                .thenReturn(Result.fail(ReasonCode.SELF_EVALUATION));

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SELF_EVALUATION.getCode());

        verify(evaluationMapper, never()).insert(any());
        verify(creditChangeMapper, never()).insert(any());
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("资格失败—ALREADY_EVALUATED：直接返回失败")
    void eligibilityFail_alreadyEvaluated() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.fail(ReasonCode.ALREADY_EVALUATED));

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.ALREADY_EVALUATED.getCode());
        verify(evaluationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("资格失败—NOT_PROJECT_MEMBER")
    void eligibilityFail_notProjectMember() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.fail(ReasonCode.NOT_PROJECT_MEMBER));

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.NOT_PROJECT_MEMBER.getCode());
        verify(evaluationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("资格失败—EVAL_WINDOW_CLOSED")
    void eligibilityFail_evalWindowClosed() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.fail(ReasonCode.EVAL_WINDOW_CLOSED));

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.EVAL_WINDOW_CLOSED.getCode());
        verify(evaluationMapper, never()).insert(any());
    }

    // ==================== 内容校验失败（Step 2，直接返回不落库） ====================

    @Test
    @DisplayName("内容校验失败—SCORE_FIELD_MISSING：直接返回失败")
    void validationFail_scoreMissing() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.fail(ReasonCode.SCORE_FIELD_MISSING));

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SCORE_FIELD_MISSING.getCode());
        verify(evaluationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("内容校验失败—SCORE_OUT_OF_RANGE")
    void validationFail_scoreOutOfRange() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.fail(ReasonCode.SCORE_OUT_OF_RANGE));

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SCORE_OUT_OF_RANGE.getCode());
        verify(evaluationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("内容校验失败—COMMENT_CONTAINS_VIOLATION")
    void validationFail_commentViolation() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.fail(ReasonCode.COMMENT_CONTAINS_VIOLATION));

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.COMMENT_CONTAINS_VIOLATION.getCode());
        verify(evaluationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("内容校验失败—LOW_SCORE_COMMENT_TOO_SHORT")
    void validationFail_lowScoreCommentTooShort() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.fail(ReasonCode.LOW_SCORE_COMMENT_TOO_SHORT));

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.LOW_SCORE_COMMENT_TOO_SHORT.getCode());
        verify(evaluationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("内容校验失败—HIGH_SCORE_MISSING_POSITIVE_TAG")
    void validationFail_highScoreMissingPositiveTag() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.fail(ReasonCode.HIGH_SCORE_MISSING_POSITIVE_TAG));

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.HIGH_SCORE_MISSING_POSITIVE_TAG.getCode());
        verify(evaluationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("内容校验失败—LOW_SCORE_MISSING_NEGATIVE_TAG")
    void validationFail_lowScoreMissingNegativeTag() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.fail(ReasonCode.LOW_SCORE_MISSING_NEGATIVE_TAG));

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.LOW_SCORE_MISSING_NEGATIVE_TAG.getCode());
        verify(evaluationMapper, never()).insert(any());
    }

    // ==================== 落库后失败（抛 BusinessException 回滚） ====================

    @Test
    @DisplayName("落库后失败—credit_change insert 失败抛 BusinessException")
    void postInsertFailure_creditChangeInsertFails() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(creditResult);
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(normalAnomalyResult);
        when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(0);

        assertThatThrownBy(() -> service.submit(validDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("落库后失败—userMapper.updateCreditScore 失败抛 BusinessException")
    void postInsertFailure_creditScoreUpdateFails() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(creditResult);
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(normalAnomalyResult);
        when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(1);
        when(userMapper.updateCreditScore(TARGET_ID, 3)).thenReturn(0);

        assertThatThrownBy(() -> service.submit(validDto))
                .isInstanceOf(BusinessException.class);
    }

    // ==================== boundaryDelta / zeroDelta ====================

    @Test
    @DisplayName("delta=0 时 normal 分支仍更新 credit_score（+0）")
    void deltaZero_normal_updatesWithZero() {
        CreditCalculationResult zeroResult = new CreditCalculationResult();
        zeroResult.setAverageScore(new BigDecimal("3.00"));
        zeroResult.setCappedDelta(0);

        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(zeroResult);
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(normalAnomalyResult);
        when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(1);
        when(userMapper.updateCreditScore(TARGET_ID, 0)).thenReturn(1);

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isSuccess()).isTrue();
        verify(userMapper).updateCreditScore(TARGET_ID, 0);
        verify(creditChangeMapper).insert(argThat(cc -> cc.getChangeValue() == 0));
    }

    @Test
    @DisplayName("negativeDelta=-5 时 normal 分支正确减分")
    void negativeDelta_normal_deductsCorrectly() {
        validDto.setCommunicationScore(1);
        validDto.setTaskScore(1);
        validDto.setSkillScore(1);
        validDto.setResponsibilityScore(1);
        validDto.setPositiveTags(Collections.emptyList());
        validDto.setNegativeTags(Arrays.asList("沟通差"));
        validDto.setComment("差差差差差差差差差差差差差差差差差差差差");

        CreditCalculationResult negResult = new CreditCalculationResult();
        negResult.setAverageScore(new BigDecimal("1.00"));
        negResult.setCappedDelta(-5);

        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(negResult);
        // INCOMPLETE_COVERAGE means not triggered, goes normal
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(AnomalyDetectionResult.incompleteCoverage());
        when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(1);
        when(userMapper.updateCreditScore(TARGET_ID, -5)).thenReturn(1);

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getEvaluationStatus()).isEqualTo("normal");
        verify(userMapper).updateCreditScore(TARGET_ID, -5);
    }

    // ==================== P0-1: 异常挂起不应撤销 appeal_restore 流水 ====================

    @Test
    @DisplayName("异常挂起不应撤销 appeal_restore 流水：仅撤销 change_type='evaluation' 的流水")
    void anomalyShouldNotSuspendAppealRestoreChanges() {
        Long oldEvalId = 999L;
        Long evalCcId = 888L;
        Long oldTargetUserId = 3L;

        CreditChange evalCc = new CreditChange();
        evalCc.setId(evalCcId);
        evalCc.setUserId(oldTargetUserId);
        evalCc.setChangeValue(3);
        evalCc.setEffective(true);
        evalCc.setSourceType("evaluation");
        evalCc.setSourceId(oldEvalId);

        AnomalyDetectionResult anomalyResult = AnomalyDetectionResult.extremeLow(
                Arrays.asList(EVALUATION_ID, oldEvalId));

        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(creditCalculationService.calculate(any(Evaluation.class)))
                .thenReturn(creditResult);
        when(anomalyDetectionService.detect(EVALUATOR_ID, PROJECT_ID))
                .thenReturn(anomalyResult);
        // Mapper 只返回 evaluation 类型的流水（appeal_restore 被 change_type='evaluation' 条件过滤）
        when(creditChangeMapper.findBySourceTypeAndSourceIds(eq("evaluation"), anyList()))
                .thenReturn(List.of(evalCc));
        when(creditChangeMapper.batchUpdateEffective(eq(List.of(evalCcId)), eq(false)))
                .thenReturn(1);
        when(evaluationMapper.batchUpdateStatus(anyList(), eq("pending_review")))
                .thenReturn(2);

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getEvaluationStatus()).isEqualTo("pending_review");
        assertThat(result.getData().getEffective()).isFalse();

        // 关键断言：batchUpdateEffective 仅包含 evaluation 类型的 credit_change id
        verify(creditChangeMapper).batchUpdateEffective(List.of(evalCcId), false);
        // 反向修正仅针对 evalCc 的 changeValue
        verify(userMapper).updateCreditScore(oldTargetUserId, -3);
        // 当前 credit_change 为 effective=false
        verify(creditChangeMapper).insert(argThat(cc ->
                cc.getSourceId().equals(EVALUATION_ID) && cc.getEffective() == false));
    }

    // ==================== P1: 重复提交唯一键冲突 ====================

    @Test
    @DisplayName("重复提交唯一键冲突：返回 ALREADY_EVALUATED，不写 credit_change 和 credit_score")
    void duplicateEvaluationInsertShouldReturnAlreadyEvaluated() {
        when(eligibilityService.validateSubmission(EVALUATOR_ID, TARGET_ID, PROJECT_ID))
                .thenReturn(Result.success());
        when(validationService.validateSubmission(validDto))
                .thenReturn(Result.success());
        when(evaluationMapper.insert(any(Evaluation.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'uk_project_evaluator_target'"));

        Result<EvaluationSubmitResult> result = service.submit(validDto);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.ALREADY_EVALUATED.getCode());
        verify(evaluationTagMapper, never()).insert(any());
        verify(creditChangeMapper, never()).insert(any());
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }
}
