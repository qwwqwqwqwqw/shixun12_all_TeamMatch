package com.teammatch.m3;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.config.GiteeProperties;
import com.teammatch.controller.GiteeOAuthController;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.TechProfileService;
import com.teammatch.service.impl.GiteeSyncService;
import com.teammatch.util.AuthUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Gitee OAuth 控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GiteeOAuthController 测试")
class GiteeOAuthControllerTest {

    private static final String JWT_SECRET = "TeamMatchSecretKey2024SecureJwtTokenForHS256Algorithm";
    private static final Long TEST_USER_ID = 1L;

    @Mock
    private GiteeProperties giteeProperties;

    @Mock
    private GiteeSyncService giteeSyncService;

    @Mock
    private TechProfileService techProfileService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private GiteeOAuthController giteeOAuthController;

    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = "Bearer " + Jwts.builder()
                .setSubject(String.valueOf(TEST_USER_ID))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();

        // Mock 配置
        lenient().when(giteeProperties.getClientId()).thenReturn("test-client-id");
        lenient().when(giteeProperties.getClientSecret()).thenReturn("test-client-secret");
        lenient().when(giteeProperties.getRedirectUri()).thenReturn("http://localhost:8080/api/profile/gitee/callback");
        lenient().when(giteeProperties.getScope()).thenReturn("user_info projects pull_requests");
        lenient().when(giteeProperties.getPublicBaseUrl()).thenReturn("http://localhost:8080");

        // Mock AuthUtil
        lenient().when(authUtil.requireUserId(anyString())).thenReturn(TEST_USER_ID);
    }

    @Test
    @DisplayName("authorize - 应返回 Gitee OAuth 授权页跳转")
    void authorize_shouldRedirectToGitee() {
        RedirectView result = giteeOAuthController.authorize(validToken);

        assertThat(result).isNotNull();
        assertThat(result.getUrl()).contains("gitee.com/oauth/authorize");
        assertThat(result.getUrl()).contains("client_id=test-client-id");
        assertThat(result.getUrl()).contains("response_type=code");
        assertThat(result.getUrl()).contains("user_info");
        assertThat(result.getUrl()).contains("projects");
        assertThat(result.getUrl()).contains("pull_requests");
    }

    @Test
    @DisplayName("authorize - token 无效应抛异常")
    void authorize_invalidToken_shouldThrow() {
        when(authUtil.requireUserId("Bearer invalid")).thenThrow(new RuntimeException("invalid"));

        try {
            giteeOAuthController.authorize("Bearer invalid");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    @DisplayName("callback - OAuth 回调成功应跳转成功页面")
    void callback_shouldRedirectToSuccessOnValidCode() throws Exception {
        // Mock token exchange response
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("access_token", "test-access-token");
        when(restTemplate.postForObject(anyString(), eq(null), eq(String.class)))
                .thenReturn("{\"access_token\":\"test-access-token\"}");
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(tokenData);

        // Mock user fetch
        Map<String, Object> userData = new HashMap<>();
        userData.put("login", "gitee-test-user");
        when(giteeSyncService.fetchGiteeUser("test-access-token")).thenReturn(userData);

        // Mock profile claim
        com.teammatch.entity.TechProfile profile = new com.teammatch.entity.TechProfile();
        profile.setId(10L);
        when(techProfileService.claimProfile(eq("gitee-test-user"), eq("gitee"), eq(TEST_USER_ID)))
                .thenReturn(profile);

        // Mock user mapper
        com.teammatch.entity.User user = new com.teammatch.entity.User();
        user.setId(TEST_USER_ID);
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        // Mock async sync
        doNothing().when(giteeSyncService).syncGiteeDataAsync(eq(10L), eq("test-access-token"));

        RedirectView result = giteeOAuthController.callback("test-code", String.valueOf(TEST_USER_ID));

        assertThat(result).isNotNull();
        assertThat(result.getUrl()).contains("/api/profile/gitee/mp-success");

        // Verify user was updated with gitee info
        assertThat(user.getGiteeUsername()).isEqualTo("gitee-test-user");
        assertThat(user.getGiteeClaimed()).isTrue();
        assertThat(user.getTechProfileId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("callback - 获取 token 失败应跳转错误页")
    void callback_shouldRedirectToErrorWhenTokenFails() throws Exception {
        Map<String, Object> tokenData = new HashMap<>();
        // No access_token in response
        when(restTemplate.postForObject(anyString(), eq(null), eq(String.class)))
                .thenReturn("{\"error\":\"invalid_grant\"}");
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(tokenData);

        RedirectView result = giteeOAuthController.callback("bad-code", String.valueOf(TEST_USER_ID));

        assertThat(result.getUrl()).contains("error=");
    }

    @Test
    @DisplayName("callback - 用户信息获取失败应跳转错误页")
    void callback_shouldRedirectToErrorWhenUserFetchFails() throws Exception {
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("access_token", "test-access-token");
        when(restTemplate.postForObject(anyString(), eq(null), eq(String.class)))
                .thenReturn("{\"access_token\":\"test-access-token\"}");
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(tokenData);

        when(giteeSyncService.fetchGiteeUser("test-access-token")).thenReturn(null);

        RedirectView result = giteeOAuthController.callback("test-code", String.valueOf(TEST_USER_ID));

        assertThat(result.getUrl()).contains("error=");
    }

    @Test
    @DisplayName("callback - 异常时应跳转错误页")
    void callback_shouldRedirectToErrorOnException() throws Exception {
        when(restTemplate.postForObject(anyString(), eq(null), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        RedirectView result = giteeOAuthController.callback("test-code", String.valueOf(TEST_USER_ID));

        assertThat(result.getUrl()).contains("error=");
    }

    @Test
    @DisplayName("success - 应返回 HTML 成功页面")
    void success_shouldReturnHtmlPage() {
        String result = giteeOAuthController.success();

        assertThat(result).contains("Gitee 绑定成功");
        assertThat(result).contains("gitee-auth-success");
        assertThat(result).contains("window.opener.postMessage");
    }
}
