package com.teammatch.m6.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.m6.entity.Penalty;
import com.teammatch.config.AuthExceptionHandler;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.m6.service.PenaltyService;
import com.teammatch.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PenaltyController 单元测试
 * 测试用户端处罚查询接口
 */
@WebMvcTest(PenaltyController.class)
@Import(AuthExceptionHandler.class)
@DisplayName("处罚控制器测试（用户端）")
class PenaltyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PenaltyService penaltyService;

    @MockBean
    private AuthUtil authUtil;

    private static final String TOKEN = "Bearer test-token";
    private static final Long USER_ID = 100L;

    private Penalty activeCreditPenalty;
    private Penalty revokedPenalty;

    @BeforeEach
    void setUp() {
        activeCreditPenalty = new Penalty();
        activeCreditPenalty.setId(1L);
        activeCreditPenalty.setUserId(USER_ID);
        activeCreditPenalty.setType("credit_deduct");
        activeCreditPenalty.setCreditDeductValue(10);
        activeCreditPenalty.setReason("违规操作");
        activeCreditPenalty.setStatus("active");
        activeCreditPenalty.setCreatedAt(LocalDateTime.now().minusDays(1));

        revokedPenalty = new Penalty();
        revokedPenalty.setId(2L);
        revokedPenalty.setUserId(USER_ID);
        revokedPenalty.setType("function_limit");
        revokedPenalty.setReason("恶意举报");
        revokedPenalty.setStatus("revoked");
        revokedPenalty.setRevokedAt(LocalDateTime.now());
        revokedPenalty.setCreatedAt(LocalDateTime.now().minusDays(2));

        when(authUtil.requireUserId(TOKEN)).thenReturn(USER_ID);
    }

    // ==================== getMyPenalties ====================

    @Test
    @DisplayName("getMyPenalties: 成功获取我的处罚列表")
    void getMyPenalties_success() throws Exception {
        List<Penalty> penalties = Arrays.asList(activeCreditPenalty, revokedPenalty);
        when(penaltyService.getUserPenalties(USER_ID)).thenReturn(penalties);

        mockMvc.perform(get("/penalties/my")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].status").value("active"))
                .andExpect(jsonPath("$.data[1].status").value("revoked"));

        verify(penaltyService).getUserPenalties(USER_ID);
    }

    @Test
    @DisplayName("getMyPenalties: 无处罚记录返回空列表")
    void getMyPenalties_empty() throws Exception {
        when(penaltyService.getUserPenalties(USER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/penalties/my")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("getMyPenalties: Token无效返回UNAUTHORIZED")
    void getMyPenalties_invalidToken() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireUserId("Bearer invalid-token");

        mockMvc.perform(get("/penalties/my")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(penaltyService, never()).getUserPenalties(any());
    }

    // ==================== getMyActivePenalties ====================

    @Test
    @DisplayName("getMyActivePenalties: 成功获取生效中的处罚")
    void getMyActivePenalties_success() throws Exception {
        when(penaltyService.getUserActivePenalties(USER_ID)).thenReturn(Arrays.asList(activeCreditPenalty));

        mockMvc.perform(get("/penalties/my/active")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("active"));

        verify(penaltyService).getUserActivePenalties(USER_ID);
    }

    @Test
    @DisplayName("getMyActivePenalties: 无生效处罚返回空列表")
    void getMyActivePenalties_empty() throws Exception {
        when(penaltyService.getUserActivePenalties(USER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/penalties/my/active")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("getMyActivePenalties: Token无效返回UNAUTHORIZED")
    void getMyActivePenalties_invalidToken() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireUserId("Bearer invalid-token");

        mockMvc.perform(get("/penalties/my/active")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(penaltyService, never()).getUserActivePenalties(any());
    }
}
