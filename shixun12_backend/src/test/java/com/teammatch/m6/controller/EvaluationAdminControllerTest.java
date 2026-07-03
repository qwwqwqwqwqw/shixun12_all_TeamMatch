package com.teammatch.m6.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.config.AuthExceptionHandler;
import com.teammatch.dto.EvaluationReviewCommand;
import com.teammatch.dto.EvaluationReviewResult;
import com.teammatch.entity.Evaluation;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.exception.AuthorizationException;
import com.teammatch.m6.dto.EvaluationReviewRequest;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.service.EvaluationReviewService;
import com.teammatch.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluationAdminController.class)
@Import(AuthExceptionHandler.class)
@DisplayName("评价复核控制器测试（管理端）")
class EvaluationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private EvaluationReviewService evaluationReviewService;

    @MockBean
    private EvaluationMapper evaluationMapper;

    private static final String ADMIN_TOKEN = "Bearer admin-token";
    private static final Long ADMIN_ID = 999L;
    private static final Long EVALUATION_ID = 1L;
    private static final Long PROJECT_ID = 100L;

    private Evaluation pendingEval1;
    private Evaluation pendingEval2;

    @BeforeEach
    void setUp() {
        pendingEval1 = new Evaluation();
        pendingEval1.setId(1L);
        pendingEval1.setProjectId(PROJECT_ID);
        pendingEval1.setEvaluatorId(10L);
        pendingEval1.setTargetId(20L);
        pendingEval1.setCommunicationScore(2);
        pendingEval1.setTaskScore(2);
        pendingEval1.setSkillScore(1);
        pendingEval1.setResponsibilityScore(2);
        pendingEval1.setAverageScore(new BigDecimal("1.75"));
        pendingEval1.setComment("配合度低，经常失联");
        pendingEval1.setStatus(Evaluation.STATUS_PENDING_REVIEW);
        pendingEval1.setCreatedAt(LocalDateTime.now().minusDays(1));

        pendingEval2 = new Evaluation();
        pendingEval2.setId(2L);
        pendingEval2.setProjectId(PROJECT_ID);
        pendingEval2.setEvaluatorId(10L);
        pendingEval2.setTargetId(30L);
        pendingEval2.setCommunicationScore(1);
        pendingEval2.setTaskScore(2);
        pendingEval2.setSkillScore(2);
        pendingEval2.setResponsibilityScore(1);
        pendingEval2.setAverageScore(new BigDecimal("1.50"));
        pendingEval2.setComment("延期严重，沟通态度差");
        pendingEval2.setStatus(Evaluation.STATUS_PENDING_REVIEW);
        pendingEval2.setCreatedAt(LocalDateTime.now().minusDays(2));

        lenient().when(authUtil.requireUserId(ADMIN_TOKEN)).thenReturn(ADMIN_ID);
    }

    // ==================== 鉴权 ====================

    @Test
    @DisplayName("B1: 非管理员访问 pending 列表返回 ADMIN_REQUIRED")
    void getPendingEvaluations_nonAdmin_returnsAdminRequired() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        mockMvc.perform(get("/admin/evaluations/pending")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(evaluationMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("B1: 无效 token 访问 pending 列表返回 M3000")
    void getPendingEvaluations_invalidToken_returnsUnauthorized() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireAdmin("Bearer invalid");

        mockMvc.perform(get("/admin/evaluations/pending")
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(evaluationMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("B2: 非管理员执行复核返回 ADMIN_REQUIRED")
    void reviewEvaluation_nonAdmin_returnsAdminRequired() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        EvaluationReviewRequest request = new EvaluationReviewRequest("approve", "复核通过");

        mockMvc.perform(post("/admin/evaluations/1/review")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(evaluationReviewService, never()).review(any());
    }

    @Test
    @DisplayName("B2: 无效 token 执行复核返回 UNAUTHORIZED")
    void reviewEvaluation_invalidToken_returnsUnauthorized() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireAdmin("Bearer invalid");

        EvaluationReviewRequest request = new EvaluationReviewRequest("approve", "复核通过");

        mockMvc.perform(post("/admin/evaluations/1/review")
                        .header("Authorization", "Bearer invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(evaluationReviewService, never()).review(any());
    }

    // ==================== B1: 待复核列表 ====================

    @Test
    @DisplayName("B1: 空列表 — 无待复核评价")
    void getPendingEvaluations_emptyList() throws Exception {
        when(evaluationMapper.selectList(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/evaluations/pending")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("B1: 正常返回待复核评价列表")
    void getPendingEvaluations_normalList() throws Exception {
        when(evaluationMapper.selectList(any())).thenReturn(Arrays.asList(pendingEval1, pendingEval2));

        mockMvc.perform(get("/admin/evaluations/pending")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].evaluationId").value(1))
                .andExpect(jsonPath("$.data[0].projectId").value(PROJECT_ID.intValue()))
                .andExpect(jsonPath("$.data[0].evaluatorId").value(10))
                .andExpect(jsonPath("$.data[0].targetId").value(20))
                .andExpect(jsonPath("$.data[0].communicationScore").value(2))
                .andExpect(jsonPath("$.data[0].taskScore").value(2))
                .andExpect(jsonPath("$.data[0].skillScore").value(1))
                .andExpect(jsonPath("$.data[0].responsibilityScore").value(2))
                .andExpect(jsonPath("$.data[0].status").value("pending_review"))
                .andExpect(jsonPath("$.data[1].evaluationId").value(2));
    }

    @Test
    @DisplayName("B1: 按 projectId 过滤")
    void getPendingEvaluations_withProjectId() throws Exception {
        when(evaluationMapper.selectList(any())).thenReturn(Arrays.asList(pendingEval1));

        mockMvc.perform(get("/admin/evaluations/pending")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("projectId", PROJECT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].projectId").value(PROJECT_ID.intValue()));
    }

    @Test
    @DisplayName("B1: status=all 时返回全部状态评价")
    void getPendingEvaluations_statusAll_returnsAll() throws Exception {
        Evaluation normalEval = new Evaluation();
        normalEval.setId(3L);
        normalEval.setProjectId(PROJECT_ID);
        normalEval.setEvaluatorId(11L);
        normalEval.setTargetId(21L);
        normalEval.setStatus(Evaluation.STATUS_NORMAL);
        normalEval.setCreatedAt(LocalDateTime.now());

        when(evaluationMapper.selectList(any())).thenReturn(Arrays.asList(pendingEval1, normalEval));

        mockMvc.perform(get("/admin/evaluations/pending")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("status", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("B1: GET /admin/evaluations 默认不按状态过滤")
    void listEvaluations_withoutStatus_returnsAll() throws Exception {
        when(evaluationMapper.selectList(any())).thenReturn(Arrays.asList(pendingEval1, pendingEval2));

        mockMvc.perform(get("/admin/evaluations")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("B1: GET /admin/evaluations 可按 status 筛选")
    void listEvaluations_withStatus_filters() throws Exception {
        when(evaluationMapper.selectList(any())).thenReturn(Arrays.asList(pendingEval1));

        mockMvc.perform(get("/admin/evaluations")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("status", Evaluation.STATUS_PENDING_REVIEW))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("pending_review"));
    }

    // ==================== B2: 执行复核 ====================

    @Test
    @DisplayName("B2: approve 成功透传 Service 返回")
    void reviewEvaluation_approve_success() throws Exception {
        EvaluationReviewResult result = new EvaluationReviewResult();
        result.setEvaluationId(EVALUATION_ID);
        result.setOldStatus("pending_review");
        result.setNewStatus("normal");
        result.setTargetId(20L);
        result.setCreditDelta(-5);
        result.setCreditEffectiveChanged(true);

        when(evaluationReviewService.review(any(EvaluationReviewCommand.class)))
                .thenReturn(Result.success(result));

        EvaluationReviewRequest request = new EvaluationReviewRequest("approve", "复核通过");

        mockMvc.perform(post("/admin/evaluations/1/review")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.newStatus").value("normal"))
                .andExpect(jsonPath("$.data.creditEffectiveChanged").value(true));
    }

    @Test
    @DisplayName("B2: void 成功透传 Service 返回")
    void reviewEvaluation_void_success() throws Exception {
        EvaluationReviewResult result = new EvaluationReviewResult();
        result.setEvaluationId(EVALUATION_ID);
        result.setOldStatus("pending_review");
        result.setNewStatus("voided");
        result.setTargetId(20L);
        result.setCreditDelta(-5);
        result.setCreditEffectiveChanged(false);

        when(evaluationReviewService.review(any(EvaluationReviewCommand.class)))
                .thenReturn(Result.success(result));

        EvaluationReviewRequest request = new EvaluationReviewRequest("void", "评价作废");

        mockMvc.perform(post("/admin/evaluations/1/review")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.newStatus").value("voided"))
                .andExpect(jsonPath("$.data.creditEffectiveChanged").value(false));
    }

    @Test
    @DisplayName("B2: keep_no_credit 成功透传 Service 返回")
    void reviewEvaluation_keepNoCredit_success() throws Exception {
        EvaluationReviewResult result = new EvaluationReviewResult();
        result.setEvaluationId(EVALUATION_ID);
        result.setOldStatus("pending_review");
        result.setNewStatus("kept_no_credit");
        result.setTargetId(20L);
        result.setCreditDelta(-5);
        result.setCreditEffectiveChanged(false);

        when(evaluationReviewService.review(any(EvaluationReviewCommand.class)))
                .thenReturn(Result.success(result));

        EvaluationReviewRequest request = new EvaluationReviewRequest("keep_no_credit", "保留但不计分");

        mockMvc.perform(post("/admin/evaluations/1/review")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.newStatus").value("kept_no_credit"))
                .andExpect(jsonPath("$.data.creditEffectiveChanged").value(false));
    }

    @Test
    @DisplayName("B2: EVALUATION_NOT_FOUND 透传 M5005")
    void reviewEvaluation_notFound() throws Exception {
        when(evaluationReviewService.review(any(EvaluationReviewCommand.class)))
                .thenReturn(Result.fail(ReasonCode.EVALUATION_NOT_FOUND));

        EvaluationReviewRequest request = new EvaluationReviewRequest("approve", "复核通过");

        mockMvc.perform(post("/admin/evaluations/999/review")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.EVALUATION_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("B2: INVALID_REVIEW_ACTION 透传 M5007")
    void reviewEvaluation_invalidAction() throws Exception {
        when(evaluationReviewService.review(any(EvaluationReviewCommand.class)))
                .thenReturn(Result.fail(ReasonCode.INVALID_REVIEW_ACTION));

        EvaluationReviewRequest request = new EvaluationReviewRequest("invalid_action", "测试");

        mockMvc.perform(post("/admin/evaluations/1/review")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.INVALID_REVIEW_ACTION.getCode()));
    }

    @Test
    @DisplayName("B2: STATUS_CONFLICT 透传 M1003")
    void reviewEvaluation_statusConflict() throws Exception {
        when(evaluationReviewService.review(any(EvaluationReviewCommand.class)))
                .thenReturn(Result.fail(ReasonCode.STATUS_CONFLICT));

        EvaluationReviewRequest request = new EvaluationReviewRequest("approve", "重复复核");

        mockMvc.perform(post("/admin/evaluations/1/review")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.STATUS_CONFLICT.getCode()));
    }

    @Test
    @DisplayName("B2: REVIEW_NOTE_TOO_LONG 透传 M5008")
    void reviewEvaluation_noteTooLong() throws Exception {
        when(evaluationReviewService.review(any(EvaluationReviewCommand.class)))
                .thenReturn(Result.fail(ReasonCode.REVIEW_NOTE_TOO_LONG));

        EvaluationReviewRequest request = new EvaluationReviewRequest("approve", "a".repeat(501));

        mockMvc.perform(post("/admin/evaluations/1/review")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.REVIEW_NOTE_TOO_LONG.getCode()));
    }

    @Test
    @DisplayName("B2: CREDIT_CHANGE_NOT_FOUND 透传 M5006")
    void reviewEvaluation_creditChangeNotFound() throws Exception {
        when(evaluationReviewService.review(any(EvaluationReviewCommand.class)))
                .thenReturn(Result.fail(ReasonCode.CREDIT_CHANGE_NOT_FOUND));

        EvaluationReviewRequest request = new EvaluationReviewRequest("approve", "复核通过");

        mockMvc.perform(post("/admin/evaluations/1/review")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.CREDIT_CHANGE_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("B2: reviewerId 从 token 注入，不由请求体传入")
    void reviewEvaluation_reviewerIdFromToken() throws Exception {
        EvaluationReviewResult result = new EvaluationReviewResult();
        result.setEvaluationId(EVALUATION_ID);
        result.setOldStatus("pending_review");
        result.setNewStatus("normal");
        result.setTargetId(20L);
        result.setCreditDelta(-5);
        result.setCreditEffectiveChanged(true);

        when(evaluationReviewService.review(any(EvaluationReviewCommand.class)))
                .thenReturn(Result.success(result));

        EvaluationReviewRequest request = new EvaluationReviewRequest("approve", "复核通过");

        mockMvc.perform(post("/admin/evaluations/1/review")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        ArgumentCaptor<EvaluationReviewCommand> captor =
                ArgumentCaptor.forClass(EvaluationReviewCommand.class);
        verify(evaluationReviewService).review(captor.capture());

        EvaluationReviewCommand capturedCommand = captor.getValue();
        assertEquals(ADMIN_ID, capturedCommand.getReviewerId(),
                "reviewerId 必须来自 token 解析值，不能来自请求体");
        assertEquals(EVALUATION_ID, capturedCommand.getEvaluationId(),
                "evaluationId 必须来自 Path Variable");
        assertEquals("approve", capturedCommand.getAction());
    }
}
