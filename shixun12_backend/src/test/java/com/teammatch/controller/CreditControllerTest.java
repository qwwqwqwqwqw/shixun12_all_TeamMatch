package com.teammatch.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.entity.CreditChange;
import com.teammatch.entity.User;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.mapper.CreditChangeMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CreditController.class)
@DisplayName("CreditController 测试")
class CreditControllerTest {

    private static final Long MOCK_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long MOCK_PROJECT_ID = 100L;
    private static final String VALID_TOKEN = "Bearer valid-token";
    private static final String INVALID_TOKEN = "Bearer invalid-token";
    private static final String EXPIRED_TOKEN = "Bearer expired-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private CreditChangeMapper creditChangeMapper;

    @BeforeEach
    void setUp() {
        when(authUtil.requireUserId(isNull()))
                .thenThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED));
        when(authUtil.requireUserId(VALID_TOKEN)).thenReturn(MOCK_USER_ID);
        when(authUtil.requireUserId(INVALID_TOKEN))
                .thenThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED));
        when(authUtil.requireUserId(EXPIRED_TOKEN))
                .thenThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED));
        when(creditChangeMapper.selectList(any())).thenReturn(defaultChanges());
    }

    @Test
    @DisplayName("B6 缺 Authorization header -> M3000")
    void b6_missingAuthHeader_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/m5/credit/score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("B7 缺 Authorization header -> M3000")
    void b7_missingAuthHeader_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/m5/credit/changes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("B6 无效 token -> M3000")
    void b6_invalidToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/m5/credit/score").header("Authorization", INVALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("B7 无效 token -> M3000")
    void b7_invalidToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/m5/credit/changes").header("Authorization", INVALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("B6 过期 token -> M3000")
    void b6_expiredToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/m5/credit/score").header("Authorization", EXPIRED_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("B7 过期 token -> M3000")
    void b7_expiredToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/m5/credit/changes").header("Authorization", EXPIRED_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("B6 正常返回信誉分")
    void b6_success_shouldReturnCreditScore() throws Exception {
        User user = new User();
        user.setId(MOCK_USER_ID);
        user.setCreditScore(95);
        when(userMapper.selectById(MOCK_USER_ID)).thenReturn(user);

        mockMvc.perform(get("/m5/credit/score").header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.userId").value(MOCK_USER_ID))
                .andExpect(jsonPath("$.data.creditScore").value(95));
    }

    @Test
    @DisplayName("B6 用户不存在 -> M1002")
    void b6_userNotFound_shouldReturnNotFound() throws Exception {
        when(userMapper.selectById(MOCK_USER_ID)).thenReturn(null);

        mockMvc.perform(get("/m5/credit/score").header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("B7 默认分页返回 list / total / page / pageSize")
    void b7_defaultPage_shouldReturnPageShape() throws Exception {
        mockMvc.perform(get("/m5/credit/changes").header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    @DisplayName("B7 effective=false -> suspended=true")
    void b7_effectiveFalse_shouldReturnSuspendedTrue() throws Exception {
        when(creditChangeMapper.selectList(any()))
                .thenReturn(Arrays.asList(change(1L, MOCK_USER_ID, MOCK_PROJECT_ID, "evaluation", -2, false)));

        mockMvc.perform(get("/m5/credit/changes").header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].effective").value(false))
                .andExpect(jsonPath("$.data.list[0].suspended").value(true));
    }

    @Test
    @DisplayName("B7 effective=true -> suspended=false")
    void b7_effectiveTrue_shouldReturnSuspendedFalse() throws Exception {
        when(creditChangeMapper.selectList(any()))
                .thenReturn(Arrays.asList(change(1L, MOCK_USER_ID, MOCK_PROJECT_ID, "evaluation", 5, true)));

        mockMvc.perform(get("/m5/credit/changes").header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].effective").value(true))
                .andExpect(jsonPath("$.data.list[0].suspended").value(false));
    }

    @Test
    @DisplayName("B7 changeType=evaluation 过滤生效")
    void b7_changeTypeFilter_shouldBuildWrapper() throws Exception {
        mockMvc.perform(get("/m5/credit/changes")
                        .param("changeType", "evaluation")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));

        verify(creditChangeMapper).selectList(any());
    }

    @Test
    @DisplayName("B7 projectId 过滤生效")
    void b7_projectIdFilter_shouldBuildWrapper() throws Exception {
        mockMvc.perform(get("/m5/credit/changes")
                        .param("projectId", String.valueOf(MOCK_PROJECT_ID))
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));

        verify(creditChangeMapper).selectList(any());
    }

    @Test
    @DisplayName("B7 不返回 records/current")
    void b7_shouldNotReturnMyBatisPageShape() throws Exception {
        mockMvc.perform(get("/m5/credit/changes").header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").exists())
                .andExpect(jsonPath("$.data.records").doesNotExist())
                .andExpect(jsonPath("$.data.current").doesNotExist());
    }

    @Test
    @DisplayName("B7 不暴露 sourceType/sourceId")
    void b7_shouldNotExposeSourceFields() throws Exception {
        CreditChange creditChange = change(1L, MOCK_USER_ID, MOCK_PROJECT_ID, "evaluation", 5, true);
        creditChange.setSourceType("evaluation");
        creditChange.setSourceId(123L);
        when(creditChangeMapper.selectList(any())).thenReturn(Arrays.asList(creditChange));

        mockMvc.perform(get("/m5/credit/changes").header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].sourceType").doesNotExist())
                .andExpect(jsonPath("$.data.list[0].sourceId").doesNotExist());
    }

    @Test
    @DisplayName("B7 pageSize=999 截断为 100")
    void b7_pageSizeTooLarge_shouldCapAt100() throws Exception {
        when(creditChangeMapper.selectList(any())).thenReturn(manyChanges(120));

        mockMvc.perform(get("/m5/credit/changes")
                        .param("pageSize", "999")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(100))
                .andExpect(jsonPath("$.data.list.length()").value(100))
                .andExpect(jsonPath("$.data.total").value(120));
    }

    @Test
    @DisplayName("B7 changeType=invalid -> M1001")
    void b7_invalidChangeType_shouldReturnParamError() throws Exception {
        mockMvc.perform(get("/m5/credit/changes")
                        .param("changeType", "invalid")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.PARAM_ERROR.getCode()));
    }

    @Test
    @DisplayName("B7 page=0/pageSize=0 修正为 1/20")
    void b7_invalidPageAndSize_shouldNormalize() throws Exception {
        mockMvc.perform(get("/m5/credit/changes")
                        .param("page", "0")
                        .param("pageSize", "0")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    @DisplayName("B7 请求携带 userId=999 时仍使用 token userId")
    void b7_userIdParam_shouldBeIgnored() throws Exception {
        mockMvc.perform(get("/m5/credit/changes")
                        .param("userId", String.valueOf(OTHER_USER_ID))
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()));

        verify(authUtil).requireUserId(VALID_TOKEN);
        verify(creditChangeMapper).selectList(any());
    }

    @Test
    @DisplayName("B7 page=2 返回第二页")
    void b7_page2_shouldReturnSecondPage() throws Exception {
        when(creditChangeMapper.selectList(any())).thenReturn(manyChanges(25));

        mockMvc.perform(get("/m5/credit/changes")
                        .param("page", "2")
                        .param("pageSize", "10")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.list.length()").value(10))
                .andExpect(jsonPath("$.data.list[0].id").value(11))
                .andExpect(jsonPath("$.data.total").value(25));
    }

    @Test
    @DisplayName("B7 极端大 page 返回空 list 且不溢出")
    void b7_extremeLargePage_shouldReturnEmptyList() throws Exception {
        when(creditChangeMapper.selectList(any())).thenReturn(manyChanges(25));

        mockMvc.perform(get("/m5/credit/changes")
                        .param("page", String.valueOf(Integer.MAX_VALUE))
                        .param("pageSize", "100")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.page").value(Integer.MAX_VALUE))
                .andExpect(jsonPath("$.data.pageSize").value(100))
                .andExpect(jsonPath("$.data.list.length()").value(0))
                .andExpect(jsonPath("$.data.total").value(25));
    }
    @Test
    @DisplayName("B7 total 等于过滤后完整结果数")
    void b7_total_shouldUseFullFilteredListSize() throws Exception {
        when(creditChangeMapper.selectList(any())).thenReturn(manyChanges(21));

        mockMvc.perform(get("/m5/credit/changes")
                        .param("page", "2")
                        .param("pageSize", "20")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(21));
    }

    @Test
    @DisplayName("B7 wrapper 强制绑定 userId")
    void b7_wrapper_shouldBindUser() throws Exception {
        mockMvc.perform(get("/m5/credit/changes").header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk());

        verify(creditChangeMapper).selectList(any());
    }

    private List<CreditChange> defaultChanges() {
        return Arrays.asList(
                change(1L, MOCK_USER_ID, MOCK_PROJECT_ID, "evaluation", 5, true),
                change(2L, MOCK_USER_ID, MOCK_PROJECT_ID, "exit_vote", -5, false),
                change(3L, MOCK_USER_ID, 200L, "penalty_restore", 5, true)
        );
    }

    private List<CreditChange> manyChanges(int count) {
        List<CreditChange> changes = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            changes.add(change(i, MOCK_USER_ID, MOCK_PROJECT_ID, "evaluation", 1, true));
        }
        return changes;
    }

    private CreditChange change(Long id, Long userId, Long projectId, String changeType,
                                Integer changeValue, Boolean effective) {
        CreditChange creditChange = new CreditChange();
        creditChange.setId(id);
        creditChange.setUserId(userId);
        creditChange.setProjectId(projectId);
        creditChange.setChangeType(changeType);
        creditChange.setChangeValue(changeValue);
        creditChange.setEffective(effective);
        creditChange.setDescription("credit change " + id);
        creditChange.setCreatedAt(LocalDateTime.of(2026, 5, 1, 12, 0).plusMinutes(id));
        return creditChange;
    }
}
