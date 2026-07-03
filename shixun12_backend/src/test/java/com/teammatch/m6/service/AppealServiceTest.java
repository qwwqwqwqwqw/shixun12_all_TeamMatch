package com.teammatch.m6.service;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.AppealRestoreCommand;
import com.teammatch.dto.AppealRestoreResult;
import com.teammatch.entity.Appeal;
import com.teammatch.entity.Evaluation;
import com.teammatch.entity.Project;
import com.teammatch.exception.BusinessException;
import com.teammatch.mapper.AppealMapper;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.ProjectMapper;
import com.teammatch.m6.dto.AppealableEvaluationVO;
import com.teammatch.m6.dto.AppealablePenaltyVO;
import com.teammatch.m6.dto.AppealCreateDTO;
import com.teammatch.m6.dto.AppealHandleDTO;
import com.teammatch.m6.dto.PenaltyRevokeDTO;
import com.teammatch.m6.entity.Penalty;
import com.teammatch.m6.mapper.PenaltyMapper;
import com.teammatch.m6.service.impl.AppealServiceImpl;
import com.teammatch.service.AppealRestoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AppealService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("申诉服务单元测试")
class AppealServiceTest {

    @Mock
    private AppealMapper appealMapper;

    @Mock
    private EvaluationMapper evaluationMapper;

    @Mock
    private PenaltyMapper penaltyMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private PenaltyService penaltyService;

    @Mock
    private AppealRestoreService appealRestoreService;

    @Mock
    private com.teammatch.service.storage.OssService ossService;

    @InjectMocks
    private AppealServiceImpl appealService;

    private static final Long USER_ID = 100L;
    private static final Long ADMIN_ID = 999L;
    private static final Long APPEAL_ID = 1L;
    private static final Long EVALUATION_ID = 50L;
    private static final Long PENALTY_ID = 60L;

    private Appeal pendingAppeal;
    private Appeal approvedAppeal;
    private Appeal rejectedAppeal;
    private Evaluation evaluation;
    private Penalty activePenalty;

    @BeforeEach
    void setUp() {
        pendingAppeal = new Appeal();
        pendingAppeal.setId(APPEAL_ID);
        pendingAppeal.setUserId(USER_ID);
        pendingAppeal.setTargetType("evaluation");
        pendingAppeal.setTargetId(EVALUATION_ID);
        pendingAppeal.setReason("评价不公正");
        pendingAppeal.setStatus("pending");
        pendingAppeal.setCreatedAt(LocalDateTime.now().minusDays(1));
        pendingAppeal.setUpdatedAt(LocalDateTime.now().minusDays(1));

        approvedAppeal = new Appeal();
        approvedAppeal.setId(2L);
        approvedAppeal.setUserId(USER_ID);
        approvedAppeal.setTargetType("penalty");
        approvedAppeal.setTargetId(PENALTY_ID);
        approvedAppeal.setReason("处罚过重");
        approvedAppeal.setStatus("approved");
        approvedAppeal.setHandlerId(ADMIN_ID);
        approvedAppeal.setHandleResult("经审核，同意撤销");
        approvedAppeal.setHandledAt(LocalDateTime.now());
        approvedAppeal.setCreatedAt(LocalDateTime.now().minusDays(2));
        approvedAppeal.setUpdatedAt(LocalDateTime.now());

        rejectedAppeal = new Appeal();
        rejectedAppeal.setId(3L);
        rejectedAppeal.setUserId(USER_ID);
        rejectedAppeal.setTargetType("evaluation");
        rejectedAppeal.setTargetId(EVALUATION_ID);
        rejectedAppeal.setReason("评价不公正");
        rejectedAppeal.setStatus("rejected");
        rejectedAppeal.setHandlerId(ADMIN_ID);
        rejectedAppeal.setHandleResult("经审核，评价合理");
        rejectedAppeal.setHandledAt(LocalDateTime.now());
        rejectedAppeal.setCreatedAt(LocalDateTime.now().minusDays(3));
        rejectedAppeal.setUpdatedAt(LocalDateTime.now());

        evaluation = new Evaluation();
        evaluation.setId(EVALUATION_ID);
        evaluation.setTargetId(USER_ID);
        evaluation.setProjectId(1L);
        evaluation.setCommunicationScore(2);
        evaluation.setTaskScore(2);
        evaluation.setSkillScore(2);
        evaluation.setResponsibilityScore(2);
        evaluation.setStatus(Evaluation.STATUS_NORMAL);

        activePenalty = new Penalty();
        activePenalty.setId(PENALTY_ID);
        activePenalty.setUserId(USER_ID);
        activePenalty.setType("credit_deduct");
        activePenalty.setCreditDeductValue(10);
        activePenalty.setStatus("active");

        lenient().when(ossService.normalizeStoredUrls(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(ossService.resolveAccessibleUrls(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ==================== createAppeal ====================

    @Test
    @DisplayName("createAppeal: 正常创建evaluation类型申诉")
    void createAppeal_evaluation_success() {
        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);
        when(appealMapper.selectOne(any())).thenReturn(null);
        when(appealMapper.insert(any(Appeal.class))).thenAnswer(invocation -> {
            Appeal appeal = invocation.getArgument(0);
            appeal.setId(APPEAL_ID);
            return 1;
        });

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("evaluation");
        dto.setTargetId(EVALUATION_ID);
        dto.setReason("评价不公正");

        Appeal result = appealService.createAppeal(USER_ID, dto);

        assertThat(result.getId()).isEqualTo(APPEAL_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getTargetType()).isEqualTo("evaluation");
        assertThat(result.getTargetId()).isEqualTo(EVALUATION_ID);
        assertThat(result.getReason()).isEqualTo("评价不公正");
        assertThat(result.getStatus()).isEqualTo("pending");
        verify(appealMapper).insert(any(Appeal.class));
    }

    @Test
    @DisplayName("createAppeal: pending_review评价不可申诉")
    void createAppeal_evaluation_pendingReview_rejected() {
        evaluation.setStatus(Evaluation.STATUS_PENDING_REVIEW);
        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("evaluation");
        dto.setTargetId(EVALUATION_ID);
        dto.setReason("想申诉");

        assertThatThrownBy(() -> appealService.createAppeal(USER_ID, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("待复核");

        verify(appealMapper, never()).insert(any());
    }

    @Test
    @DisplayName("createAppeal: voided评价不可申诉")
    void createAppeal_evaluation_voided_rejected() {
        evaluation.setStatus(Evaluation.STATUS_VOIDED);
        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("evaluation");
        dto.setTargetId(EVALUATION_ID);
        dto.setReason("想申诉");

        assertThatThrownBy(() -> appealService.createAppeal(USER_ID, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("该评价已裁定，无法申诉");

        verify(appealMapper, never()).insert(any());
    }

    @Test
    @DisplayName("createAppeal: kept_no_credit评价不可申诉")
    void createAppeal_evaluation_keptNoCredit_rejected() {
        evaluation.setStatus(Evaluation.STATUS_KEPT_NO_CREDIT);
        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("evaluation");
        dto.setTargetId(EVALUATION_ID);
        dto.setReason("想申诉");

        assertThatThrownBy(() -> appealService.createAppeal(USER_ID, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("该评价已裁定，无法申诉");

        verify(appealMapper, never()).insert(any());
    }

    @Test
    @DisplayName("createAppeal: 正常创建penalty类型申诉")
    void createAppeal_penalty_success() {
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(activePenalty);
        when(appealMapper.selectOne(any())).thenReturn(null);
        when(appealMapper.insert(any(Appeal.class))).thenAnswer(invocation -> {
            Appeal appeal = invocation.getArgument(0);
            appeal.setId(APPEAL_ID);
            return 1;
        });

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("penalty");
        dto.setTargetId(PENALTY_ID);
        dto.setReason("处罚过重");

        Appeal result = appealService.createAppeal(USER_ID, dto);

        assertThat(result.getTargetType()).isEqualTo("penalty");
        assertThat(result.getStatus()).isEqualTo("pending");
        verify(appealMapper).insert(any(Appeal.class));
    }

    @Test
    @DisplayName("createAppeal: 无效的目标类型抛出异常")
    void createAppeal_invalidTargetType_throws() {
        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("invalid");
        dto.setTargetId(EVALUATION_ID);
        dto.setReason("申诉原因");

        assertThatThrownBy(() -> appealService.createAppeal(USER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无效的申诉目标类型");

        verify(appealMapper, never()).insert(any());
    }

    @Test
    @DisplayName("createAppeal: evaluation不存在抛出异常")
    void createAppeal_evaluationNotFound_throws() {
        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(null);

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("evaluation");
        dto.setTargetId(EVALUATION_ID);
        dto.setReason("评价不公正");

        assertThatThrownBy(() -> appealService.createAppeal(USER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("评价记录不存在");
    }

    @Test
    @DisplayName("createAppeal: 不能对他人评价申诉")
    void createAppeal_evaluationNotBelongToUser_throws() {
        Evaluation otherEvaluation = new Evaluation();
        otherEvaluation.setId(EVALUATION_ID);
        otherEvaluation.setTargetId(200L); // 不是当前用户
        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(otherEvaluation);

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("evaluation");
        dto.setTargetId(EVALUATION_ID);
        dto.setReason("评价不公正");

        assertThatThrownBy(() -> appealService.createAppeal(USER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只能对自己的评价提交申诉");
    }

    @Test
    @DisplayName("createAppeal: 已存在待处理申诉抛出异常")
    void createAppeal_duplicatePending_throws() {
        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);
        when(appealMapper.selectOne(any())).thenReturn(pendingAppeal);

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("evaluation");
        dto.setTargetId(EVALUATION_ID);
        dto.setReason("评价不公正");

        assertThatThrownBy(() -> appealService.createAppeal(USER_ID, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("该目标已存在待处理的申诉");

        verify(appealMapper, never()).insert(any());
    }

    @Test
    @DisplayName("createAppeal: 已驳回申诉不可重复提交")
    void createAppeal_duplicateRejected_throws() {
        when(evaluationMapper.selectById(EVALUATION_ID)).thenReturn(evaluation);
        when(appealMapper.selectOne(any())).thenReturn(rejectedAppeal);

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("evaluation");
        dto.setTargetId(EVALUATION_ID);
        dto.setReason("再次申诉");

        assertThatThrownBy(() -> appealService.createAppeal(USER_ID, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("该目标已提交过申诉，请勿重复提交");

        verify(appealMapper, never()).insert(any());
    }

    @Test
    @DisplayName("createAppeal: 已撤销的处罚不能申诉")
    void createAppeal_revokedPenalty_throws() {
        Penalty revokedPenalty = new Penalty();
        revokedPenalty.setId(PENALTY_ID);
        revokedPenalty.setUserId(USER_ID);
        revokedPenalty.setStatus("revoked");
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(revokedPenalty);

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("penalty");
        dto.setTargetId(PENALTY_ID);
        dto.setReason("处罚过重");

        assertThatThrownBy(() -> appealService.createAppeal(USER_ID, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("该处罚已撤销，无需申诉");
    }

    @Test
    @DisplayName("createAppeal: penalty不存在抛出异常")
    void createAppeal_penaltyNotFound_throws() {
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(null);

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("penalty");
        dto.setTargetId(PENALTY_ID);
        dto.setReason("处罚过重");

        assertThatThrownBy(() -> appealService.createAppeal(USER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("处罚记录不存在");
    }

    @Test
    @DisplayName("createAppeal: 不能对他人处罚申诉")
    void createAppeal_penaltyNotBelongToUser_throws() {
        Penalty otherPenalty = new Penalty();
        otherPenalty.setId(PENALTY_ID);
        otherPenalty.setUserId(200L); // 不是当前用户
        otherPenalty.setStatus("active");
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(otherPenalty);

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("penalty");
        dto.setTargetId(PENALTY_ID);
        dto.setReason("处罚过重");

        assertThatThrownBy(() -> appealService.createAppeal(USER_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只能对自己的处罚提交申诉");
    }

    // ==================== getMyAppeals ====================

    @Test
    @DisplayName("getMyAppeals: 返回用户的申诉列表")
    void getMyAppeals_success() {
        when(appealMapper.selectList(any()))
                .thenReturn(Arrays.asList(pendingAppeal, approvedAppeal));

        List<Appeal> result = appealService.getMyAppeals(USER_ID);

        assertThat(result).hasSize(2);
        verify(appealMapper).selectList(any());
    }

    @Test
    @DisplayName("getMyAppeals: 无申诉记录返回空列表")
    void getMyAppeals_empty() {
        when(appealMapper.selectList(any()))
                .thenReturn(Collections.emptyList());

        List<Appeal> result = appealService.getMyAppeals(USER_ID);

        assertThat(result).isEmpty();
    }

    // ==================== getAppealList ====================

    @Test
    @DisplayName("getAppealList: 查询全部申诉")
    void getAppealList_all() {
        when(appealMapper.selectList(any())).thenReturn(Arrays.asList(pendingAppeal, approvedAppeal));

        List<Appeal> result = appealService.getAppealList(null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getAppealList: 按状态筛选")
    void getAppealList_byStatus() {
        when(appealMapper.selectList(any())).thenReturn(Arrays.asList(pendingAppeal));

        List<Appeal> result = appealService.getAppealList("pending", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("pending");
    }

    @Test
    @DisplayName("getAppealList: 按目标类型筛选")
    void getAppealList_byTargetType() {
        when(appealMapper.selectList(any())).thenReturn(Arrays.asList(approvedAppeal));

        List<Appeal> result = appealService.getAppealList(null, "penalty");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTargetType()).isEqualTo("penalty");
    }

    @Test
    @DisplayName("getAppealList: 同时按状态和目标类型筛选")
    void getAppealList_byStatusAndTargetType() {
        when(appealMapper.selectList(any())).thenReturn(Arrays.asList(pendingAppeal));

        List<Appeal> result = appealService.getAppealList("pending", "evaluation");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("pending");
        assertThat(result.get(0).getTargetType()).isEqualTo("evaluation");
    }

    // ==================== handleAppeal ====================

    @Test
    @DisplayName("handleAppeal: 批准evaluation类型申诉并调用信誉恢复")
    void handleAppeal_evaluationApproved_success() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(pendingAppeal);
        when(appealRestoreService.restore(any(AppealRestoreCommand.class)))
                .thenReturn(Result.success(new AppealRestoreResult()));

        AppealHandleDTO dto = new AppealHandleDTO();
        dto.setStatus("approved");
        dto.setHandleResult("经审核，同意撤销评价");

        Appeal result = appealService.handleAppeal(APPEAL_ID, ADMIN_ID, dto);

        assertThat(result.getStatus()).isEqualTo("approved");
        assertThat(result.getHandlerId()).isEqualTo(ADMIN_ID);
        verify(appealMapper).updateById(any(Appeal.class));
        verify(appealRestoreService).restore(argThat(cmd -> APPEAL_ID.equals(cmd.getAppealId())));
        verify(penaltyService, never()).revokePenalty(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("handleAppeal: evaluation申诉信誉恢复失败应回滚")
    void handleAppeal_evaluationRestoreFail_throws() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(pendingAppeal);
        when(appealRestoreService.restore(any(AppealRestoreCommand.class)))
                .thenReturn(Result.fail(ReasonCode.CREDIT_CHANGE_NOT_FOUND));

        AppealHandleDTO dto = new AppealHandleDTO();
        dto.setStatus("approved");

        assertThatThrownBy(() -> appealService.handleAppeal(APPEAL_ID, ADMIN_ID, dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getReasonCode())
                        .isEqualTo(ReasonCode.CREDIT_CHANGE_NOT_FOUND));
        verify(penaltyService, never()).revokePenalty(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("handleAppeal: 批准penalty类型申诉并撤销处罚")
    void handleAppeal_penaltyApproved_success() {
        Appeal penaltyAppeal = new Appeal();
        penaltyAppeal.setId(APPEAL_ID);
        penaltyAppeal.setUserId(USER_ID);
        penaltyAppeal.setTargetType("penalty");
        penaltyAppeal.setTargetId(PENALTY_ID);
        penaltyAppeal.setStatus("pending");

        when(appealMapper.selectById(APPEAL_ID)).thenReturn(penaltyAppeal);
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(activePenalty);
        when(penaltyService.revokePenalty(anyLong(), anyLong(), any(PenaltyRevokeDTO.class))).thenReturn(activePenalty);

        AppealHandleDTO dto = new AppealHandleDTO();
        dto.setStatus("approved");
        dto.setHandleResult("经审核，同意撤销处罚");

        Appeal result = appealService.handleAppeal(APPEAL_ID, ADMIN_ID, dto);

        assertThat(result.getStatus()).isEqualTo("approved");
        verify(penaltyService).revokePenalty(eq(PENALTY_ID), eq(ADMIN_ID), any(PenaltyRevokeDTO.class));
    }

    @Test
    @DisplayName("handleAppeal: 驳回申诉")
    void handleAppeal_rejected_success() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(pendingAppeal);

        AppealHandleDTO dto = new AppealHandleDTO();
        dto.setStatus("rejected");
        dto.setHandleResult("经审核，评价合理");

        Appeal result = appealService.handleAppeal(APPEAL_ID, ADMIN_ID, dto);

        assertThat(result.getStatus()).isEqualTo("rejected");
        assertThat(result.getHandlerId()).isEqualTo(ADMIN_ID);
        verify(penaltyService, never()).revokePenalty(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("handleAppeal: 申诉不存在抛出异常")
    void handleAppeal_notFound_throws() {
        when(appealMapper.selectById(999L)).thenReturn(null);

        AppealHandleDTO dto = new AppealHandleDTO();
        dto.setStatus("approved");

        assertThatThrownBy(() -> appealService.handleAppeal(999L, ADMIN_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("申诉记录不存在");
    }

    @Test
    @DisplayName("handleAppeal: 已处理的申诉重复处理抛出异常")
    void handleAppeal_alreadyHandled_throws() {
        when(appealMapper.selectById(2L)).thenReturn(approvedAppeal);

        AppealHandleDTO dto = new AppealHandleDTO();
        dto.setStatus("rejected");

        assertThatThrownBy(() -> appealService.handleAppeal(2L, ADMIN_ID, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("申诉已处理，无法重复处理");
    }

    @Test
    @DisplayName("handleAppeal: 无效的处理结果抛出异常")
    void handleAppeal_invalidStatus_throws() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(pendingAppeal);

        AppealHandleDTO dto = new AppealHandleDTO();
        dto.setStatus("invalid");

        assertThatThrownBy(() -> appealService.handleAppeal(APPEAL_ID, ADMIN_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无效的处理结果");
    }

    @Test
    @DisplayName("handleAppeal: penalty类型申诉撤销处罚失败应抛出异常")
    void handleAppeal_penaltyRevokeFail_throws() {
        Appeal penaltyAppeal = new Appeal();
        penaltyAppeal.setId(APPEAL_ID);
        penaltyAppeal.setUserId(USER_ID);
        penaltyAppeal.setTargetType("penalty");
        penaltyAppeal.setTargetId(PENALTY_ID);
        penaltyAppeal.setStatus("pending");

        when(appealMapper.selectById(APPEAL_ID)).thenReturn(penaltyAppeal);
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(activePenalty);
        when(penaltyService.revokePenalty(anyLong(), anyLong(), any(PenaltyRevokeDTO.class)))
                .thenThrow(new RuntimeException("处罚撤销失败"));

        AppealHandleDTO dto = new AppealHandleDTO();
        dto.setStatus("approved");
        dto.setHandleResult("经审核，同意撤销处罚");

        assertThatThrownBy(() -> appealService.handleAppeal(APPEAL_ID, ADMIN_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("处罚撤销失败");
    }

    @Test
    @DisplayName("handleAppeal: 批准penalty类型申诉使用默认处理结果")
    void handleAppeal_penaltyApprovedDefaultResult_success() {
        Appeal penaltyAppeal = new Appeal();
        penaltyAppeal.setId(APPEAL_ID);
        penaltyAppeal.setUserId(USER_ID);
        penaltyAppeal.setTargetType("penalty");
        penaltyAppeal.setTargetId(PENALTY_ID);
        penaltyAppeal.setStatus("pending");

        when(appealMapper.selectById(APPEAL_ID)).thenReturn(penaltyAppeal);
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(activePenalty);
        when(penaltyService.revokePenalty(anyLong(), anyLong(), any(PenaltyRevokeDTO.class))).thenReturn(activePenalty);

        AppealHandleDTO dto = new AppealHandleDTO();
        dto.setStatus("approved");
        // 不设置 handleResult，测试默认消息

        Appeal result = appealService.handleAppeal(APPEAL_ID, ADMIN_ID, dto);

        assertThat(result.getStatus()).isEqualTo("approved");
        verify(penaltyService).revokePenalty(eq(PENALTY_ID), eq(ADMIN_ID), any(PenaltyRevokeDTO.class));
    }

    @Test
    @DisplayName("handleAppeal: 处罚已撤销时批准申诉应成功且跳过重复撤销")
    void handleAppeal_penaltyAlreadyRevoked_approved_success() {
        Appeal penaltyAppeal = new Appeal();
        penaltyAppeal.setId(APPEAL_ID);
        penaltyAppeal.setUserId(USER_ID);
        penaltyAppeal.setTargetType("penalty");
        penaltyAppeal.setTargetId(PENALTY_ID);
        penaltyAppeal.setStatus("pending");

        Penalty revokedPenalty = new Penalty();
        revokedPenalty.setId(PENALTY_ID);
        revokedPenalty.setStatus("revoked");

        when(appealMapper.selectById(APPEAL_ID)).thenReturn(penaltyAppeal);
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(revokedPenalty);

        AppealHandleDTO dto = new AppealHandleDTO();
        dto.setStatus("approved");
        dto.setHandleResult("申诉通过");

        Appeal result = appealService.handleAppeal(APPEAL_ID, ADMIN_ID, dto);

        assertThat(result.getStatus()).isEqualTo("approved");
        verify(penaltyService, never()).revokePenalty(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("handleAppeal: 处罚撤销冲突应透传 IllegalStateException")
    void handleAppeal_penaltyRevokeConflict_throwsIllegalState() {
        Appeal penaltyAppeal = new Appeal();
        penaltyAppeal.setId(APPEAL_ID);
        penaltyAppeal.setUserId(USER_ID);
        penaltyAppeal.setTargetType("penalty");
        penaltyAppeal.setTargetId(PENALTY_ID);
        penaltyAppeal.setStatus("pending");

        when(appealMapper.selectById(APPEAL_ID)).thenReturn(penaltyAppeal);
        when(penaltyMapper.selectById(PENALTY_ID)).thenReturn(activePenalty);
        when(penaltyService.revokePenalty(anyLong(), anyLong(), any(PenaltyRevokeDTO.class)))
                .thenThrow(new IllegalStateException("处罚已撤销，无法重复撤销"));

        AppealHandleDTO dto = new AppealHandleDTO();
        dto.setStatus("approved");
        dto.setHandleResult("申诉通过");

        assertThatThrownBy(() -> appealService.handleAppeal(APPEAL_ID, ADMIN_ID, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("处罚已撤销，无法重复撤销");
    }

    // ==================== getAppealById ====================

    @Test
    @DisplayName("getAppealById: 查询存在的申诉")
    void getAppealById_exists() {
        when(appealMapper.selectById(APPEAL_ID)).thenReturn(pendingAppeal);

        Appeal result = appealService.getAppealById(APPEAL_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(APPEAL_ID);
    }

    @Test
    @DisplayName("getAppealById: 查询不存在的申诉返回null")
    void getAppealById_notExists() {
        when(appealMapper.selectById(999L)).thenReturn(null);

        Appeal result = appealService.getAppealById(999L);

        assertThat(result).isNull();
    }

    // ==================== listAppealableEvaluations / listAppealablePenalties ====================

    @Test
    @DisplayName("listAppealableEvaluations: 返回 normal 且未申诉的评价")
    void listAppealableEvaluations_success() {
        evaluation.setAverageScore(new java.math.BigDecimal("2.50"));
        evaluation.setComment("沟通不足");
        evaluation.setCreatedAt(LocalDateTime.now().minusDays(1));

        Project project = new Project();
        project.setId(1L);
        project.setTitle("后端实训");

        when(evaluationMapper.findNormalByTargetId(USER_ID)).thenReturn(List.of(evaluation));
        when(appealMapper.selectOne(any())).thenReturn(null);
        when(projectMapper.selectById(1L)).thenReturn(project);

        List<AppealableEvaluationVO> result = appealService.listAppealableEvaluations(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEvaluationId()).isEqualTo(EVALUATION_ID);
        assertThat(result.get(0).getProjectTitle()).isEqualTo("后端实训");
    }

    @Test
    @DisplayName("listAppealableEvaluations: 已申诉过的评价不返回")
    void listAppealableEvaluations_excludesAppealed() {
        when(evaluationMapper.findNormalByTargetId(USER_ID)).thenReturn(List.of(evaluation));
        when(appealMapper.selectOne(any())).thenReturn(rejectedAppeal);

        List<AppealableEvaluationVO> result = appealService.listAppealableEvaluations(USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listAppealablePenalties: 返回 active 且未申诉的处罚")
    void listAppealablePenalties_success() {
        activePenalty.setReason("违规操作");
        activePenalty.setCreatedAt(LocalDateTime.now().minusDays(1));

        when(penaltyMapper.selectActiveByUserId(USER_ID)).thenReturn(List.of(activePenalty));
        when(appealMapper.selectOne(any())).thenReturn(null);

        List<AppealablePenaltyVO> result = appealService.listAppealablePenalties(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPenaltyId()).isEqualTo(PENALTY_ID);
        assertThat(result.get(0).getStatus()).isEqualTo("active");
    }

    @Test
    @DisplayName("listAppealablePenalties: 已申诉过的处罚不返回")
    void listAppealablePenalties_excludesAppealed() {
        when(penaltyMapper.selectActiveByUserId(USER_ID)).thenReturn(List.of(activePenalty));
        when(appealMapper.selectOne(any())).thenReturn(approvedAppeal);

        List<AppealablePenaltyVO> result = appealService.listAppealablePenalties(USER_ID);

        assertThat(result).isEmpty();
    }

    // ==================== getPendingAppeal ====================

    @Test
    @DisplayName("getPendingAppeal: 查询存在的待处理申诉")
    void getPendingAppeal_exists() {
        when(appealMapper.selectOne(any())).thenReturn(pendingAppeal);

        Appeal result = appealService.getPendingAppeal("evaluation", EVALUATION_ID);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("pending");
    }

    @Test
    @DisplayName("getPendingAppeal: 查询不存在的待处理申诉返回null")
    void getPendingAppeal_notExists() {
        when(appealMapper.selectOne(any())).thenReturn(null);

        Appeal result = appealService.getPendingAppeal("evaluation", EVALUATION_ID);

        assertThat(result).isNull();
    }
}
