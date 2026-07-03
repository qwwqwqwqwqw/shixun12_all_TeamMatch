package com.teammatch.m3;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.config.WeChatProperties;
import com.teammatch.dto.LoginRequest;
import com.teammatch.dto.PasswordLoginRequest;
import com.teammatch.dto.PasswordRequest;
import com.teammatch.dto.SendEmailCodeRequest;
import com.teammatch.dto.UsernameRequest;
import com.teammatch.dto.VerifyEmailCodeRequest;
import com.teammatch.entity.User;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.exception.AuthorizationException;
import com.teammatch.exception.DuplicateDataException;
import com.teammatch.exception.NotFoundException;
import com.teammatch.exception.ValidationException;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.EmailService;
import com.teammatch.service.impl.AuthServiceImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * M3 认证服务单元测试
 * 重点覆盖：密码登录权限收口、修改密码校验、管理员创建密码逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("M3 认证服务测试")
class AuthServiceImplTest {

    private static final String JWT_SECRET = "TeamMatchSecretKey2024SecureJwtTokenForHS256Algorithm";
    private static final Long TEST_USER_ID = 1L;
    private static final String CODE_PREFIX = "email:code:";

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private EmailService emailService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WeChatProperties weChatProperties;

    @InjectMocks
    private AuthServiceImpl authService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private BCryptPasswordEncoder passwordEncoder;
    private String validToken;

    @BeforeEach
    void setUp() {
        // 真实 ObjectMapper（用于解析微信 code2session JSON 响应）
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "objectMapper", new ObjectMapper());
        
        passwordEncoder = new BCryptPasswordEncoder();
        // 生成一个有效的测试 Token
        validToken = Jwts.builder()
                .setSubject(String.valueOf(TEST_USER_ID))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();

        // 配置 WeChatProperties mock（被 @InjectMocks 注入，设置默认返回值避免 NPE）
        lenient().when(weChatProperties.getAppId()).thenReturn("mock-appid");
        lenient().when(weChatProperties.getSecret()).thenReturn("mock-secret");
        
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== 密码登录 (passwordLogin) ====================

    @Test
    @DisplayName("密码登录 - 普通用户尝试登录应抛出 ADMIN_REQUIRED")
    void passwordLogin_userRole_shouldThrowAuthorizationException() {
        PasswordLoginRequest request = new PasswordLoginRequest();
        request.setUsername("testuser");
        request.setPassword("123456");

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setUsername("testuser");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setRole("user"); // 普通用户
        user.setStatus("active");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        assertThatThrownBy(() -> authService.passwordLogin(request))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.ADMIN_REQUIRED);
    }

    @Test
    @DisplayName("密码登录 - 用户未设置密码应抛出 PASSWORD_NOT_SET")
    void passwordLogin_noPasswordHash_shouldThrowAuthenticationException() {
        PasswordLoginRequest request = new PasswordLoginRequest();
        request.setUsername("testuser");
        request.setPassword("123456");

        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash(null); // 未设置密码
        user.setRole("admin");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        assertThatThrownBy(() -> authService.passwordLogin(request))
                .isInstanceOf(AuthenticationException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.PASSWORD_NOT_SET);
    }

    @Test
    @DisplayName("密码登录 - 管理员正常登录")
    void passwordLogin_adminSuccess_shouldReturnUser() {
        PasswordLoginRequest request = new PasswordLoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setUsername("admin");
        user.setPasswordHash(passwordEncoder.encode("admin123"));
        user.setRole("admin");
        user.setStatus("active");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        User result = authService.passwordLogin(request);
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("admin");
    }

    // ==================== 修改密码 (changePassword) ====================

    @Test
    @DisplayName("修改密码 - 新旧密码相同应抛出 SAME_OLD_NEW_PASSWORD")
    void changePassword_sameOldNewPassword_shouldThrowValidationException() {
        PasswordRequest request = new PasswordRequest();
        request.setOldPassword("123456");
        request.setNewPassword("123456");

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setPasswordHash(passwordEncoder.encode("123456"));

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        assertThatThrownBy(() -> authService.changePassword(request, "Bearer " + validToken))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.SAME_OLD_NEW_PASSWORD);
    }

    @Test
    @DisplayName("修改密码 - 旧密码错误应抛出 OLD_PASSWORD_INCORRECT")
    void changePassword_wrongOldPassword_shouldThrowAuthenticationException() {
        PasswordRequest request = new PasswordRequest();
        request.setOldPassword("wrongpass");
        request.setNewPassword("newpass123");

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setPasswordHash(passwordEncoder.encode("123456"));

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        assertThatThrownBy(() -> authService.changePassword(request, "Bearer " + validToken))
                .isInstanceOf(AuthenticationException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.OLD_PASSWORD_INCORRECT);
    }

    // ==================== 管理员创建密码 (adminCreatePassword) ====================

    @Test
    @DisplayName("管理员创建密码 - 用户不存在应抛出 USER_NOT_FOUND")
    void adminCreatePassword_userNotFound_shouldThrowNotFoundException() {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("nonexistent");
        request.setPassword("newpass123");

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(createAdminUser());
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> authService.adminCreatePassword(request, "Bearer " + validToken))
                .isInstanceOf(com.teammatch.exception.NotFoundException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("管理员创建密码 - 用户已设置密码应抛出 PASSWORD_ALREADY_SET")
    void adminCreatePassword_passwordAlreadySet_shouldThrowDuplicateDataException() {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("existinguser");
        request.setPassword("newpass123");

        User existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setPasswordHash(passwordEncoder.encode("oldpass"));

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(createAdminUser());
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingUser);

        assertThatThrownBy(() -> authService.adminCreatePassword(request, "Bearer " + validToken))
                .isInstanceOf(com.teammatch.exception.DuplicateDataException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.PASSWORD_ALREADY_SET);
    }

    // ==================== 辅助方法与其他覆盖 ====================

    @Test
    @DisplayName("发送邮箱验证码 - 应成功存入 Redis 并调用邮件服务")
    void sendEmailCode_shouldSuccess() {
        SendEmailCodeRequest request = new SendEmailCodeRequest();
        request.setEmail("test@example.com");
        
        // Mock Redis hasKey 返回 false（允许发送）
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        
        authService.sendEmailCode(request, "Bearer " + validToken);
        
        // 验证邮件服务被调用
        verify(emailService).sendVerificationCode(eq("test@example.com"), anyString());
        // 验证 Redis 设置了验证码和防刷标记
        verify(valueOperations, times(2)).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("发送邮箱验证码 - 60秒内重复发送应抛出异常")
    void sendEmailCode_tooFrequent_shouldThrowException() {
        SendEmailCodeRequest request = new SendEmailCodeRequest();
        request.setEmail("test@example.com");
        
        // Mock Redis hasKey 返回 true（60秒内已发送过）
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        
        assertThatThrownBy(() -> authService.sendEmailCode(request, "Bearer " + validToken))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.SEND_CODE_TOO_FREQUENT);
    }

    @Test
    @DisplayName("发送邮箱验证码 - 邮箱为空应抛出异常")
    void sendEmailCode_emptyEmail_shouldThrowException() {
        SendEmailCodeRequest request = new SendEmailCodeRequest();
        request.setEmail("");
        
        assertThatThrownBy(() -> authService.sendEmailCode(request, "Bearer " + validToken))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.INVALID_EMAIL);
    }

    @Test
    @DisplayName("发送邮箱验证码 - 邮箱格式错误应抛出异常")
    void sendEmailCode_invalidEmailFormat_shouldThrowException() {
        SendEmailCodeRequest request = new SendEmailCodeRequest();
        request.setEmail("invalid-email");
        
        assertThatThrownBy(() -> authService.sendEmailCode(request, "Bearer " + validToken))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.INVALID_EMAIL);
    }

    @Test
    @DisplayName("发送邮箱验证码 - 邮件服务失败应抛出异常并清理 Redis")
    void sendEmailCode_emailServiceFailed_shouldThrowExceptionAndCleanRedis() {
        SendEmailCodeRequest request = new SendEmailCodeRequest();
        request.setEmail("test@example.com");
        
        // Mock Redis hasKey 返回 false（允许发送）
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        // Mock 邮件服务抛出异常
        doThrow(new RuntimeException("SMTP connection failed"))
            .when(emailService).sendVerificationCode(anyString(), anyString());
        
        assertThatThrownBy(() -> authService.sendEmailCode(request, "Bearer " + validToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(ReasonCode.EMAIL_SEND_FAILED.getMessage());
        
        // 验证 Redis 中的脏数据被清理（codeKey 和 sendLimitKey）
        verify(redisTemplate, times(2)).delete(anyString());
    }

    @Test
    @DisplayName("isAdmin - 管理员角色应返回 true")
    void isAdmin_adminRole_shouldReturnTrue() {
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(createAdminUser());
        assertThat(authService.isAdmin("Bearer " + validToken)).isTrue();
    }

    @Test
    @DisplayName("isAdmin - 普通用户角色应返回 false")
    void isAdmin_userRole_shouldReturnFalse() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setRole("user");
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);
        assertThat(authService.isAdmin("Bearer " + validToken)).isFalse();
    }

    @Test
    @DisplayName("isAdmin - 用户不存在应返回 false")
    void isAdmin_nullUser_shouldReturnFalse() {
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(null);
        assertThat(authService.isAdmin("Bearer " + validToken)).isFalse();
    }

    // ==================== 微信登录 (wechatLogin) ====================

    @Test
    @DisplayName("微信登录 - 新用户应创建用户并返回")
    void wechatLogin_newUser_shouldCreateAndReturn() {
        LoginRequest request = new LoginRequest();
        request.setCode("wx-code-123");

        // 模拟微信 code2session 返回（JSON 字符串，微信接口 Content-Type: text/plain）
        String wxResponse = "{\"openid\":\"real-openid-abc123\",\"session_key\":\"session-key-xyz\",\"errcode\":0}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(wxResponse);

        // 用户不存在
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        User result = authService.wechatLogin(request);

        assertThat(result).isNotNull();
        assertThat(result.getOpenid()).isEqualTo("real-openid-abc123");
        assertThat(result.getRole()).isEqualTo("user");
        assertThat(result.getCreditScore()).isEqualTo(100);
        verify(userMapper).insert(any(User.class));
        verify(restTemplate).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("微信登录 - 已存在用户应直接返回")
    void wechatLogin_existingUser_shouldReturnExisting() {
        LoginRequest request = new LoginRequest();
        request.setCode("wx-code-123");

        // 模拟微信 code2session 返回（JSON 字符串）
        String wxResponse = "{\"openid\":\"real-openid-abc123\",\"session_key\":\"session-key-xyz\",\"errcode\":0}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(wxResponse);

        // 用户已存在
        User existingUser = new User();
        existingUser.setId(TEST_USER_ID);
        existingUser.setOpenid("real-openid-abc123");
        existingUser.setNickname("ExistingUser");
        existingUser.setStatus("active");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingUser);

        User result = authService.wechatLogin(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getOpenid()).isEqualTo("real-openid-abc123");
        verify(userMapper, never()).insert(any(User.class));
        verify(restTemplate).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("微信登录 - code 无效应抛出异常")
    void wechatLogin_invalidCode_shouldThrow() {
        LoginRequest request = new LoginRequest();
        request.setCode("invalid-code");

        // 模拟微信返回错误码（JSON 字符串）
        String wxResponse = "{\"errcode\":40029,\"errmsg\":\"invalid code\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(wxResponse);

        assertThatThrownBy(() -> authService.wechatLogin(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("微信登录失败");

        verify(userMapper, never()).selectOne(any());
        verify(userMapper, never()).insert(any());
    }

    // ==================== Mock 登录 (mockLogin) ====================

    @Test
    @DisplayName("Mock登录 - 新用户应创建用户并返回")
    void mockLogin_newUser_shouldCreateAndReturn() {
        LoginRequest request = new LoginRequest();
        request.setCode("mock-openid-123");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        User result = authService.mockLogin(request);

        assertThat(result).isNotNull();
        assertThat(result.getOpenid()).isEqualTo("mock-openid-123");
        assertThat(result.getRole()).isEqualTo("user");
        assertThat(result.getCreditScore()).isEqualTo(100);
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("Mock登录 - 已存在用户应直接返回")
    void mockLogin_existingUser_shouldReturnExisting() {
        LoginRequest request = new LoginRequest();
        request.setCode("mock-openid-123");

        User existingUser = new User();
        existingUser.setId(TEST_USER_ID);
        existingUser.setOpenid("mock-openid-123");
        existingUser.setNickname("ExistingUser");
        existingUser.setStatus("active");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingUser);

        User result = authService.mockLogin(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_USER_ID);
        verify(userMapper, never()).insert(any(User.class));
    }

    // ==================== 验证邮箱 (verifyEmailCode) ====================

    @Test
    @DisplayName("验证邮箱 - 验证码正确应成功验证")
    void verifyEmailCode_correctCode_shouldSuccess() {
        VerifyEmailCodeRequest request = new VerifyEmailCodeRequest();
        request.setCode("123456");
        request.setEmail("test@example.com");

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmailVerified(false);

        when(valueOperations.get(CODE_PREFIX + TEST_USER_ID)).thenReturn("123456");
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        boolean result = authService.verifyEmailCode(request, "Bearer " + validToken);

        assertThat(result).isTrue();
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getEmailVerified()).isTrue();
        verify(userMapper).updateById(user);
    }

    @Test
    @DisplayName("验证邮箱 - 验证码错误应返回 false")
    void verifyEmailCode_wrongCode_shouldReturnFalse() {
        VerifyEmailCodeRequest request = new VerifyEmailCodeRequest();
        request.setCode("wrong-code");
        request.setEmail("test@example.com");

        when(valueOperations.get(CODE_PREFIX + TEST_USER_ID)).thenReturn("123456");

        boolean result = authService.verifyEmailCode(request, "Bearer " + validToken);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("验证邮箱 - 邮箱被其他用户占用应抛出异常")
    void verifyEmailCode_emailOccupied_shouldThrowException() {
        VerifyEmailCodeRequest request = new VerifyEmailCodeRequest();
        request.setCode("123456");
        request.setEmail("occupied@example.com");

        User occupiedUser = new User();
        occupiedUser.setId(999L);
        occupiedUser.setEmail("occupied@example.com");

        when(valueOperations.get(CODE_PREFIX + TEST_USER_ID)).thenReturn("123456");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(occupiedUser);

        assertThatThrownBy(() -> authService.verifyEmailCode(request, "Bearer " + validToken))
                .isInstanceOf(DuplicateDataException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.EMAIL_OCCUPIED);
    }

    // ==================== 管理员修改密码 (adminUpdatePassword) ====================

    @Test
    @DisplayName("管理员修改密码 - 非管理员应抛出 ADMIN_REQUIRED")
    void adminUpdatePassword_nonAdmin_shouldThrowAuthorizationException() {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("targetuser");
        request.setNewPassword("newpass123");

        User normalUser = new User();
        normalUser.setId(TEST_USER_ID);
        normalUser.setRole("user");

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(normalUser);

        assertThatThrownBy(() -> authService.adminUpdatePassword(request, "Bearer " + validToken))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.ADMIN_REQUIRED);
    }

    @Test
    @DisplayName("管理员修改密码 - 用户不存在应抛出 USER_NOT_FOUND")
    void adminUpdatePassword_userNotFound_shouldThrowNotFoundException() {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("nonexistent");
        request.setNewPassword("newpass123");

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(createAdminUser());
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> authService.adminUpdatePassword(request, "Bearer " + validToken))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("管理员修改密码 - 提供正确旧密码应成功修改")
    void adminUpdatePassword_withCorrectOldPassword_shouldSuccess() {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("targetuser");
        request.setOldPassword("oldpass");
        request.setNewPassword("newpass123");

        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setUsername("targetuser");
        targetUser.setPasswordHash(passwordEncoder.encode("oldpass"));

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(createAdminUser());
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(targetUser);

        authService.adminUpdatePassword(request, "Bearer " + validToken);

        verify(userMapper).updateById(targetUser);
    }

    @Test
    @DisplayName("管理员修改密码 - 提供错误旧密码应抛出 OLD_PASSWORD_INCORRECT")
    void adminUpdatePassword_wrongOldPassword_shouldThrowException() {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("targetuser");
        request.setOldPassword("wrongpass");
        request.setNewPassword("newpass123");

        User targetUser = new User();
        targetUser.setUsername("targetuser");
        targetUser.setPasswordHash(passwordEncoder.encode("oldpass"));

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(createAdminUser());
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(targetUser);

        assertThatThrownBy(() -> authService.adminUpdatePassword(request, "Bearer " + validToken))
                .isInstanceOf(AuthenticationException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.OLD_PASSWORD_INCORRECT);
    }

    @Test
    @DisplayName("管理员修改密码 - 不提供旧密码应直接修改")
    void adminUpdatePassword_withoutOldPassword_shouldSuccess() {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("targetuser");
        request.setNewPassword("newpass123");

        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setUsername("targetuser");
        targetUser.setPasswordHash(passwordEncoder.encode("oldpass"));

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(createAdminUser());
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(targetUser);

        authService.adminUpdatePassword(request, "Bearer " + validToken);

        verify(userMapper).updateById(targetUser);
    }

    // ==================== 首次绑定用户名 (bindUsername) ====================

    @Test
    @DisplayName("首次绑定用户名 - 应成功绑定")
    void bindUsername_shouldSuccess() {
        UsernameRequest request = new UsernameRequest();
        request.setUsername("new-username");

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setUsername(null); // 未绑定过

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        authService.bindUsername(request, "Bearer " + validToken);

        assertThat(user.getUsername()).isEqualTo("new-username");
        verify(userMapper).updateById(user);
    }

    @Test
    @DisplayName("首次绑定用户名 - 已绑定过应抛出异常")
    void bindUsername_alreadyBound_shouldThrowException() {
        UsernameRequest request = new UsernameRequest();
        request.setUsername("new-username");

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setUsername("old-username"); // 已绑定

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        assertThatThrownBy(() -> authService.bindUsername(request, "Bearer " + validToken))
                .isInstanceOf(DuplicateDataException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.USERNAME_ALREADY_BOUND);
    }

    @Test
    @DisplayName("首次绑定用户名 - 用户名被占用应抛出异常")
    void bindUsername_usernameOccupied_shouldThrowException() {
        UsernameRequest request = new UsernameRequest();
        request.setUsername("occupied-username");

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setUsername(null);

        User occupiedUser = new User();
        occupiedUser.setId(999L);
        occupiedUser.setUsername("occupied-username");

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);
        // 直接返回被占用的用户
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(occupiedUser);

        assertThatThrownBy(() -> authService.bindUsername(request, "Bearer " + validToken))
                .isInstanceOf(DuplicateDataException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.USERNAME_OCCUPIED);
    }

    // ==================== 修改用户名 (updateUsername) ====================

    @Test
    @DisplayName("修改用户名 - 应成功修改")
    void updateUsername_shouldSuccess() {
        UsernameRequest request = new UsernameRequest();
        request.setUsername("new-username");

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setUsername("old-username");

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        authService.updateUsername(request, "Bearer " + validToken);

        assertThat(user.getUsername()).isEqualTo("new-username");
        verify(userMapper).updateById(user);
    }

    @Test
    @DisplayName("修改用户名 - 用户名被占用应抛出异常")
    void updateUsername_usernameOccupied_shouldThrowException() {
        UsernameRequest request = new UsernameRequest();
        request.setUsername("occupied-username");

        User user = new User();
        user.setId(TEST_USER_ID);

        User occupiedUser = new User();
        occupiedUser.setId(999L);
        occupiedUser.setUsername("occupied-username");

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(occupiedUser);

        assertThatThrownBy(() -> authService.updateUsername(request, "Bearer " + validToken))
                .isInstanceOf(DuplicateDataException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.USERNAME_OCCUPIED);
    }

    // ==================== 管理员创建密码 (成功场景) ====================

    @Test
    @DisplayName("管理员创建密码 - 应成功为用户设置密码")
    void adminCreatePassword_shouldSuccess() {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("targetuser");
        request.setPassword("newpass123");

        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setUsername("targetuser");
        targetUser.setPasswordHash(null); // 未设置密码

        when(userMapper.selectById(TEST_USER_ID)).thenReturn(createAdminUser());
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(targetUser);

        authService.adminCreatePassword(request, "Bearer " + validToken);

        assertThat(targetUser.getPasswordHash()).isNotNull();
        verify(userMapper).updateById(targetUser);
    }

    // ==================== getUserIdFromToken 测试 ====================

    @Test
    @DisplayName("getUserIdFromToken - 从有效 Token 中应正确解析用户 ID")
    void getUserIdFromToken_validToken_shouldReturnUserId() {
        // 生成一个有效的 Token
        String token = Jwts.builder()
                .setSubject(String.valueOf(TEST_USER_ID))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();

        Long userId = authService.getUserIdFromToken("Bearer " + token);

        assertThat(userId).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("getUserIdFromToken - 不带 Bearer 前缀的 Token 也应正确解析")
    void getUserIdFromToken_withoutBearerPrefix_shouldReturnUserId() {
        String token = Jwts.builder()
                .setSubject(String.valueOf(999L))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();

        Long userId = authService.getUserIdFromToken(token);

        assertThat(userId).isEqualTo(999L);
    }

    @Test
    @DisplayName("getUserIdFromToken - 无效 Token 应抛出 AuthenticationException")
    void getUserIdFromToken_invalidToken_shouldThrowException() {
        String invalidToken = "invalid.token.here";

        assertThatThrownBy(() -> authService.getUserIdFromToken(invalidToken))
                .isInstanceOf(com.teammatch.exception.AuthenticationException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("getUserIdFromToken - 过期 Token 应抛出 AuthenticationException")
    void getUserIdFromToken_expiredToken_shouldThrowException() {
        // 生成一个已过期的 Token
        String expiredToken = Jwts.builder()
                .setSubject(String.valueOf(TEST_USER_ID))
                .setIssuedAt(new Date(System.currentTimeMillis() - 8 * 24 * 60 * 60 * 1000)) // 8天前
                .setExpiration(new Date(System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000)) // 1天前过期
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();

        assertThatThrownBy(() -> authService.getUserIdFromToken(expiredToken))
                .isInstanceOf(com.teammatch.exception.AuthenticationException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.UNAUTHORIZED);
    }

    // ==================== 辅助方法 ====================

    private User createAdminUser() {
        User admin = new User();
        admin.setId(TEST_USER_ID);
        admin.setRole("admin");
        return admin;
    }
}
