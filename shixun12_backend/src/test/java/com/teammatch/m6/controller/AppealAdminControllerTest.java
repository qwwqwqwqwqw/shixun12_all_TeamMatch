package com.teammatch.m6.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.config.AuthExceptionHandler;
import com.teammatch.entity.Appeal;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.exception.AuthorizationException;
import com.teammatch.exception.BusinessException;
import com.teammatch.m6.dto.AppealHandleDTO;
import com.teammatch.m6.service.AppealService;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppealAdminController.class)
@Import(AuthExceptionHandler.class)
@DisplayName("申诉控制器测试（管理端）")
class AppealAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppealService appealService;

    @MockBean
    private AuthUtil authUtil;

    private static final String ADMIN_TOKEN = "Bearer admin-token";
    private static final Long ADMIN_ID = 999L;
    private static final Long APPEAL_ID = 1L;

    private Appeal pendingAppeal;

    @BeforeEach
    void setUp() {
        pendingAppeal = new Appeal();
        pendingAppeal.setId(APPEAL_ID);
        pendingAppeal.setUserId(100L);
        pendingAppeal.setTargetType("evaluation");
        pendingAppeal.setTargetId(200L);
        pendingAppeal.setStatus("pending");

        lenient().when(authUtil.requireUserId(ADMIN_TOKEN)).thenReturn(ADMIN_ID);
        lenient().doNothing().when(authUtil).requireAdmin(ADMIN_TOKEN);
    }

    private AppealHandleDTO validHandleDto(String status) {
        AppealHandleDTO dto = new AppealHandleDTO();
        dto.setStatus(status);
        return dto;
    }

    private ResultActions performHandle(Long appealId, AppealHandleDTO dto) throws Exception {
        return performHandle(appealId, dto, ADMIN_TOKEN);
    }

    private ResultActions performHandle(Long appealId, AppealHandleDTO dto, String token) throws Exception {
        return mockMvc.perform(put("/admin/appeals/{id}/handle", appealId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    // ==================== 权限 ====================

    @Test
    @DisplayName("getAppealList: 非管理员返回M3009")
    void getAppealList_nonAdmin() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        mockMvc.perform(get("/admin/appeals").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(appealService, never()).getAppealList(any(), any());
    }

    @Test
    @DisplayName("getAppealList: Token无效返回M3000")
    void getAppealList_invalidToken() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireAdmin("Bearer invalid");

        mockMvc.perform(get("/admin/appeals").header("Authorization", "Bearer invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("getAppealById: 非管理员返回M3009")
    void getAppealById_nonAdmin() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        mockMvc.perform(get("/admin/appeals/" + APPEAL_ID).header("Authorization", ADMIN_TOKEN))
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(appealService, never()).getAppealById(any());
    }

    @Test
    @DisplayName("handleAppeal: 非管理员返回M3009")
    void handleAppeal_nonAdmin() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        performHandle(APPEAL_ID, validHandleDto("approved"))
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(appealService, never()).handleAppeal(any(), any(), any());
    }

    // ==================== getAppealList ====================

    @Test
    @DisplayName("getAppealList: 管理员成功查询全部")
    void getAppealList_success() throws Exception {
        when(appealService.getAppealList(null, null)).thenReturn(List.of(pendingAppeal));

        mockMvc.perform(get("/admin/appeals").header("Authorization", ADMIN_TOKEN))
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("getAppealList: 按 status 与 targetType 筛选")
    void getAppealList_withFilters() throws Exception {
        when(appealService.getAppealList("pending", "penalty")).thenReturn(List.of(pendingAppeal));

        mockMvc.perform(get("/admin/appeals")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("status", "pending")
                        .param("targetType", "penalty"))
                .andExpect(jsonPath("$.code").value("00000"));

        verify(appealService).getAppealList("pending", "penalty");
    }

    @Test
    @DisplayName("getAppealList: 空列表")
    void getAppealList_empty() throws Exception {
        when(appealService.getAppealList(null, null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/appeals").header("Authorization", ADMIN_TOKEN))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== getAppealById ====================

    @Test
    @DisplayName("getAppealById: 管理员成功查询详情")
    void getAppealById_success() throws Exception {
        when(appealService.getAppealById(APPEAL_ID)).thenReturn(pendingAppeal);

        mockMvc.perform(get("/admin/appeals/" + APPEAL_ID).header("Authorization", ADMIN_TOKEN))
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.id").value(APPEAL_ID));
    }

    @Test
    @DisplayName("getAppealById: 不存在返回 NOT_FOUND")
    void getAppealById_notFound() throws Exception {
        when(appealService.getAppealById(999L)).thenReturn(null);

        mockMvc.perform(get("/admin/appeals/999").header("Authorization", ADMIN_TOKEN))
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_FOUND.getCode()));
    }

    // ==================== handleAppeal ====================

    @Test
    @DisplayName("handleAppeal: Token无效返回M3000")
    void handleAppeal_invalidToken() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireAdmin("Bearer invalid-token");

        performHandle(APPEAL_ID, validHandleDto("approved"), "Bearer invalid-token")
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(appealService, never()).handleAppeal(any(), any(), any());
    }

    @Test
    @DisplayName("handleAppeal: 参数校验失败返回400")
    void handleAppeal_validationError() throws Exception {
        AppealHandleDTO dto = new AppealHandleDTO();

        mockMvc.perform(put("/admin/appeals/" + APPEAL_ID + "/handle")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(appealService, never()).handleAppeal(any(), any(), any());
    }

    @Test
    @DisplayName("handleAppeal: 管理员成功批准")
    void handleAppeal_success() throws Exception {
        Appeal handled = new Appeal();
        handled.setId(APPEAL_ID);
        handled.setStatus("approved");
        when(appealService.handleAppeal(eq(APPEAL_ID), eq(ADMIN_ID), any(AppealHandleDTO.class)))
                .thenReturn(handled);

        AppealHandleDTO dto = validHandleDto("approved");
        dto.setHandleResult("同意");

        performHandle(APPEAL_ID, dto)
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.status").value("approved"));
    }

    @Test
    @DisplayName("handleAppeal: IllegalArgumentException 返回 PARAM_ERROR")
    void handleAppeal_illegalArgument_returnsParamError() throws Exception {
        when(appealService.handleAppeal(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("无效的处理结果"));

        performHandle(APPEAL_ID, validHandleDto("approved"))
                .andExpect(jsonPath("$.code").value(ReasonCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("无效的处理结果"));
    }

    @Test
    @DisplayName("handleAppeal: IllegalStateException 返回 STATUS_CONFLICT")
    void handleAppeal_illegalState_returnsStatusConflict() throws Exception {
        when(appealService.handleAppeal(any(), any(), any()))
                .thenThrow(new IllegalStateException("申诉已处理"));

        performHandle(APPEAL_ID, validHandleDto("rejected"))
                .andExpect(jsonPath("$.code").value(ReasonCode.STATUS_CONFLICT.getCode()))
                .andExpect(jsonPath("$.message").value("申诉已处理"));
    }

    @Test
    @DisplayName("handleAppeal: BusinessException 返回对应业务码")
    void handleAppeal_businessException_returnsReasonCode() throws Exception {
        when(appealService.handleAppeal(any(), any(), any()))
                .thenThrow(new BusinessException(ReasonCode.APPEAL_NOT_FOUND, "申诉不存在"));

        performHandle(APPEAL_ID, validHandleDto("approved"))
                .andExpect(jsonPath("$.code").value(ReasonCode.APPEAL_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("申诉不存在"));
    }

    @Test
    @DisplayName("handleAppeal: RuntimeException 返回 UNKNOWN_ERROR 固定文案")
    void handleAppeal_runtimeException_returnsUnknownError() throws Exception {
        when(appealService.handleAppeal(any(), any(), any()))
                .thenThrow(new RuntimeException("internal NPE detail"));

        performHandle(APPEAL_ID, validHandleDto("approved"))
                .andExpect(jsonPath("$.code").value(ReasonCode.UNKNOWN_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ReasonCode.UNKNOWN_ERROR.getMessage()));
    }
}
