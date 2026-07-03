package com.teammatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.EvaluationSubmitDTO;
import com.teammatch.dto.EvaluationSubmitResult;
import com.teammatch.dto.EvaluatableMemberDTO;
import com.teammatch.dto.SubmitEvaluationRequest;
import com.teammatch.entity.Evaluation;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.util.AuthUtil;
import com.teammatch.service.EvaluationEligibilityService;
import com.teammatch.service.EvaluationSubmitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluationController.class)
@DisplayName("EvaluationController 测试")
class EvaluationControllerTest {

    private static final Long MOCK_USER_ID = 1L;
    private static final Long MOCK_PROJECT_ID = 100L;
    private static final Long MOCK_TARGET_ID = 2L;
    private static final String VALID_TOKEN = "Bearer mock-jwt-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private EvaluationEligibilityService evaluationEligibilityService;

    @MockBean
    private EvaluationSubmitService evaluationSubmitService;

    @MockBean
    private EvaluationMapper evaluationMapper;

    @BeforeEach
    void setUp() {
        // 默认：有效 token 返回 MOCK_USER_ID
        when(authUtil.requireUserId(VALID_TOKEN)).thenReturn(MOCK_USER_ID);
        when(authUtil.requireUserId(null)).thenThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED));
        when(authUtil.requireUserId("")).thenThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED));
    }

    // ==================== 鉴权测试 ====================

    @Test
    @DisplayName("A1: B1 缺少 Authorization header → UNAUTHORIZED")
    void b1_missingAuthHeader_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/m5/projects/{projectId}/evaluation-eligibility", MOCK_PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("A1: B5 缺少 Authorization header → UNAUTHORIZED")
    void b5_missingAuthHeader_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/m5/evaluations/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("A2: B1 无效 token → UNAUTHORIZED")
    void b1_invalidToken_shouldReturnUnauthorized() throws Exception {
        when(authUtil.requireUserId("Bearer invalid-token"))
                .thenThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED));

        mockMvc.perform(get("/m5/projects/{projectId}/evaluation-eligibility", MOCK_PROJECT_ID)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("A2: B5 无效 token → UNAUTHORIZED")
    void b5_invalidToken_shouldReturnUnauthorized() throws Exception {
        when(authUtil.requireUserId("Bearer invalid-token"))
                .thenThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED));

        mockMvc.perform(get("/m5/evaluations/received")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("A3: 空白 Authorization header → UNAUTHORIZED")
    void blankAuthHeader_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/m5/projects/{projectId}/evaluation-eligibility", MOCK_PROJECT_ID)
                        .header("Authorization", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("A4: B4 Service 抛非鉴权异常 → 不映射为 UNAUTHORIZED")
    void nonAuthException_shouldNotReturnUnauthorized() throws Exception {
        // 鉴权通过，但 Service 内部抛异常 → 不应被 Controller catch 误判为 M3000
        // 异常应向上传播（不被 Controller 吞掉），证明 Controller 的 catch 只覆盖鉴权
        when(evaluationSubmitService.submit(any(EvaluationSubmitDTO.class)))
                .thenThrow(new RuntimeException("数据库连接失败"));

        SubmitEvaluationRequest request = buildSubmitRequest();

        // 执行请求，预期 Servlet 层收到异常（非 200 + M3000）
        Exception ex = null;
        try {
            mockMvc.perform(post("/m5/evaluations")
                    .header("Authorization", VALID_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        } catch (Exception e) {
            ex = e;
        }
        // 关键：异常已传播，不是被 Controller catch 返回 M3000
        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).contains("Request processing failed");
    }

    // ==================== B1: 项目级互评资格 ====================

    @Test
    @DisplayName("E1: B1 正常互评资格 → success true")
    void b1_eligible_shouldReturnTrue() throws Exception {
        when(evaluationEligibilityService.checkProjectEligibility(MOCK_USER_ID, MOCK_PROJECT_ID))
                .thenReturn(Result.success(true));

        mockMvc.perform(get("/m5/projects/{projectId}/evaluation-eligibility", MOCK_PROJECT_ID)
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("E2: B1 非项目成员 → NOT_PROJECT_MEMBER")
    void b1_notMember_shouldReturnNotProjectMember() throws Exception {
        when(evaluationEligibilityService.checkProjectEligibility(MOCK_USER_ID, MOCK_PROJECT_ID))
                .thenReturn(Result.fail(ReasonCode.NOT_PROJECT_MEMBER));

        mockMvc.perform(get("/m5/projects/{projectId}/evaluation-eligibility", MOCK_PROJECT_ID)
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_PROJECT_MEMBER.getCode()));
    }

    @Test
    @DisplayName("E3: B1 项目未结束 → PROJECT_NOT_ENDED")
    void b1_projectNotEnded_shouldReturnProjectNotEnded() throws Exception {
        when(evaluationEligibilityService.checkProjectEligibility(MOCK_USER_ID, MOCK_PROJECT_ID))
                .thenReturn(Result.fail(ReasonCode.PROJECT_NOT_ENDED));

        mockMvc.perform(get("/m5/projects/{projectId}/evaluation-eligibility", MOCK_PROJECT_ID)
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.PROJECT_NOT_ENDED.getCode()));
    }

    @Test
    @DisplayName("E4: B1 互评窗口关闭 → EVAL_WINDOW_CLOSED")
    void b1_windowClosed_shouldReturnEvalWindowClosed() throws Exception {
        when(evaluationEligibilityService.checkProjectEligibility(MOCK_USER_ID, MOCK_PROJECT_ID))
                .thenReturn(Result.fail(ReasonCode.EVAL_WINDOW_CLOSED));

        mockMvc.perform(get("/m5/projects/{projectId}/evaluation-eligibility", MOCK_PROJECT_ID)
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.EVAL_WINDOW_CLOSED.getCode()));
    }

    // ==================== B2: 目标级互评资格 ====================

    @Test
    @DisplayName("E5: B2 正常可评价 target → success true")
    void b2_eligible_shouldReturnTrue() throws Exception {
        when(evaluationEligibilityService.validateSubmission(MOCK_USER_ID, MOCK_TARGET_ID, MOCK_PROJECT_ID))
                .thenReturn(Result.success());

        mockMvc.perform(get("/m5/projects/{projectId}/members/{targetId}/evaluation-eligibility",
                        MOCK_PROJECT_ID, MOCK_TARGET_ID)
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("E6: B2 自评 → SELF_EVALUATION")
    void b2_selfEvaluation_shouldReturnSelfEvaluation() throws Exception {
        when(evaluationEligibilityService.validateSubmission(MOCK_USER_ID, MOCK_TARGET_ID, MOCK_PROJECT_ID))
                .thenReturn(Result.fail(ReasonCode.SELF_EVALUATION));

        mockMvc.perform(get("/m5/projects/{projectId}/members/{targetId}/evaluation-eligibility",
                        MOCK_PROJECT_ID, MOCK_TARGET_ID)
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SELF_EVALUATION.getCode()));
    }

    @Test
    @DisplayName("E7: B2 重复评价 → ALREADY_EVALUATED")
    void b2_alreadyEvaluated_shouldReturnAlreadyEvaluated() throws Exception {
        when(evaluationEligibilityService.validateSubmission(MOCK_USER_ID, MOCK_TARGET_ID, MOCK_PROJECT_ID))
                .thenReturn(Result.fail(ReasonCode.ALREADY_EVALUATED));

        mockMvc.perform(get("/m5/projects/{projectId}/members/{targetId}/evaluation-eligibility",
                        MOCK_PROJECT_ID, MOCK_TARGET_ID)
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ALREADY_EVALUATED.getCode()));
    }

    @Test
    @DisplayName("E8: B2 target 非项目成员 → TARGET_NOT_PROJECT_MEMBER")
    void b2_targetNotMember_shouldReturnTargetNotProjectMember() throws Exception {
        when(evaluationEligibilityService.validateSubmission(MOCK_USER_ID, MOCK_TARGET_ID, MOCK_PROJECT_ID))
                .thenReturn(Result.fail(ReasonCode.TARGET_NOT_PROJECT_MEMBER));

        mockMvc.perform(get("/m5/projects/{projectId}/members/{targetId}/evaluation-eligibility",
                        MOCK_PROJECT_ID, MOCK_TARGET_ID)
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.TARGET_NOT_PROJECT_MEMBER.getCode()));
    }

    // ==================== B3: 可评价成员列表 ====================

    @Test
    @DisplayName("E9: B3 正常返回成员列表")
    void b3_shouldReturnMemberList() throws Exception {
        List<EvaluatableMemberDTO> members = new ArrayList<>();
        EvaluatableMemberDTO m1 = new EvaluatableMemberDTO();
        m1.setUserId(2L);
        m1.setNickname("李四");
        m1.setAvatarUrl("http://example.com/avatar2.jpg");
        m1.setEvaluated(false);
        members.add(m1);

        EvaluatableMemberDTO m2 = new EvaluatableMemberDTO();
        m2.setUserId(3L);
        m2.setNickname("王五");
        m2.setAvatarUrl("http://example.com/avatar3.jpg");
        m2.setEvaluated(true);
        members.add(m2);

        when(evaluationEligibilityService.getEvaluatableMembers(MOCK_USER_ID, MOCK_PROJECT_ID))
                .thenReturn(Result.success(members));

        mockMvc.perform(get("/m5/projects/{projectId}/evaluatable-members", MOCK_PROJECT_ID)
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].userId").value(2))
                .andExpect(jsonPath("$.data[0].nickname").value("李四"))
                .andExpect(jsonPath("$.data[0].evaluated").value(false))
                .andExpect(jsonPath("$.data[1].userId").value(3))
                .andExpect(jsonPath("$.data[1].evaluated").value(true));
    }

    @Test
    @DisplayName("E10: B3 资格不通过 → 透传失败码")
    void b3_notEligible_shouldPassThroughReasonCode() throws Exception {
        when(evaluationEligibilityService.getEvaluatableMembers(MOCK_USER_ID, MOCK_PROJECT_ID))
                .thenReturn(Result.fail(ReasonCode.NOT_PROJECT_MEMBER));

        mockMvc.perform(get("/m5/projects/{projectId}/evaluatable-members", MOCK_PROJECT_ID)
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_PROJECT_MEMBER.getCode()));
    }

    // ==================== B4: 提交互评 ====================

    @Test
    @DisplayName("E11: B4 正常提交 → 返回 EvaluationSubmitResult")
    void b4_submit_shouldReturnResult() throws Exception {
        SubmitEvaluationRequest request = buildSubmitRequest();

        EvaluationSubmitResult submitResult = new EvaluationSubmitResult();
        submitResult.setEvaluationId(10L);
        submitResult.setEvaluationStatus("normal");
        submitResult.setEffective(true);

        when(evaluationSubmitService.submit(any(EvaluationSubmitDTO.class)))
                .thenReturn(Result.success(submitResult));

        mockMvc.perform(post("/m5/evaluations")
                        .header("Authorization", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.evaluationId").value(10))
                .andExpect(jsonPath("$.data.evaluationStatus").value("normal"));
    }

    @Test
    @DisplayName("E12: B4 Controller 正确注入 evaluatorId")
    void b4_shouldInjectEvaluatorIdFromToken() throws Exception {
        SubmitEvaluationRequest request = buildSubmitRequest();

        EvaluationSubmitResult submitResult = new EvaluationSubmitResult();
        submitResult.setEvaluationId(10L);
        when(evaluationSubmitService.submit(any(EvaluationSubmitDTO.class)))
                .thenReturn(Result.success(submitResult));

        mockMvc.perform(post("/m5/evaluations")
                        .header("Authorization", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 验证 Service 收到的 DTO 中 evaluatorId 被正确注入为 MOCK_USER_ID
        ArgumentCaptor<EvaluationSubmitDTO> captor = ArgumentCaptor.forClass(EvaluationSubmitDTO.class);
        verify(evaluationSubmitService).submit(captor.capture());
        assertThat(captor.getValue().getEvaluatorId()).isEqualTo(MOCK_USER_ID);
    }

    @Test
    @DisplayName("E13: B4 自评 → SELF_EVALUATION")
    void b4_selfEvaluation_shouldReturnSelfEvaluation() throws Exception {
        SubmitEvaluationRequest request = buildSubmitRequest();

        when(evaluationSubmitService.submit(any(EvaluationSubmitDTO.class)))
                .thenReturn(Result.fail(ReasonCode.SELF_EVALUATION));

        mockMvc.perform(post("/m5/evaluations")
                        .header("Authorization", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SELF_EVALUATION.getCode()));
    }

    @Test
    @DisplayName("E14: B4 重复评价 → ALREADY_EVALUATED")
    void b4_duplicate_shouldReturnAlreadyEvaluated() throws Exception {
        SubmitEvaluationRequest request = buildSubmitRequest();

        when(evaluationSubmitService.submit(any(EvaluationSubmitDTO.class)))
                .thenReturn(Result.fail(ReasonCode.ALREADY_EVALUATED));

        mockMvc.perform(post("/m5/evaluations")
                        .header("Authorization", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ALREADY_EVALUATED.getCode()));
    }

    // ==================== B5: 查看我收到的互评 ====================

    @Test
    @DisplayName("E15: B5 不传 projectId → 返回全部非 voided 评价")
    void b5_noProjectId_shouldReturnAllNonVoided() throws Exception {
        List<Evaluation> evaluations = Arrays.asList(
                buildEvaluation(1L, 100L, Evaluation.STATUS_NORMAL),
                buildEvaluation(2L, 200L, Evaluation.STATUS_PENDING_REVIEW)
        );
        when(evaluationMapper.findByTargetId(MOCK_USER_ID)).thenReturn(evaluations);

        mockMvc.perform(get("/m5/evaluations/received")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].evaluationId").value(1))
                .andExpect(jsonPath("$.data[1].evaluationId").value(2))
                // 确认不包含 evaluatorId
                .andExpect(jsonPath("$.data[0].evaluatorId").doesNotExist());
    }

    @Test
    @DisplayName("E16: B5 voided 评价被过滤")
    void b5_shouldFilterVoided() throws Exception {
        List<Evaluation> evaluations = Arrays.asList(
                buildEvaluation(1L, 100L, Evaluation.STATUS_NORMAL),
                buildEvaluation(2L, 100L, Evaluation.STATUS_VOIDED),
                buildEvaluation(3L, 100L, Evaluation.STATUS_PENDING_REVIEW)
        );
        when(evaluationMapper.findByTargetId(MOCK_USER_ID)).thenReturn(evaluations);

        mockMvc.perform(get("/m5/evaluations/received")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                // voided (id=2) 不在列表中
                .andExpect(jsonPath("$.data[0].evaluationId").value(1))
                .andExpect(jsonPath("$.data[1].evaluationId").value(3));
    }

    @Test
    @DisplayName("E17: B5 pending_review 正常展示")
    void b5_pendingReview_shouldBeVisible() throws Exception {
        List<Evaluation> evaluations = Collections.singletonList(
                buildEvaluation(1L, 100L, Evaluation.STATUS_PENDING_REVIEW)
        );
        when(evaluationMapper.findByTargetId(MOCK_USER_ID)).thenReturn(evaluations);

        mockMvc.perform(get("/m5/evaluations/received")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value(Evaluation.STATUS_PENDING_REVIEW));
    }

    @Test
    @DisplayName("B5 kept_no_credit 正常展示")
    void b5_keptNoCredit_shouldBeVisible() throws Exception {
        List<Evaluation> evaluations = Collections.singletonList(
                buildEvaluation(1L, 100L, Evaluation.STATUS_KEPT_NO_CREDIT)
        );
        when(evaluationMapper.findByTargetId(MOCK_USER_ID)).thenReturn(evaluations);

        mockMvc.perform(get("/m5/evaluations/received")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value(Evaluation.STATUS_KEPT_NO_CREDIT));
    }

    @Test
    @DisplayName("E18: B5 传入 projectId → 只返回该项目的评价")
    void b5_withProjectId_shouldFilterByProject() throws Exception {
        List<Evaluation> evaluations = Arrays.asList(
                buildEvaluation(1L, 100L, Evaluation.STATUS_NORMAL),
                buildEvaluation(2L, 200L, Evaluation.STATUS_NORMAL),
                buildEvaluation(3L, 100L, Evaluation.STATUS_PENDING_REVIEW)
        );
        when(evaluationMapper.findByTargetId(MOCK_USER_ID)).thenReturn(evaluations);

        mockMvc.perform(get("/m5/evaluations/received")
                        .param("projectId", "100")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].projectId").value(100))
                .andExpect(jsonPath("$.data[1].projectId").value(100));
    }

    @Test
    @DisplayName("E19: B5 传入 projectId 但无匹配 → 空数组")
    void b5_withProjectIdNoMatch_shouldReturnEmptyArray() throws Exception {
        List<Evaluation> evaluations = Collections.singletonList(
                buildEvaluation(1L, 100L, Evaluation.STATUS_NORMAL)
        );
        when(evaluationMapper.findByTargetId(MOCK_USER_ID)).thenReturn(evaluations);

        mockMvc.perform(get("/m5/evaluations/received")
                        .param("projectId", "999")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("E20: B5 不传 projectId → 返回不同 projectId 的评价")
    void b5_noProjectId_shouldReturnMultipleProjects() throws Exception {
        List<Evaluation> evaluations = Arrays.asList(
                buildEvaluation(1L, 100L, Evaluation.STATUS_NORMAL),
                buildEvaluation(2L, 200L, Evaluation.STATUS_NORMAL),
                buildEvaluation(3L, 300L, Evaluation.STATUS_NORMAL)
        );
        when(evaluationMapper.findByTargetId(MOCK_USER_ID)).thenReturn(evaluations);

        mockMvc.perform(get("/m5/evaluations/received")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].projectId").value(100))
                .andExpect(jsonPath("$.data[1].projectId").value(200))
                .andExpect(jsonPath("$.data[2].projectId").value(300));
    }

    // ==================== 辅助方法 ====================

    private SubmitEvaluationRequest buildSubmitRequest() {
        SubmitEvaluationRequest request = new SubmitEvaluationRequest();
        request.setProjectId(MOCK_PROJECT_ID);
        request.setTargetId(MOCK_TARGET_ID);
        request.setCommunicationScore(4);
        request.setTaskScore(5);
        request.setSkillScore(4);
        request.setResponsibilityScore(5);
        request.setComment("沟通积极，按时交付");
        request.setPositiveTags(Arrays.asList("沟通积极", "按时交付"));
        request.setNegativeTags(Collections.emptyList());
        return request;
    }

    private Evaluation buildEvaluation(Long id, Long projectId, String status) {
        Evaluation e = new Evaluation();
        e.setId(id);
        e.setProjectId(projectId);
        e.setEvaluatorId(999L);  // 不应出现在 VO 中
        e.setTargetId(MOCK_USER_ID);
        e.setCommunicationScore(4);
        e.setTaskScore(4);
        e.setSkillScore(4);
        e.setResponsibilityScore(4);
        e.setAverageScore(new BigDecimal("4.00"));
        e.setComment("测试评价");
        e.setStatus(status);
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }
}
