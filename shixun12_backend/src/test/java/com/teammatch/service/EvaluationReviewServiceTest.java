package com.teammatch.service;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.EvaluationReviewCommand;
import com.teammatch.dto.EvaluationReviewResult;
import com.teammatch.entity.CreditChange;
import com.teammatch.entity.Evaluation;
import com.teammatch.exception.BusinessException;
import com.teammatch.mapper.CreditChangeMapper;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.impl.EvaluationReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EvaluationReviewService 测试
 * 覆盖 M5-6 approve / void / keep_no_credit 复核核心场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("评价复核服务测试")
class EvaluationReviewServiceTest {

    private static final Long EVALUATION_ID = 1000L;
    private static final Long REVIEWER_ID = 99L;
    private static final Long TARGET_ID = 2L;
    private static final Long CC_ID = 500L;

    @Mock
    private EvaluationMapper evaluationMapper;

    @Mock
    private CreditChangeMapper creditChangeMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private EvaluationReviewServiceImpl service;

    private Evaluation pendingEvaluation;
    private CreditChange suspendedCc;

    @BeforeEach
    void setUp() {
        pendingEvaluation = new Evaluation();
        pendingEvaluation.setId(EVALUATION_ID);
        pendingEvaluation.setProjectId(100L);
        pendingEvaluation.setEvaluatorId(1L);
        pendingEvaluation.setTargetId(TARGET_ID);
        pendingEvaluation.setCommunicationScore(1);
        pendingEvaluation.setTaskScore(1);
        pendingEvaluation.setSkillScore(1);
        pendingEvaluation.setResponsibilityScore(1);
        pendingEvaluation.setAverageScore(new BigDecimal("1.00"));
        pendingEvaluation.setComment("差差差差差差差差差差差差差差差差差差差差");
        pendingEvaluation.setStatus("pending_review");

        suspendedCc = new CreditChange();
        suspendedCc.setId(CC_ID);
        suspendedCc.setUserId(TARGET_ID);
        suspendedCc.setProjectId(100L);
        suspendedCc.setChangeType("evaluation");
        suspendedCc.setChangeValue(-5);
        suspendedCc.setEffective(false);
        suspendedCc.setSourceType("evaluation");
        suspendedCc.setSourceId(EVALUATION_ID);
        suspendedCc.setDescription("项目互评信誉变化");

        // 默认 Mapper 写操作成功
        lenient().when(evaluationMapper.updateReviewIfPending(anyLong(), anyString(),
                anyLong(), any(), any())).thenReturn(1);
        lenient().when(creditChangeMapper.updateEffectiveToTrue(anyLong(), anyString(), anyLong(), anyString(), anyLong()))
                .thenReturn(1);
        lenient().when(creditChangeMapper.updateDescription(anyLong(), anyString()))
                .thenReturn(1);
        lenient().when(userMapper.updateCreditScore(anyLong(), anyInt())).thenReturn(1);
    }

    // ==================== approve ====================

    @Test
    @DisplayName("approve pending_review → normal，effective 0→1，credit_score 更新")
    void approve_pendingReview_succeeds() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", "经复核，评价合理");

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isSuccess()).isTrue();
        EvaluationReviewResult data = result.getData();
        assertThat(data.getEvaluationId()).isEqualTo(EVALUATION_ID);
        assertThat(data.getOldStatus()).isEqualTo("pending_review");
        assertThat(data.getNewStatus()).isEqualTo("normal");
        assertThat(data.getTargetId()).isEqualTo(TARGET_ID);
        assertThat(data.getCreditDelta()).isEqualTo(-5);
        assertThat(data.getCreditEffectiveChanged()).isTrue();

        // 验证 evaluation 状态更新
        verify(evaluationMapper).updateReviewIfPending(eq(EVALUATION_ID), eq("normal"),
                eq(REVIEWER_ID), eq("经复核，评价合理"), any(LocalDateTime.class));
        // 验证 credit_change.effective 0→1
        verify(creditChangeMapper).updateEffectiveToTrue(CC_ID, "evaluation", EVALUATION_ID, "evaluation", TARGET_ID);
        // 验证 credit_score 更新
        verify(userMapper).updateCreditScore(TARGET_ID, -5);
    }

    @Test
    @DisplayName("approve：reviewNote 可为 null")
    void approve_nullReviewNote() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isSuccess()).isTrue();
        verify(evaluationMapper).updateReviewIfPending(eq(EVALUATION_ID), eq("normal"),
                eq(REVIEWER_ID), eq(null), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("approve：reviewer_id 和 reviewed_at 正确保存")
    void approve_fieldsSavedCorrectly() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, 77L, "approve", "复核通过");

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isSuccess()).isTrue();
        verify(evaluationMapper).updateReviewIfPending(eq(EVALUATION_ID), anyString(),
                eq(77L), eq("复核通过"), any(LocalDateTime.class));
    }

    // ==================== void ====================

    @Test
    @DisplayName("void pending_review → voided，effective 保持 0，credit_score 不变")
    void void_pendingReview_succeeds() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "void", "作废处理");

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isSuccess()).isTrue();
        EvaluationReviewResult data = result.getData();
        assertThat(data.getEvaluationId()).isEqualTo(EVALUATION_ID);
        assertThat(data.getOldStatus()).isEqualTo("pending_review");
        assertThat(data.getNewStatus()).isEqualTo("voided");
        assertThat(data.getCreditEffectiveChanged()).isFalse();

        // 验证 evaluation 状态更新
        verify(evaluationMapper).updateReviewIfPending(eq(EVALUATION_ID), eq("voided"),
                eq(REVIEWER_ID), eq("作废处理"), any(LocalDateTime.class));
        // 验证不更新 credit_change.effective
        verify(creditChangeMapper, never()).updateEffectiveToTrue(anyLong(), anyString(), anyLong(), anyString(), anyLong());
        // 验证不更新 credit_score
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("void：reviewNote 可为 null")
    void void_nullReviewNote() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "void", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getNewStatus()).isEqualTo("voided");
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    // ==================== keep_no_credit ====================

    @Test
    @DisplayName("keep_no_credit pending_review → kept_no_credit，effective 保持 0，credit_score 不变")
    void keepNoCredit_pendingReview_succeeds() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "keep_no_credit", "保留但不计分");

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isSuccess()).isTrue();
        EvaluationReviewResult data = result.getData();
        assertThat(data.getNewStatus()).isEqualTo("kept_no_credit");
        assertThat(data.getCreditEffectiveChanged()).isFalse();

        verify(evaluationMapper).updateReviewIfPending(eq(EVALUATION_ID), eq("kept_no_credit"),
                eq(REVIEWER_ID), eq("保留但不计分"), any(LocalDateTime.class));
        verify(creditChangeMapper, never()).updateEffectiveToTrue(anyLong(), anyString(), anyLong(), anyString(), anyLong());
        verify(creditChangeMapper).updateDescription(eq(CC_ID),
                contains("保留但不计分"));
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("keep_no_credit：reviewNote 可为 null")
    void keepNoCredit_nullReviewNote() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "keep_no_credit", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getNewStatus()).isEqualTo("kept_no_credit");
        verify(creditChangeMapper).updateDescription(eq(CC_ID),
                contains("保留但不计分"));
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    // ==================== 错误场景 ====================

    @Test
    @DisplayName("evaluation 不存在 → EVALUATION_NOT_FOUND")
    void evaluationNotFound() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                9999L, REVIEWER_ID, "approve", null);

        when(evaluationMapper.selectById(9999L)).thenReturn(null);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.EVALUATION_NOT_FOUND.getCode());
        verify(creditChangeMapper, never()).findSuspendedOne(anyString(),
                anyLong(), anyString(), anyLong());
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("evaluation.status=normal 不是 pending_review → STATUS_CONFLICT")
    void statusNotPendingReview_normal() {
        pendingEvaluation.setStatus("normal");
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.STATUS_CONFLICT.getCode());
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("evaluation.status=voided 不是 pending_review → STATUS_CONFLICT")
    void statusNotPendingReview_voided() {
        pendingEvaluation.setStatus("voided");
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.STATUS_CONFLICT.getCode());
    }

    @Test
    @DisplayName("evaluation.status=kept_no_credit 不是 pending_review → STATUS_CONFLICT")
    void statusNotPendingReview_keptNoCredit() {
        pendingEvaluation.setStatus("kept_no_credit");
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.STATUS_CONFLICT.getCode());
    }

    @Test
    @DisplayName("credit_change 不存在 → CREDIT_CHANGE_NOT_FOUND")
    void creditChangeNotFound() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(null);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.CREDIT_CHANGE_NOT_FOUND.getCode());
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("credit_change.effective 已是 true（数据不一致） → CREDIT_CHANGE_NOT_FOUND")
    void creditChangeAlreadyEffective() {
        suspendedCc.setEffective(true);
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.CREDIT_CHANGE_NOT_FOUND.getCode());
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("void 时 credit_change 不存在 → CREDIT_CHANGE_NOT_FOUND，不允许静默成功")
    void void_creditChangeNotFound_fails() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "void", "作废");

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(null);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.CREDIT_CHANGE_NOT_FOUND.getCode());
        verify(evaluationMapper, never()).updateReviewIfPending(anyLong(), anyString(),
                anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("keep_no_credit 时 credit_change 不存在 → CREDIT_CHANGE_NOT_FOUND，不允许静默成功")
    void keepNoCredit_creditChangeNotFound_fails() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "keep_no_credit", "保留");

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(null);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.CREDIT_CHANGE_NOT_FOUND.getCode());
        verify(evaluationMapper, never()).updateReviewIfPending(anyLong(), anyString(),
                anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("重复 approve（已 normal 再 approve）→ STATUS_CONFLICT，不重复加分")
    void duplicateApprove_statusConflict() {
        pendingEvaluation.setStatus("normal");
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", "重复审批");

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.STATUS_CONFLICT.getCode());
        verify(creditChangeMapper, never()).updateEffectiveToTrue(anyLong(), anyString(), anyLong(), anyString(), anyLong());
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("reviewNote > 500 字符 → REVIEW_NOTE_TOO_LONG")
    void reviewNoteTooLong() {
        String longNote = "a".repeat(501);
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", longNote);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.REVIEW_NOTE_TOO_LONG.getCode());
        verify(evaluationMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("reviewNote == 500 字符 → 允许")
    void reviewNote_maxLength() {
        String maxNote = "a".repeat(500);
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", maxNote);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("action 无效值 → INVALID_REVIEW_ACTION")
    void invalidAction() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "delete", null);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.INVALID_REVIEW_ACTION.getCode());
    }

    @Test
    @DisplayName("action 为 null → INVALID_REVIEW_ACTION")
    void actionNull() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, null, null);

        Result<EvaluationReviewResult> result = service.review(command);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.INVALID_REVIEW_ACTION.getCode());
    }

    // ==================== 落库失败回滚 ====================

    @Test
    @DisplayName("approve 时 evaluationMapper.updateReview 返回 0 → BusinessException")
    void approve_updateReviewFails_throwsException() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);
        when(evaluationMapper.updateReviewIfPending(anyLong(), anyString(), anyLong(),
                any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.review(command))
                .isInstanceOf(BusinessException.class);

        verify(creditChangeMapper, never()).updateEffectiveToTrue(anyLong(), anyString(), anyLong(), anyString(), anyLong());
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("approve 时 batchUpdateEffective 返回 0 → BusinessException")
    void approve_batchUpdateEffectiveFails_throwsException() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);
        when(creditChangeMapper.updateEffectiveToTrue(eq(CC_ID), eq("evaluation"),
                eq(EVALUATION_ID), eq("evaluation"), eq(TARGET_ID))).thenReturn(0);

        assertThatThrownBy(() -> service.review(command))
                .isInstanceOf(BusinessException.class);

        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("approve 时 updateCreditScore 返回 0 → BusinessException")
    void approve_updateCreditScoreFails_throwsException() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);
        when(userMapper.updateCreditScore(TARGET_ID, -5)).thenReturn(0);

        assertThatThrownBy(() -> service.review(command))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("void 时 evaluationMapper.updateReview 返回 0 → BusinessException")
    void void_updateReviewFails_throwsException() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "void", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);
        when(evaluationMapper.updateReviewIfPending(anyLong(), anyString(), anyLong(),
                any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.review(command))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("keep_no_credit 时 evaluationMapper.updateReview 返回 0 → BusinessException")
    void keepNoCredit_updateReviewFails_throwsException() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "keep_no_credit", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);
        when(evaluationMapper.updateReviewIfPending(anyLong(), anyString(), anyLong(),
                any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.review(command))
                .isInstanceOf(BusinessException.class);
        verify(creditChangeMapper, never()).updateDescription(anyLong(), anyString());
    }

    @Test
    @DisplayName("keep_no_credit 时 updateDescription 返回 0 → BusinessException")
    void keepNoCredit_updateDescriptionFails_throwsException() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "keep_no_credit", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);
        when(creditChangeMapper.updateDescription(eq(CC_ID), anyString()))
                .thenReturn(0);

        assertThatThrownBy(() -> service.review(command))
                .isInstanceOf(BusinessException.class);
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    // ==================== credit_change 查询限定完整条件 ====================

    @Test
    @DisplayName("credit_change 查询限定 source_type + source_id + change_type + user_id")
    void creditChangeQuery_fullConditions() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", null);

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);

        service.review(command);

        verify(creditChangeMapper).findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID);
    }

    // ==================== 并发重复 approve 防护 ====================

    @Test
    @DisplayName("重复approve：第二次条件更新返回0→STATUS_CONFLICT，credit_score仅更新一次")
    void approveDuplicateShouldUpdateCreditScoreOnlyOnce() {
        EvaluationReviewCommand command = new EvaluationReviewCommand(
                EVALUATION_ID, REVIEWER_ID, "approve", "复核通过");

        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(pendingEvaluation);
        when(creditChangeMapper.findSuspendedOne("evaluation", EVALUATION_ID, "evaluation", TARGET_ID))
                .thenReturn(suspendedCc);
        when(evaluationMapper.updateReviewIfPending(eq(EVALUATION_ID), eq("normal"),
                eq(REVIEWER_ID), eq("复核通过"), any(LocalDateTime.class)))
                .thenReturn(1).thenReturn(0);

        // 第一次 approve 成功
        Result<EvaluationReviewResult> result1 = service.review(command);
        assertThat(result1.isSuccess()).isTrue();
        verify(userMapper).updateCreditScore(TARGET_ID, -5);

        // 第二次 approve：条件更新返回0，抛 STATUS_CONFLICT
        assertThatThrownBy(() -> service.review(command))
                .isInstanceOf(BusinessException.class);

        // credit_score 仅更新一次
        verify(userMapper, times(1)).updateCreditScore(anyLong(), anyInt());
    }
}
