package com.teammatch.service;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.AppealRestoreCommand;
import com.teammatch.dto.AppealRestoreResult;
import com.teammatch.entity.Appeal;
import com.teammatch.entity.CreditChange;
import com.teammatch.entity.Evaluation;
import com.teammatch.exception.BusinessException;
import com.teammatch.mapper.AppealMapper;
import com.teammatch.mapper.CreditChangeMapper;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.impl.AppealRestoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M5-7 申诉恢复服务测试
 * 覆盖正常恢复 / 跳过 / 幂等 / 异常分支 / 事务回滚场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("申诉恢复服务测试")
class AppealRestoreServiceTest {

    private static final Long APPEAL_ID = 1001L;
    private static final Long EVALUATION_ID = 2001L;
    private static final Long USER_ID = 10L;
    private static final Long CC_ID = 5001L;
    private static final Long PROJECT_ID = 3001L;

    @Mock
    private AppealMapper appealMapper;

    @Mock
    private EvaluationMapper evaluationMapper;

    @Mock
    private CreditChangeMapper creditChangeMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AppealRestoreServiceImpl appealRestoreService;

    private Appeal approvedAppeal;
    private Evaluation evaluation;
    private CreditChange originalCreditChange;

    @BeforeEach
    void setUp() {
        approvedAppeal = new Appeal();
        approvedAppeal.setId(APPEAL_ID);
        approvedAppeal.setUserId(USER_ID);
        approvedAppeal.setTargetType("evaluation");
        approvedAppeal.setTargetId(EVALUATION_ID);
        approvedAppeal.setStatus("approved");

        evaluation = new Evaluation();
        evaluation.setId(EVALUATION_ID);
        evaluation.setTargetId(USER_ID);
        evaluation.setProjectId(PROJECT_ID);

        originalCreditChange = new CreditChange();
        originalCreditChange.setId(CC_ID);
        originalCreditChange.setUserId(USER_ID);
        originalCreditChange.setProjectId(PROJECT_ID);
        originalCreditChange.setChangeType("evaluation");
        originalCreditChange.setChangeValue(-5);
        originalCreditChange.setEffective(true);
        originalCreditChange.setSourceType("evaluation");
        originalCreditChange.setSourceId(EVALUATION_ID);

        // 基础桩：默认返回成功以简化 success 场景 setup
        lenient().when(userMapper.updateCreditScore(anyLong(), anyInt())).thenReturn(1);
        lenient().when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(1);
    }

    // ==================== 成功路径 ====================

    @Test
    @DisplayName("正常恢复：原负分 -5 → appeal_restore +5")
    void restore_negativeOriginal_deltaPositive5() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);
        when(creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID)).thenReturn(originalCreditChange);
        when(creditChangeMapper.findAppealRestoreExists(
                "evaluation", EVALUATION_ID, "appeal_restore", USER_ID)).thenReturn(null);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getOriginalChangeValue()).isEqualTo(-5);
        assertThat(result.getData().getRestoreValue()).isEqualTo(5);
        assertThat(result.getData().getSkipped()).isNotEqualTo(Boolean.TRUE);
        assertThat(result.getData().getAlreadyRestored()).isNotEqualTo(Boolean.TRUE);
        verify(userMapper).updateCreditScore(USER_ID, 5);
    }

    @Test
    @DisplayName("正常恢复：原正分 +3 → appeal_restore -3")
    void restore_positiveOriginal_deltaNegative3() {
        originalCreditChange.setChangeValue(3);

        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);
        when(creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID)).thenReturn(originalCreditChange);
        when(creditChangeMapper.findAppealRestoreExists(
                "evaluation", EVALUATION_ID, "appeal_restore", USER_ID)).thenReturn(null);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getRestoreValue()).isEqualTo(-3);
        verify(userMapper).updateCreditScore(USER_ID, -3);
    }

    @Test
    @DisplayName("正常恢复：原 0 分 → appeal_restore 0，不改变 credit_score")
    void restore_zeroOriginal_noCreditChange() {
        originalCreditChange.setChangeValue(0);

        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);
        when(creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID)).thenReturn(originalCreditChange);
        when(creditChangeMapper.findAppealRestoreExists(
                "evaluation", EVALUATION_ID, "appeal_restore", USER_ID)).thenReturn(null);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getRestoreValue()).isEqualTo(0);
        verify(userMapper).updateCreditScore(USER_ID, 0);
    }

    @Test
    @DisplayName("返回结果字段完整性校验")
    void restore_resultFieldsPopulated() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);
        when(creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID)).thenReturn(originalCreditChange);
        when(creditChangeMapper.findAppealRestoreExists(
                "evaluation", EVALUATION_ID, "appeal_restore", USER_ID)).thenReturn(null);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isSuccess()).isTrue();
        AppealRestoreResult data = result.getData();
        assertThat(data.getAppealId()).isEqualTo(APPEAL_ID);
        assertThat(data.getEvaluationId()).isEqualTo(EVALUATION_ID);
        assertThat(data.getTargetUserId()).isEqualTo(USER_ID);
        assertThat(data.getOriginalChangeValue()).isEqualTo(-5);
        assertThat(data.getRestoreValue()).isEqualTo(5);
        assertThat(data.getSkipped()).isNotEqualTo(Boolean.TRUE);
        assertThat(data.getAlreadyRestored()).isNotEqualTo(Boolean.TRUE);
    }

    // ==================== 幂等 ====================

    @Test
    @DisplayName("appeal_restore 已存在 → alreadyRestored=true，success")
    void restore_alreadyExecuted_returnsAlreadyRestored() {
        CreditChange existingRestore = new CreditChange();
        existingRestore.setId(9999L);
        existingRestore.setChangeValue(5);

        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);
        when(creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID)).thenReturn(originalCreditChange);
        when(creditChangeMapper.findAppealRestoreExists(
                "evaluation", EVALUATION_ID, "appeal_restore", USER_ID)).thenReturn(existingRestore);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getAlreadyRestored()).isTrue();
        assertThat(result.getData().getRestoreValue()).isEqualTo(5);
        verify(creditChangeMapper, never()).insert(any(CreditChange.class));
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    // ==================== 跳过恢复 ====================

    @Test
    @DisplayName("原流水 effective=0 → skipped=true，success，不恢复")
    void restore_originalEffectiveZero_skipped() {
        originalCreditChange.setEffective(false);

        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);
        when(creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID)).thenReturn(originalCreditChange);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getSkipped()).isTrue();
        assertThat(result.getData().getSkipReason()).isEqualTo("原评价流水未生效");
        verify(creditChangeMapper, never()).insert(any(CreditChange.class));
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    // ==================== 异常分支 ====================

    @Test
    @DisplayName("appeal 不存在 → M5009")
    void restore_appealNotFound_fail() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(null);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.APPEAL_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("appeal.status = pending → M5010")
    void restore_appealNotApproved_pending() {
        approvedAppeal.setStatus("pending");
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.APPEAL_NOT_APPROVED.getCode());
    }

    @Test
    @DisplayName("appeal.status = rejected → M5010")
    void restore_appealNotApproved_rejected() {
        approvedAppeal.setStatus("rejected");
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.APPEAL_NOT_APPROVED.getCode());
    }

    @Test
    @DisplayName("appeal.target_type = penalty → M5011")
    void restore_invalidTargetType_penalty() {
        approvedAppeal.setTargetType("penalty");
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.INVALID_APPEAL_TARGET_TYPE.getCode());
    }

    @Test
    @DisplayName("userId 不一致 → PARAM_ERROR")
    void restore_userIdMismatch_paramError() {
        evaluation.setTargetId(99L);
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("evaluation 不存在 → M5005")
    void restore_evaluationNotFound_fail() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(null);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.EVALUATION_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("evaluation.status = voided → EVALUATION_ALREADY_INVALIDATED")
    void restore_evaluationVoided_fail() {
        evaluation.setStatus(Evaluation.STATUS_VOIDED);
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.EVALUATION_ALREADY_INVALIDATED.getCode());
    }

    @Test
    @DisplayName("原 credit_change 不存在 → M5006")
    void restore_originalCreditChangeNotFound_fail() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);
        when(creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID)).thenReturn(null);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.CREDIT_CHANGE_NOT_FOUND.getCode());
    }

    // ==================== 事务回滚 ====================

    @Test
    @DisplayName("insert 成功后 updateCreditScore 返回 0 → BusinessException 回滚")
    void restore_updateCreditScoreFails_businessException() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);
        when(creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID)).thenReturn(originalCreditChange);
        when(creditChangeMapper.findAppealRestoreExists(
                "evaluation", EVALUATION_ID, "appeal_restore", USER_ID)).thenReturn(null);
        when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(1);
        when(userMapper.updateCreditScore(USER_ID, 5)).thenReturn(0);

        assertThatThrownBy(() -> appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("更新用户信誉分失败");
    }

    @Test
    @DisplayName("insert credit_change 失败 → BusinessException 回滚")
    void restore_insertFails_businessException() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);
        when(creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID)).thenReturn(originalCreditChange);
        when(creditChangeMapper.findAppealRestoreExists(
                "evaluation", EVALUATION_ID, "appeal_restore", USER_ID)).thenReturn(null);
        when(creditChangeMapper.insert(any(CreditChange.class))).thenReturn(0);

        assertThatThrownBy(() -> appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("插入appeal_restore流水失败");
        verify(userMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    // ==================== 边界 ====================

    @Test
    @DisplayName("Mapper 方法调用参数验证")
    void restore_verifyMapperCalls() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);
        when(creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID)).thenReturn(originalCreditChange);
        when(creditChangeMapper.findAppealRestoreExists(
                "evaluation", EVALUATION_ID, "appeal_restore", USER_ID)).thenReturn(null);

        appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        verify(appealMapper).selectById(APPEAL_ID);
        verify(evaluationMapper).selectByIdForUpdate(EVALUATION_ID);
        verify(creditChangeMapper).findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID);
        verify(creditChangeMapper).findAppealRestoreExists(
                "evaluation", EVALUATION_ID, "appeal_restore", USER_ID);
        verify(creditChangeMapper).insert(any(CreditChange.class));
        verify(userMapper).updateCreditScore(USER_ID, 5);
    }

    @Test
    @DisplayName("kept_no_credit 状态：effective=0 时跳过恢复")
    void restore_keptNoCredit_effectiveZero_skipped() {
        originalCreditChange.setEffective(false);
        evaluation.setStatus(Evaluation.STATUS_KEPT_NO_CREDIT);

        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);
        when(creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID)).thenReturn(originalCreditChange);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getSkipped()).isTrue();
        verify(creditChangeMapper, never()).insert(any(CreditChange.class));
    }

    @Test
    @DisplayName("pending_review 状态：effective=0 时跳过恢复")
    void restore_pendingReview_effectiveZero_skipped() {
        originalCreditChange.setEffective(false);
        evaluation.setStatus(Evaluation.STATUS_PENDING_REVIEW);

        when(appealMapper.selectById(APPEAL_ID)).thenReturn(approvedAppeal);
        when(evaluationMapper.selectByIdForUpdate(EVALUATION_ID)).thenReturn(evaluation);
        when(creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                "evaluation", EVALUATION_ID, "evaluation", USER_ID)).thenReturn(originalCreditChange);

        Result<AppealRestoreResult> result = appealRestoreService.restore(new AppealRestoreCommand(APPEAL_ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getSkipped()).isTrue();
        verify(creditChangeMapper, never()).insert(any(CreditChange.class));
    }
}
