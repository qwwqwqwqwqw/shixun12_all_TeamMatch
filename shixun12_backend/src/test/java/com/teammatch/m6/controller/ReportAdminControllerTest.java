package com.teammatch.m6.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.m6.dto.ReportHandleDTO;
import com.teammatch.m6.entity.Report;
import com.teammatch.config.AuthExceptionHandler;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.exception.AuthorizationException;
import com.teammatch.m6.service.ReportService;
import com.teammatch.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReportAdminController 单元测试
 * 测试管理端举报接口
 */
@WebMvcTest(ReportAdminController.class)
@Import(AuthExceptionHandler.class)
@DisplayName("举报管理控制器测试（管理端）")
class ReportAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    @MockBean
    private AuthUtil authUtil;

    private static final String ADMIN_TOKEN = "Bearer admin-token";
    private static final Long REPORT_ID = 1L;
    private static final Long HANDLER_ID = 999L;

    private Report pendingReport;
    private Report resolvedReport;

    @BeforeEach
    void setUp() {
        pendingReport = new Report();
        pendingReport.setId(REPORT_ID);
        pendingReport.setReporterId(100L);
        pendingReport.setTargetType("user");
        pendingReport.setTargetId(200L);
        pendingReport.setReason("违规内容");
        pendingReport.setStatus("pending");

        resolvedReport = new Report();
        resolvedReport.setId(2L);
        resolvedReport.setReporterId(101L);
        resolvedReport.setTargetType("project");
        resolvedReport.setTargetId(300L);
        resolvedReport.setReason("虚假项目");
        resolvedReport.setStatus("resolved");
        resolvedReport.setHandlerId(HANDLER_ID);
        resolvedReport.setHandleResult("已处理");

        lenient().when(authUtil.requireUserId(ADMIN_TOKEN)).thenReturn(HANDLER_ID);
        lenient().doNothing().when(authUtil).requireAdmin(ADMIN_TOKEN);
    }

    private ReportHandleDTO validHandleDto() {
        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        return dto;
    }

    private ResultActions performHandleReport(Long reportId, ReportHandleDTO dto) throws Exception {
        return mockMvc.perform(put("/admin/reports/{id}/handle", reportId)
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    // ==================== 权限校验 ====================

    @Test
    @DisplayName("getReportList: Token无效返回UNAUTHORIZED")
    void getReportList_invalidToken_returnsUnauthorized() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireAdmin("Bearer invalid-token");

        mockMvc.perform(get("/admin/reports")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(reportService, never()).getReportList(any());
    }

    @Test
    @DisplayName("getReportList: 非管理员访问返回403")
    void getReportList_nonAdmin_returnsForbidden() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        mockMvc.perform(get("/admin/reports")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(reportService, never()).getReportList(any());
    }

    @Test
    @DisplayName("getReport: Token无效返回UNAUTHORIZED")
    void getReport_invalidToken_returnsUnauthorized() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireAdmin("Bearer invalid-token");

        mockMvc.perform(get("/admin/reports/1")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(reportService, never()).getReportById(any());
    }

    @Test
    @DisplayName("getReport: 非管理员访问返回403")
    void getReport_nonAdmin_returnsForbidden() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        mockMvc.perform(get("/admin/reports/1")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(reportService, never()).getReportById(any());
    }

    @Test
    @DisplayName("handleReport: 非管理员访问返回403")
    void handleReport_nonAdmin_returnsForbidden() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");

        mockMvc.perform(put("/admin/reports/1/handle")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(reportService, never()).handleReport(any(), any(), any());
    }

    @Test
    @DisplayName("handleReport: Token无效返回UNAUTHORIZED")
    void handleReport_invalidToken_returnsUnauthorized() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireAdmin("Bearer invalid-token");

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");

        mockMvc.perform(put("/admin/reports/1/handle")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(reportService, never()).handleReport(any(), any(), any());
    }

    // ==================== getReportList ====================

    @Test
    @DisplayName("getReportList: 管理员查询全部举报")
    void getReportList_all_success() throws Exception {
        List<Report> reports = Arrays.asList(pendingReport, resolvedReport);
        when(reportService.getReportList(null)).thenReturn(reports);

        mockMvc.perform(get("/admin/reports")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("getReportList: 管理员按pending状态筛选")
    void getReportList_pending_success() throws Exception {
        when(reportService.getReportList("pending")).thenReturn(Arrays.asList(pendingReport));

        mockMvc.perform(get("/admin/reports")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("status", "pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data[0].status").value("pending"));
    }

    @Test
    @DisplayName("getReportList: 管理员按resolved状态筛选")
    void getReportList_resolved_success() throws Exception {
        when(reportService.getReportList("resolved")).thenReturn(Arrays.asList(resolvedReport));

        mockMvc.perform(get("/admin/reports")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("status", "resolved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data[0].status").value("resolved"));
    }

    @Test
    @DisplayName("getReportList: 管理员按dismissed状态筛选")
    void getReportList_dismissed_success() throws Exception {
        Report dismissedReport = new Report();
        dismissedReport.setId(3L);
        dismissedReport.setReporterId(102L);
        dismissedReport.setTargetType("user");
        dismissedReport.setTargetId(400L);
        dismissedReport.setReason("恶意举报");
        dismissedReport.setStatus("dismissed");
        dismissedReport.setHandlerId(HANDLER_ID);
        dismissedReport.setHandleResult("举报不实，予以驳回");

        when(reportService.getReportList("dismissed")).thenReturn(Arrays.asList(dismissedReport));

        mockMvc.perform(get("/admin/reports")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("status", "dismissed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data[0].status").value("dismissed"));
    }

    @Test
    @DisplayName("getReportList: 空结果返回空列表")
    void getReportList_empty() throws Exception {
        when(reportService.getReportList(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/reports")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ==================== getReport ====================

    @Test
    @DisplayName("getReport: 管理员获取举报详情")
    void getReport_success() throws Exception {
        when(reportService.getReportById(REPORT_ID)).thenReturn(pendingReport);

        mockMvc.perform(get("/admin/reports/1")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    @DisplayName("getReport: 举报不存在返回NOT_FOUND")
    void getReport_notFound() throws Exception {
        when(reportService.getReportById(999L)).thenReturn(null);

        mockMvc.perform(get("/admin/reports/999")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_FOUND.getCode()));
    }

    // ==================== handleReport ====================

    @Test
    @DisplayName("handleReport: 管理员处理举报为resolved")
    void handleReport_resolve_success() throws Exception {
        Report handledReport = new Report();
        handledReport.setId(REPORT_ID);
        handledReport.setStatus("resolved");
        handledReport.setHandlerId(HANDLER_ID);
        handledReport.setHandleResult("已核实并处理");

        when(reportService.handleReport(eq(REPORT_ID), any(Long.class), any(ReportHandleDTO.class)))
                .thenReturn(handledReport);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setHandleResult("已核实并处理");

        mockMvc.perform(put("/admin/reports/1/handle")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.status").value("resolved"))
                .andExpect(jsonPath("$.data.handleResult").value("已核实并处理"));
    }

    @Test
    @DisplayName("handleReport: 管理员处理举报为dismissed")
    void handleReport_dismiss_success() throws Exception {
        Report handledReport = new Report();
        handledReport.setId(REPORT_ID);
        handledReport.setStatus("dismissed");
        handledReport.setHandlerId(HANDLER_ID);

        when(reportService.handleReport(eq(REPORT_ID), any(Long.class), any(ReportHandleDTO.class)))
                .thenReturn(handledReport);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("dismissed");
        dto.setHandleResult("举报不实");

        mockMvc.perform(put("/admin/reports/1/handle")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.status").value("dismissed"));
    }

    @Test
    @DisplayName("handleReport: 参数校验失败返回400")
    void handleReport_validationError() throws Exception {
        ReportHandleDTO dto = new ReportHandleDTO();
        // status为空，触发@NotBlank校验
        dto.setHandleResult("处理结果");

        mockMvc.perform(put("/admin/reports/1/handle")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(reportService, never()).handleReport(any(), any(), any());
    }

    @Test
    @DisplayName("handleReport: 非法状态值返回400")
    void handleReport_invalidStatus() throws Exception {
        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("invalid"); // 非法值，只能是resolved或dismissed

        mockMvc.perform(put("/admin/reports/1/handle")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("handleReport: handleResult超长返回400")
    void handleReport_handleResultTooLong() throws Exception {
        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setHandleResult("a".repeat(501)); // 超过500字符限制

        mockMvc.perform(put("/admin/reports/1/handle")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(reportService, never()).handleReport(any(), any(), any());
    }

    @Test
    @DisplayName("handleReport: 无handleResult也能正常处理")
    void handleReport_noHandleResult() throws Exception {
        Report handledReport = new Report();
        handledReport.setId(REPORT_ID);
        handledReport.setStatus("resolved");
        handledReport.setHandlerId(HANDLER_ID);
        // handleResult为null

        when(reportService.handleReport(eq(REPORT_ID), any(Long.class), any(ReportHandleDTO.class)))
                .thenReturn(handledReport);

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        // 不设置handleResult

        mockMvc.perform(put("/admin/reports/1/handle")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.status").value("resolved"));
    }

    @Test
    @DisplayName("handleReport: 举报不存在返回NOT_FOUND")
    void handleReport_notFound() throws Exception {
        when(reportService.handleReport(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("举报不存在"));

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");

        mockMvc.perform(put("/admin/reports/999/handle")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("handleReport: IllegalArgumentException 返回 PARAM_ERROR")
    void handleReport_paramError_returnsParamError() throws Exception {
        when(reportService.handleReport(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("仅 user 类型举报可联动创建处罚"));

        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setStatus("resolved");
        dto.setCreatePenalty(true);

        mockMvc.perform(put("/admin/reports/1/handle")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("仅 user 类型举报可联动创建处罚"));
    }

    @Test
    @DisplayName("handleReport: 重复处理返回STATUS_CONFLICT")
    void handleReport_alreadyHandled() throws Exception {
        when(reportService.handleReport(any(), any(), any()))
                .thenThrow(new IllegalStateException("举报已处理"));

        performHandleReport(1L, validHandleDto())
                .andExpect(jsonPath("$.code").value(ReasonCode.STATUS_CONFLICT.getCode()))
                .andExpect(jsonPath("$.message").value("举报已处理"));
    }

    @Test
    @DisplayName("handleReport: IllegalArgumentException 无 message 时返回默认 PARAM_ERROR 文案")
    void handleReport_illegalArgumentNullMessage_returnsDefaultParamError() throws Exception {
        when(reportService.handleReport(any(), any(), any()))
                .thenThrow(new IllegalArgumentException());

        performHandleReport(1L, validHandleDto())
                .andExpect(jsonPath("$.code").value(ReasonCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ReasonCode.PARAM_ERROR.getMessage()));
    }

    @Test
    @DisplayName("handleReport: IllegalArgumentException 含「举报不存在」子串返回 NOT_FOUND")
    void handleReport_illegalArgumentReportNotExistPhrase_returnsNotFound() throws Exception {
        when(reportService.handleReport(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("举报不存在: 42"));

        performHandleReport(42L, validHandleDto())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("handleReport: IllegalStateException 无 message 时返回默认 STATUS_CONFLICT 文案")
    void handleReport_illegalStateNullMessage_returnsDefaultStatusConflict() throws Exception {
        when(reportService.handleReport(any(), any(), any()))
                .thenThrow(new IllegalStateException());

        performHandleReport(1L, validHandleDto())
                .andExpect(jsonPath("$.code").value(ReasonCode.STATUS_CONFLICT.getCode()))
                .andExpect(jsonPath("$.message").value(ReasonCode.STATUS_CONFLICT.getMessage()));
    }

    @Test
    @DisplayName("handleReport: 未预期 RuntimeException 返回 UNKNOWN_ERROR 且不暴露内部信息")
    void handleReport_runtimeException_returnsUnknownError() throws Exception {
        when(reportService.handleReport(any(), any(), any()))
                .thenThrow(new RuntimeException("Cannot invoke \"foo\" because \"bar\" is null"));

        performHandleReport(1L, validHandleDto())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNKNOWN_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ReasonCode.UNKNOWN_ERROR.getMessage()));
    }
}