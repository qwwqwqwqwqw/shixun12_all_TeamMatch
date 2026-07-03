package com.teammatch.m3;

import com.teammatch.common.Result;
import com.teammatch.common.ReasonCode;
import com.teammatch.controller.AuthController;
import com.teammatch.dto.LoginRequest;
import com.teammatch.dto.LoginResponse;
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
import com.teammatch.service.AuthService;
import com.teammatch.service.impl.AuthServiceImpl;
import com.teammatch.service.storage.OssService;
import io.jsonwebtoken.Jwts;
import org.springframework.core.env.Environment;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * M3 AuthController 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("M3 AuthController 测试")
class AuthControllerTest {

    private static final String JWT_SECRET = "TeamMatchSecretKey2024SecureJwtTokenForHS256Algorithm";
    private static final Long TEST_USER_ID = 1L;

    @Mock
    private AuthService authService;

    @Mock
    private Environment environment;

    @Mock
    private OssService ossService;

    @InjectMocks
    private AuthController authController;

    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = "Bearer " + Jwts.builder()
                .setSubject(String.valueOf(TEST_USER_ID))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();
    }

    @Test
    @DisplayName("login - 成功登录应返回 LoginResponse")
    void login_shouldReturnLoginResponse() {
        LoginRequest request = new LoginRequest();
        request.setCode("wx-code-123");

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setNickname("TestUser");
        user.setAvatarUrl("http://example.com/avatar.jpg");
        user.setEmailVerified(true);
        user.setFormalProfileCompleted(true);
        user.setCreditScore(100);

        when(authService.wechatLogin(request)).thenReturn(user);
        when(authService.generateToken(user)).thenReturn("test-token");

        Result<LoginResponse> result = authController.login(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getData().getToken()).isEqualTo("test-token");
    }

    @Test
    @DisplayName("mockLogin - Mock登录应返回 LoginResponse")
    void mockLogin_shouldReturnLoginResponse() {
        LoginRequest request = new LoginRequest();
        request.setCode("mock-openid-123");

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setNickname("MockUser");
        user.setCreditScore(100);

        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(authService.mockLogin(request)).thenReturn(user);
        when(authService.generateToken(user)).thenReturn("mock-token");

        Result<LoginResponse> result = authController.mockLogin(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getData().getToken()).isEqualTo("mock-token");
    }

    @Test
    @DisplayName("passwordLogin - 管理员成功登录应返回 LoginResponse")
    void passwordLogin_adminSuccess_shouldReturnLoginResponse() {
        PasswordLoginRequest request = new PasswordLoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setUsername("admin");
        user.setNickname("AdminUser");
        user.setRole("admin");
        user.setEmailVerified(true);
        user.setFormalProfileCompleted(true);
        user.setCreditScore(100);

        when(authService.passwordLogin(request)).thenReturn(user);
        when(authService.generateToken(user)).thenReturn("admin-token");

        Result<LoginResponse> result = authController.passwordLogin(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getData().getNickname()).isEqualTo("AdminUser");
        assertThat(result.getData().getToken()).isEqualTo("admin-token");
    }

    @Test
    @DisplayName("passwordLogin - 认证异常应返回失败")
    void passwordLogin_authenticationException_shouldReturnFail() {
        PasswordLoginRequest request = new PasswordLoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpass");

        when(authService.passwordLogin(request))
                .thenThrow(new AuthenticationException(ReasonCode.INVALID_PASSWORD));

        Result<LoginResponse> result = authController.passwordLogin(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.INVALID_PASSWORD.getCode());
    }

    @Test
    @DisplayName("passwordLogin - 权限异常应返回失败")
    void passwordLogin_authorizationException_shouldReturnFail() {
        PasswordLoginRequest request = new PasswordLoginRequest();
        request.setUsername("user");
        request.setPassword("password");

        when(authService.passwordLogin(request))
                .thenThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED));

        Result<LoginResponse> result = authController.passwordLogin(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.ADMIN_REQUIRED.getCode());
    }

    @Test
    @DisplayName("passwordLogin - 校验异常应返回失败")
    void passwordLogin_validationException_shouldReturnFail() {
        PasswordLoginRequest request = new PasswordLoginRequest();
        request.setUsername("");
        request.setPassword("password");

        when(authService.passwordLogin(request))
                .thenThrow(new ValidationException(ReasonCode.USERNAME_REQUIRED));

        Result<LoginResponse> result = authController.passwordLogin(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.USERNAME_REQUIRED.getCode());
    }

    @Test
    @DisplayName("sendEmailCode - 应成功发送验证码")
    void sendEmailCode_shouldSuccess() {
        SendEmailCodeRequest request = new SendEmailCodeRequest();
        request.setEmail("test@example.com");

        doNothing().when(authService).sendEmailCode(any(SendEmailCodeRequest.class), anyString());

        Result<Void> result = authController.sendEmailCode(request, validToken);

        assertThat(result.isSuccess()).isTrue();
        verify(authService).sendEmailCode(eq(request), eq(validToken));
    }

    @Test
    @DisplayName("verifyEmailCode - 验证成功应返回成功消息")
    void verifyEmailCode_success_shouldReturnSuccess() {
        VerifyEmailCodeRequest request = new VerifyEmailCodeRequest();
        request.setCode("123456");
        request.setEmail("test@example.com");

        when(authService.verifyEmailCode(request, validToken)).thenReturn(true);

        Result<String> result = authController.verifyEmailCode(request, validToken);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("邮箱验证成功");
    }

    @Test
    @DisplayName("verifyEmailCode - 验证码错误应返回失败")
    void verifyEmailCode_invalidCode_shouldReturnFail() {
        VerifyEmailCodeRequest request = new VerifyEmailCodeRequest();
        request.setCode("wrongcode");
        request.setEmail("test@example.com");

        when(authService.verifyEmailCode(request, validToken)).thenReturn(false);

        Result<String> result = authController.verifyEmailCode(request, validToken);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.INVALID_CODE.getCode());
    }

    @Test
    @DisplayName("verifyEmailCode - 邮箱被占用应返回失败")
    void verifyEmailCode_emailOccupied_shouldReturnFail() {
        VerifyEmailCodeRequest request = new VerifyEmailCodeRequest();
        request.setCode("123456");
        request.setEmail("occupied@example.com");

        when(authService.verifyEmailCode(request, validToken))
                .thenThrow(new DuplicateDataException(ReasonCode.EMAIL_OCCUPIED));

        Result<String> result = authController.verifyEmailCode(request, validToken);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.EMAIL_OCCUPIED.getCode());
    }

    @Test
    @DisplayName("createPassword - 管理员成功创建密码应返回成功")
    void createPassword_success_shouldReturnSuccess() {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("newuser");
        request.setPassword("newpass123");

        doNothing().when(authService).adminCreatePassword(request, validToken);

        Result<Void> result = authController.createPassword(request, validToken);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("createPassword - 权限异常应返回失败")
    void createPassword_authorizationException_shouldReturnFail() {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("user");
        request.setPassword("password");

        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authService).adminCreatePassword(request, validToken);

        Result<Void> result = authController.createPassword(request, validToken);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.ADMIN_REQUIRED.getCode());
    }

    @Test
    @DisplayName("updatePassword - 管理员成功修改密码应返回成功")
    void updatePassword_success_shouldReturnSuccess() {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("targetuser");
        request.setNewPassword("newpass123");

        doNothing().when(authService).adminUpdatePassword(request, validToken);

        Result<Void> result = authController.updatePassword(request, validToken);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("updatePassword - 认证异常应返回失败")
    void updatePassword_authenticationException_shouldReturnFail() {
        PasswordRequest request = new PasswordRequest();
        request.setUsername("targetuser");
        request.setOldPassword("wrongpass");
        request.setNewPassword("newpass123");

        doThrow(new AuthenticationException(ReasonCode.OLD_PASSWORD_INCORRECT))
                .when(authService).adminUpdatePassword(request, validToken);

        Result<Void> result = authController.updatePassword(request, validToken);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.OLD_PASSWORD_INCORRECT.getCode());
    }

    @Test
    @DisplayName("changePassword - 用户成功修改密码应返回成功")
    void changePassword_success_shouldReturnSuccess() {
        PasswordRequest request = new PasswordRequest();
        request.setOldPassword("oldpass");
        request.setNewPassword("newpass123");

        doNothing().when(authService).changePassword(request, validToken);

        Result<Void> result = authController.changePassword(request, validToken);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("changePassword - 新旧密码相同应返回失败")
    void changePassword_samePassword_shouldReturnFail() {
        PasswordRequest request = new PasswordRequest();
        request.setOldPassword("samepass");
        request.setNewPassword("samepass");

        doThrow(new ValidationException(ReasonCode.SAME_OLD_NEW_PASSWORD))
                .when(authService).changePassword(request, validToken);

        Result<Void> result = authController.changePassword(request, validToken);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SAME_OLD_NEW_PASSWORD.getCode());
    }

    @Test
    @DisplayName("bindUsername - 成功绑定用户名应返回成功")
    void bindUsername_success_shouldReturnSuccess() {
        UsernameRequest request = new UsernameRequest();
        request.setUsername("newusername");

        doNothing().when(authService).bindUsername(request, validToken);

        Result<Void> result = authController.bindUsername(request, validToken);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("bindUsername - 用户名已绑定应返回失败")
    void bindUsername_alreadyBound_shouldReturnFail() {
        UsernameRequest request = new UsernameRequest();
        request.setUsername("existinguser");

        doThrow(new DuplicateDataException(ReasonCode.USERNAME_ALREADY_BOUND))
                .when(authService).bindUsername(request, validToken);

        Result<Void> result = authController.bindUsername(request, validToken);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.USERNAME_ALREADY_BOUND.getCode());
    }

    @Test
    @DisplayName("updateUsername - 成功修改用户名应返回成功")
    void updateUsername_success_shouldReturnSuccess() {
        UsernameRequest request = new UsernameRequest();
        request.setUsername("newusername");

        doNothing().when(authService).updateUsername(request, validToken);

        Result<Void> result = authController.updateUsername(request, validToken);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("updateUsername - 用户名被占用应返回失败")
    void updateUsername_usernameOccupied_shouldReturnFail() {
        UsernameRequest request = new UsernameRequest();
        request.setUsername("occupied");

        doThrow(new DuplicateDataException(ReasonCode.USERNAME_OCCUPIED))
                .when(authService).updateUsername(request, validToken);

        Result<Void> result = authController.updateUsername(request, validToken);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.USERNAME_OCCUPIED.getCode());
    }
}
