package com.teammatch.m6.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.m6.dto.ReportCreateDTO;
import com.teammatch.m6.entity.Report;
import com.teammatch.config.AuthExceptionHandler;
import com.teammatch.exception.AuthenticationException;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReportController 单元测试
 * 测试用户端举报接口
 */
@WebMvcTest(ReportController.class)
@Import(AuthExceptionHandler.class)
@DisplayName("举报控制器测试（用户端）")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    @MockBean
    private AuthUtil authUtil;

    private static final String TOKEN = "Bearer test-token";
    private static final Long REPORTER_ID = 100L;

    private Report mockReport;

    @BeforeEach
    void setUp() {
        mockReport = new Report();
        mockReport.setId(1L);
        mockReport.setReporterId(REPORTER_ID);
        mockReport.setTargetType("user");
        mockReport.setTargetId(200L);
        mockReport.setReason("违规内容");
        mockReport.setStatus("pending");

        when(authUtil.requireUserId(TOKEN)).thenReturn(REPORTER_ID);
    }

    // ==================== createReport ====================

    @Test
    @DisplayName("createReport: 成功提交举报")
    void createReport_success() throws Exception {
        when(reportService.createReport(any(Long.class), any(ReportCreateDTO.class)))
                .thenReturn(mockReport);

        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setTargetType("user");
        dto.setTargetId(200L);
        dto.setReason("违规内容");

        mockMvc.perform(post("/reports")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.targetType").value("user"))
                .andExpect(jsonPath("$.data.status").value("pending"));

        verify(reportService).createReport(any(Long.class), any(ReportCreateDTO.class));
    }

    @Test
    @DisplayName("createReport: 参数校验失败返回400")
    void createReport_validationError() throws Exception {
        ReportCreateDTO dto = new ReportCreateDTO();
        // targetType为空，触发@NotBlank校验
        dto.setTargetId(200L);
        dto.setReason("违规内容");

        mockMvc.perform(post("/reports")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(reportService, never()).createReport(any(), any());
    }

    @Test
    @DisplayName("createReport: targetType非法值返回400")
    void createReport_invalidTargetType() throws Exception {
        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setTargetType("invalid"); // 非法值，只能是user或project
        dto.setTargetId(200L);
        dto.setReason("违规内容");

        mockMvc.perform(post("/reports")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("createReport: Token无效返回UNAUTHORIZED")
    void createReport_invalidToken() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireUserId("Bearer invalid-token");

        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setTargetType("user");
        dto.setTargetId(200L);
        dto.setReason("违规内容");

        mockMvc.perform(post("/reports")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(reportService, never()).createReport(any(), any());
    }

    @Test
    @DisplayName("createReport: 业务异常返回PARAM_ERROR")
    void createReport_businessError() throws Exception {
        when(reportService.createReport(any(Long.class), any()))
                .thenThrow(new IllegalArgumentException("参数错误"));

        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setTargetType("user");
        dto.setTargetId(200L);
        dto.setReason("违规内容");

        mockMvc.perform(post("/reports")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.PARAM_ERROR.getCode()));
    }

    @Test
    @DisplayName("createReport: targetId为空返回400")
    void createReport_nullTargetId() throws Exception {
        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setTargetType("user");
        // targetId为空
        dto.setReason("违规内容");

        mockMvc.perform(post("/reports")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(reportService, never()).createReport(any(), any());
    }

    @Test
    @DisplayName("createReport: reason为空返回400")
    void createReport_emptyReason() throws Exception {
        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setTargetType("user");
        dto.setTargetId(200L);
        // reason为空

        mockMvc.perform(post("/reports")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("createReport: reason超长返回400")
    void createReport_reasonTooLong() throws Exception {
        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setTargetType("user");
        dto.setTargetId(200L);
        dto.setReason("a".repeat(501)); // 超过500字符限制

        mockMvc.perform(post("/reports")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("createReport: 成功提交project类型举报")
    void createReport_projectType_success() throws Exception {
        Report projectReport = new Report();
        projectReport.setId(2L);
        projectReport.setReporterId(REPORTER_ID);
        projectReport.setTargetType("project");
        projectReport.setTargetId(300L);
        projectReport.setReason("虚假项目");
        projectReport.setStatus("pending");

        when(reportService.createReport(any(Long.class), any(ReportCreateDTO.class)))
                .thenReturn(projectReport);

        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setTargetType("project");
        dto.setTargetId(300L);
        dto.setReason("虚假项目");

        mockMvc.perform(post("/reports")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.targetType").value("project"))
                .andExpect(jsonPath("$.data.targetId").value(300));
    }

    // ==================== getMyReports ====================

    @Test
    @DisplayName("getMyReports: 成功获取我的举报列表")
    void getMyReports_success() throws Exception {
        Report report2 = new Report();
        report2.setId(2L);
        report2.setReporterId(REPORTER_ID);
        report2.setTargetType("project");
        report2.setTargetId(300L);
        report2.setReason("虚假项目");
        report2.setStatus("resolved");

        List<Report> reports = Arrays.asList(mockReport, report2);
        when(reportService.getMyReports(any(Long.class))).thenReturn(reports);

        mockMvc.perform(get("/reports/my")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].status").value("pending"))
                .andExpect(jsonPath("$.data[1].status").value("resolved"));

        verify(reportService).getMyReports(any(Long.class));
    }

    @Test
    @DisplayName("getMyReports: 无举报记录返回空列表")
    void getMyReports_empty() throws Exception {
        when(reportService.getMyReports(REPORTER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/reports/my")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("getMyReports: Token无效返回UNAUTHORIZED")
    void getMyReports_invalidToken() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireUserId("Bearer invalid-token");

        mockMvc.perform(get("/reports/my")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(reportService, never()).getMyReports(any());
    }
}
