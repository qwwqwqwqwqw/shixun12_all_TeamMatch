package com.teammatch.m6.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.config.AuthExceptionHandler;
import com.teammatch.entity.Appeal;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.m6.dto.AppealCreateDTO;
import com.teammatch.m6.dto.AppealableEvaluationVO;
import com.teammatch.m6.dto.AppealablePenaltyVO;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppealController.class)
@Import(AuthExceptionHandler.class)
@DisplayName("申诉控制器测试（用户端）")
class AppealControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppealService appealService;

    @MockBean
    private AuthUtil authUtil;

    private static final String TOKEN = "Bearer user-token";
    private static final Long USER_ID = 100L;
    private static final Long APPEAL_ID = 1L;

    private Appeal mockAppeal;

    @BeforeEach
    void setUp() {
        mockAppeal = new Appeal();
        mockAppeal.setId(APPEAL_ID);
        mockAppeal.setUserId(USER_ID);
        mockAppeal.setTargetType("evaluation");
        mockAppeal.setTargetId(200L);
        mockAppeal.setReason("评价不公正");
        mockAppeal.setStatus("pending");

        when(authUtil.requireUserId(TOKEN)).thenReturn(USER_ID);
    }

    @Test
    @DisplayName("createAppeal: 成功提交申诉")
    void createAppeal_success() throws Exception {
        when(appealService.createAppeal(eq(USER_ID), any(AppealCreateDTO.class))).thenReturn(mockAppeal);

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("evaluation");
        dto.setTargetId(200L);
        dto.setReason("评价不公正");

        mockMvc.perform(post("/appeals")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.id").value(APPEAL_ID));

        verify(appealService).createAppeal(eq(USER_ID), any(AppealCreateDTO.class));
    }

    @Test
    @DisplayName("createAppeal: Token无效返回M3000")
    void createAppeal_invalidToken() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireUserId("Bearer invalid-token");

        AppealCreateDTO dto = new AppealCreateDTO();
        dto.setTargetType("evaluation");
        dto.setTargetId(200L);
        dto.setReason("评价不公正");

        mockMvc.perform(post("/appeals")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(appealService, never()).createAppeal(any(), any());
    }

    @Test
    @DisplayName("listAppealableEvaluations: 成功获取可申诉评价")
    void listAppealableEvaluations_success() throws Exception {
        AppealableEvaluationVO vo = new AppealableEvaluationVO();
        vo.setEvaluationId(200L);
        vo.setProjectId(1L);
        vo.setProjectTitle("后端实训");
        when(appealService.listAppealableEvaluations(USER_ID)).thenReturn(List.of(vo));

        mockMvc.perform(get("/appeals/appealable/evaluations")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].projectTitle").value("后端实训"));
    }

    @Test
    @DisplayName("listAppealablePenalties: 成功获取可申诉处罚")
    void listAppealablePenalties_success() throws Exception {
        AppealablePenaltyVO vo = new AppealablePenaltyVO();
        vo.setPenaltyId(60L);
        vo.setType("credit_deduct");
        vo.setStatus("active");
        when(appealService.listAppealablePenalties(USER_ID)).thenReturn(List.of(vo));

        mockMvc.perform(get("/appeals/appealable/penalties")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data[0].penaltyId").value(60));
    }

    @Test
    @DisplayName("getMyAppeals: 成功获取列表")
    void getMyAppeals_success() throws Exception {
        when(appealService.getMyAppeals(USER_ID)).thenReturn(List.of(mockAppeal));

        mockMvc.perform(get("/appeals/my")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("getAppealById: 成功查看自己的申诉")
    void getAppealById_success() throws Exception {
        when(appealService.getAppealById(APPEAL_ID)).thenReturn(mockAppeal);

        mockMvc.perform(get("/appeals/" + APPEAL_ID)
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.id").value(APPEAL_ID));
    }

    @Test
    @DisplayName("getAppealById: 查看他人申诉返回M1004")
    void getAppealById_forbidden() throws Exception {
        Appeal otherAppeal = new Appeal();
        otherAppeal.setId(APPEAL_ID);
        otherAppeal.setUserId(999L);
        when(appealService.getAppealById(APPEAL_ID)).thenReturn(otherAppeal);

        mockMvc.perform(get("/appeals/" + APPEAL_ID)
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.FORBIDDEN.getCode()));
    }

    @Test
    @DisplayName("getAppealById: 不存在返回M1002")
    void getAppealById_notFound() throws Exception {
        when(appealService.getAppealById(APPEAL_ID)).thenReturn(null);

        mockMvc.perform(get("/appeals/" + APPEAL_ID)
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_FOUND.getCode()));
    }
}
