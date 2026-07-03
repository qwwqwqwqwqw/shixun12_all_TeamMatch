package com.teammatch.m6.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.m6.dto.PenaltyCreateDTO;
import com.teammatch.m6.dto.PenaltyRevokeDTO;
import com.teammatch.m6.entity.Penalty;
import com.teammatch.config.AuthExceptionHandler;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.exception.AuthorizationException;
import com.teammatch.exception.BusinessException;
import com.teammatch.m6.service.PenaltyService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PenaltyAdminController 单元测试
 * 测试管理端处罚接口
 */
@WebMvcTest(PenaltyAdminController.class)
@Import(AuthExceptionHandler.class)
@DisplayName("处罚管理控制器测试（管理端）")
class PenaltyAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PenaltyService penaltyService;

    @MockBean
    private AuthUtil authUtil;

    private static final String ADMIN_TOKEN = "Bearer admin-token";
    private static final Long ADMIN_ID = 999L;
    private static final Long PENALTY_ID = 1L;
    private static final Long USER_ID = 100L;

    private Penalty activeCreditPenalty;
    private Penalty activeFunctionPenalty;
    private Penalty revokedPenalty;

    @BeforeEach
    void setUp() {
        activeCreditPenalty = new Penalty();
        activeCreditPenalty.setId(PENALTY_ID);
        activeCreditPenalty.setUserId(USER_ID);
        activeCreditPenalty.setType("credit_deduct");
        activeCreditPenalty.setCreditDeductValue(10);
        activeCreditPenalty.setReason("违规操作");
        activeCreditPenalty.setAdminId(ADMIN_ID);
        activeCreditPenalty.setStatus("active");
        activeCreditPenalty.setCreatedAt(LocalDateTime.now().minusDays(1));

        activeFunctionPenalty = new Penalty();
        activeFunctionPenalty.setId(2L);
        activeFunctionPenalty.setUserId(USER_ID);
        activeFunctionPenalty.setType("function_limit");
        activeFunctionPenalty.setReason("恶意举报");
        activeFunctionPenalty.setAdminId(ADMIN_ID);
        activeFunctionPenalty.setStatus("active");
        activeFunctionPenalty.setCreatedAt(LocalDateTime.now().minusDays(2));

        revokedPenalty = new Penalty();
        revokedPenalty.setId(3L);
        revokedPenalty.setUserId(USER_ID);
        revokedPenalty.setType("credit_deduct");
        revokedPenalty.setCreditDeductValue(5);
        revokedPenalty.setReason("临时处罚");
        revokedPenalty.setAdminId(ADMIN_ID);
        revokedPenalty.setStatus("revoked");
        revokedPenalty.setRevokedAt(LocalDateTime.now());
        revokedPenalty.setCreatedAt(LocalDateTime.now().minusDays(3));

        lenient().doNothing().when(authUtil).requireAdmin(ADMIN_TOKEN);
        lenient().when(authUtil.requireUserId(ADMIN_TOKEN)).thenReturn(ADMIN_ID);
    }

    // ==================== 权限校验 ====================

    @Test
    @DisplayName("createPenalty: 非管理员访问返回403")
    void createPenalty_nonAdmin_returnsForbidden() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("credit_deduct");
        dto.setCreditDeductValue(10);
        dto.setReason("违规");

        mockMvc.perform(post("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(penaltyService, never()).createPenalty(any(), any());
    }

    @Test
    @DisplayName("getPenaltyList: 非管理员访问返回403")
    void getPenaltyList_nonAdmin_returnsForbidden() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        mockMvc.perform(get("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(penaltyService, never()).getPenaltyList(any(), any());
    }

    @Test
    @DisplayName("getPenalty: 非管理员访问返回403")
    void getPenalty_nonAdmin_returnsForbidden() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        mockMvc.perform(get("/admin/penalties/1")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(penaltyService, never()).getPenaltyById(any());
    }

    @Test
    @DisplayName("revokePenalty: 非管理员访问返回403")
    void revokePenalty_nonAdmin_returnsForbidden() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        mockMvc.perform(put("/admin/penalties/1/revoke")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(penaltyService, never()).revokePenalty(any(), any(), any());
    }

    // ==================== createPenalty ====================

    @Test
    @DisplayName("createPenalty: 管理员创建credit_deduct处罚")
    void createPenalty_creditDeduct_success() throws Exception {
        when(penaltyService.createPenalty(eq(ADMIN_ID), any(PenaltyCreateDTO.class)))
                .thenReturn(activeCreditPenalty);

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("credit_deduct");
        dto.setCreditDeductValue(10);
        dto.setReason("违规操作");

        mockMvc.perform(post("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.type").value("credit_deduct"))
                .andExpect(jsonPath("$.data.creditDeductValue").value(10))
                .andExpect(jsonPath("$.data.status").value("active"));
    }

    @Test
    @DisplayName("createPenalty: 管理员创建function_limit处罚")
    void createPenalty_functionLimit_success() throws Exception {
        when(penaltyService.createPenalty(eq(ADMIN_ID), any(PenaltyCreateDTO.class)))
                .thenReturn(activeFunctionPenalty);

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("function_limit");
        dto.setReason("恶意举报");

        mockMvc.perform(post("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.type").value("function_limit"));
    }

    @Test
    @DisplayName("createPenalty: 不能对自己执行处罚")
    void createPenalty_selfPenalty_returnsBusinessCode() throws Exception {
        when(penaltyService.createPenalty(eq(ADMIN_ID), any(PenaltyCreateDTO.class)))
                .thenThrow(new BusinessException(ReasonCode.PENALTY_SELF_NOT_ALLOWED));

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(ADMIN_ID);
        dto.setType("function_limit");
        dto.setReason("测试自罚");

        mockMvc.perform(post("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.PENALTY_SELF_NOT_ALLOWED.getCode()))
                .andExpect(jsonPath("$.message").value(ReasonCode.PENALTY_SELF_NOT_ALLOWED.getMessage()));
    }

    @Test
    @DisplayName("createPenalty: 参数校验失败返回400")
    void createPenalty_validationError() throws Exception {
        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        // userId为空
        dto.setType("credit_deduct");
        dto.setReason("违规");

        mockMvc.perform(post("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(penaltyService, never()).createPenalty(any(), any());
    }

    @Test
    @DisplayName("createPenalty: 非法处罚类型返回400")
    void createPenalty_invalidType() throws Exception {
        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("invalid_type");
        dto.setReason("违规");

        mockMvc.perform(post("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("createPenalty: credit_deduct缺少扣分值返回400")
    void createPenalty_missingCreditValue() throws Exception {
        when(penaltyService.createPenalty(eq(ADMIN_ID), any(PenaltyCreateDTO.class)))
                .thenThrow(new IllegalArgumentException("credit_deduct类型的处罚必须指定扣分值"));

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("credit_deduct");
        dto.setReason("违规");
        // 未设置creditDeductValue

        mockMvc.perform(post("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("credit_deduct类型的处罚必须指定扣分值"));
    }

    @Test
    @DisplayName("createPenalty: 未知运行时异常返回UNKNOWN_ERROR")
    void createPenalty_unexpectedRuntimeException_returnsUnknownError() throws Exception {
        when(penaltyService.createPenalty(eq(ADMIN_ID), any(PenaltyCreateDTO.class)))
                .thenThrow(new RuntimeException("数据库连接失败"));

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("function_limit");
        dto.setReason("违规");

        mockMvc.perform(post("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNKNOWN_ERROR.getCode()));
    }

    @Test
    @DisplayName("createPenalty: Token无效返回UNAUTHORIZED")
    void createPenalty_invalidToken_returnsUnauthorized() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireAdmin("Bearer invalid-token");

        PenaltyCreateDTO dto = new PenaltyCreateDTO();
        dto.setUserId(USER_ID);
        dto.setType("credit_deduct");
        dto.setCreditDeductValue(10);
        dto.setReason("违规");

        mockMvc.perform(post("/admin/penalties")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(penaltyService, never()).createPenalty(any(), any());
    }

    // ==================== getPenaltyList ====================

    @Test
    @DisplayName("getPenaltyList: 管理员查询全部处罚")
    void getPenaltyList_all_success() throws Exception {
        List<Penalty> penalties = Arrays.asList(activeCreditPenalty, activeFunctionPenalty, revokedPenalty);
        when(penaltyService.getPenaltyList(null, null)).thenReturn(penalties);

        mockMvc.perform(get("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("getPenaltyList: 按状态筛选active")
    void getPenaltyList_byStatusActive() throws Exception {
        when(penaltyService.getPenaltyList("active", null))
                .thenReturn(Arrays.asList(activeCreditPenalty, activeFunctionPenalty));

        mockMvc.perform(get("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].status").value("active"));
    }

    @Test
    @DisplayName("getPenaltyList: 按类型筛选credit_deduct")
    void getPenaltyList_byType() throws Exception {
        when(penaltyService.getPenaltyList(null, "credit_deduct"))
                .thenReturn(Arrays.asList(activeCreditPenalty, revokedPenalty));

        mockMvc.perform(get("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("type", "credit_deduct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].type").value("credit_deduct"));
    }

    @Test
    @DisplayName("getPenaltyList: 空结果返回空列表")
    void getPenaltyList_empty() throws Exception {
        when(penaltyService.getPenaltyList(any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ==================== getPenalty ====================

    @Test
    @DisplayName("getPenalty: 管理员获取处罚详情")
    void getPenalty_success() throws Exception {
        when(penaltyService.getPenaltyById(PENALTY_ID)).thenReturn(activeCreditPenalty);

        mockMvc.perform(get("/admin/penalties/1")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.type").value("credit_deduct"));
    }

    @Test
    @DisplayName("getPenalty: 处罚不存在返回NOT_FOUND")
    void getPenalty_notFound() throws Exception {
        when(penaltyService.getPenaltyById(999L)).thenReturn(null);

        mockMvc.perform(get("/admin/penalties/999")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_FOUND.getCode()));
    }

    // ==================== revokePenalty ====================

    @Test
    @DisplayName("revokePenalty: 管理员撤销处罚")
    void revokePenalty_success() throws Exception {
        Penalty revoked = new Penalty();
        revoked.setId(PENALTY_ID);
        revoked.setStatus("revoked");
        revoked.setRevokedAt(LocalDateTime.now());

        when(penaltyService.revokePenalty(eq(PENALTY_ID), eq(ADMIN_ID), any(PenaltyRevokeDTO.class)))
                .thenReturn(revoked);

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过，撤销处罚");

        mockMvc.perform(put("/admin/penalties/1/revoke")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.status").value("revoked"));
    }

    @Test
    @DisplayName("revokePenalty: 处罚不存在返回NOT_FOUND")
    void revokePenalty_notFound() throws Exception {
        when(penaltyService.revokePenalty(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("处罚记录不存在"));

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        mockMvc.perform(put("/admin/penalties/999/revoke")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("处罚记录不存在"));
    }

    @Test
    @DisplayName("revokePenalty: 非不存在类IllegalArgumentException返回PARAM_ERROR")
    void revokePenalty_illegalArgumentWithoutNotFound_returnsParamError() throws Exception {
        when(penaltyService.revokePenalty(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("撤销原因格式不合法"));

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        mockMvc.perform(put("/admin/penalties/1/revoke")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("撤销原因格式不合法"));
    }

    @Test
    @DisplayName("revokePenalty: IllegalArgumentException无message时使用默认PARAM_ERROR文案")
    void revokePenalty_illegalArgumentNullMessage_returnsDefaultParamErrorMessage() throws Exception {
        when(penaltyService.revokePenalty(any(), any(), any()))
                .thenThrow(new IllegalArgumentException());

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        mockMvc.perform(put("/admin/penalties/1/revoke")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ReasonCode.PARAM_ERROR.getMessage()));
    }

    @Test
    @DisplayName("revokePenalty: BusinessException返回对应业务码")
    void revokePenalty_businessException_returnsBusinessCode() throws Exception {
        when(penaltyService.revokePenalty(any(), any(), any()))
                .thenThrow(new BusinessException(ReasonCode.USER_NOT_FOUND, "用户不存在"));

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        mockMvc.perform(put("/admin/penalties/1/revoke")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    @DisplayName("revokePenalty: 未知运行时异常返回UNKNOWN_ERROR")
    void revokePenalty_unexpectedRuntimeException_returnsUnknownError() throws Exception {
        when(penaltyService.revokePenalty(any(), any(), any()))
                .thenThrow(new RuntimeException("数据库写入失败"));

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        mockMvc.perform(put("/admin/penalties/1/revoke")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNKNOWN_ERROR.getCode()));
    }

    @Test
    @DisplayName("revokePenalty: 已撤销处罚返回STATUS_CONFLICT")
    void revokePenalty_alreadyRevoked() throws Exception {
        when(penaltyService.revokePenalty(any(), any(), any()))
                .thenThrow(new IllegalStateException("处罚已撤销"));

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("重复撤销");

        mockMvc.perform(put("/admin/penalties/3/revoke")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.STATUS_CONFLICT.getCode()))
                .andExpect(jsonPath("$.message").value("处罚已撤销"));
    }

    @Test
    @DisplayName("revokePenalty: 缺少撤销原因返回400")
    void revokePenalty_missingReason() throws Exception {
        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        // reason为空

        mockMvc.perform(put("/admin/penalties/1/revoke")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(penaltyService, never()).revokePenalty(any(), any(), any());
    }

    @Test
    @DisplayName("revokePenalty: Token无效返回UNAUTHORIZED")
    void revokePenalty_invalidToken_returnsUnauthorized() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireAdmin("Bearer invalid-token");

        PenaltyRevokeDTO dto = new PenaltyRevokeDTO();
        dto.setReason("申诉通过");

        mockMvc.perform(put("/admin/penalties/1/revoke")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(penaltyService, never()).revokePenalty(any(), any(), any());
    }

    @Test
    @DisplayName("getPenaltyList: 同时按状态和类型筛选")
    void getPenaltyList_byStatusAndType() throws Exception {
        when(penaltyService.getPenaltyList("active", "credit_deduct"))
                .thenReturn(Arrays.asList(activeCreditPenalty));

        mockMvc.perform(get("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("status", "active")
                        .param("type", "credit_deduct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("active"))
                .andExpect(jsonPath("$.data[0].type").value("credit_deduct"));
    }

    @Test
    @DisplayName("getPenaltyList: 按状态筛选revoked")
    void getPenaltyList_byStatusRevoked() throws Exception {
        when(penaltyService.getPenaltyList("revoked", null))
                .thenReturn(Arrays.asList(revokedPenalty));

        mockMvc.perform(get("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("status", "revoked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("revoked"));
    }

    @Test
    @DisplayName("getPenaltyList: 按类型筛选function_limit")
    void getPenaltyList_byTypeFunctionLimit() throws Exception {
        when(penaltyService.getPenaltyList(null, "function_limit"))
                .thenReturn(Arrays.asList(activeFunctionPenalty));

        mockMvc.perform(get("/admin/penalties")
                        .header("Authorization", ADMIN_TOKEN)
                        .param("type", "function_limit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("function_limit"));
    }
}
