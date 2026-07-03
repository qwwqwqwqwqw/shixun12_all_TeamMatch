package com.teammatch.m3;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.controller.ProfileController;
import com.teammatch.dto.AddSkillTagRequest;
import com.teammatch.dto.ProfileDetailVO;
import com.teammatch.dto.UpdateProfileRequest;
import com.teammatch.dto.UpdateUserSkillsRequest;
import com.teammatch.entity.SkillTag;
import com.teammatch.entity.User;
import com.teammatch.exception.DuplicateDataException;
import com.teammatch.exception.NotFoundException;
import com.teammatch.service.AuthService;
import com.teammatch.service.ProfileService;
import com.teammatch.service.storage.OssService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * M3 ProfileController 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("M3 ProfileController 测试")
class ProfileControllerTest {

    private static final String JWT_SECRET = "TeamMatchSecretKey2024SecureJwtTokenForHS256Algorithm";
    private static final Long TEST_USER_ID = 1L;

    @Mock
    private ProfileService profileService;

    @Mock
    private AuthService authService;

    @Mock
    private AuthUtil authUtil;

    @Mock
    private OssService ossService;

    @InjectMocks
    private ProfileController profileController;

    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = "Bearer " + Jwts.builder()
                .setSubject(String.valueOf(TEST_USER_ID))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();
        
        // Mock AuthUtil 的行为（使用 lenient 避免 UnnecessaryStubbingException）
        lenient().when(authUtil.requireUserId(anyString())).thenReturn(TEST_USER_ID);
        lenient().doNothing().when(authUtil).requireAdmin(anyString());
        lenient().when(ossService.resolveAccessibleUrl(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("updateProfile - 成功更新档案应返回成功")
    void updateProfile_shouldReturnSuccess() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("NewNickname");
        request.setSchool("NewSchool");
        request.setMajor("CS");

        doNothing().when(profileService).updateProfile(eq(TEST_USER_ID), any());

        Result<Void> result = profileController.updateProfile(request, validToken);

        assertThat(result.isSuccess()).isTrue();
        verify(profileService).updateProfile(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("bindGithub - 成功绑定 GitHub 应返回成功")
    void bindGithub_shouldReturnSuccess() {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("githubUsername", "testgithub");

        doNothing().when(profileService).bindGithub(eq(TEST_USER_ID), eq("testgithub"));

        Result<Void> result = profileController.bindGithub(params, validToken);

        assertThat(result.isSuccess()).isTrue();
        verify(profileService).bindGithub(eq(TEST_USER_ID), eq("testgithub"));
    }

    @Test
    @DisplayName("updateGithub - 成功更新 GitHub 用户名应返回成功")
    void updateGithub_shouldReturnSuccess() {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("githubUsername", "newgithub");

        doNothing().when(profileService).updateGithub(eq(TEST_USER_ID), eq("newgithub"));

        Result<Void> result = profileController.updateGithub(params, validToken);

        assertThat(result.isSuccess()).isTrue();
        verify(profileService).updateGithub(eq(TEST_USER_ID), eq("newgithub"));
    }

    @Test
    @DisplayName("updateGithub - GitHub 未绑定应返回失败")
    void updateGithub_notBound_shouldReturnFail() {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("githubUsername", "newgithub");

        doThrow(new RuntimeException(com.teammatch.common.ReasonCode.GITHUB_NOT_BOUND.getMessage()))
                .when(profileService).updateGithub(eq(TEST_USER_ID), eq("newgithub"));

        Result<Void> result = profileController.updateGithub(params, validToken);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("addSkill - 成功添加技能应返回成功")
    void addSkill_shouldReturnSuccess() {
        java.util.Map<String, Long> params = new java.util.HashMap<>();
        params.put("skillTagId", 1L);

        doNothing().when(profileService).addSkill(eq(TEST_USER_ID), eq(1L));

        Result<Void> result = profileController.addSkill(params, validToken);

        assertThat(result.isSuccess()).isTrue();
        verify(profileService).addSkill(eq(TEST_USER_ID), eq(1L));
    }

    @Test
    @DisplayName("updateUserSkills - 成功更新技能列表应返回成功")
    void updateUserSkills_shouldReturnSuccess() {
        UpdateUserSkillsRequest request = new UpdateUserSkillsRequest();
        request.setSkillTagIds(Arrays.asList(1L, 2L, 3L));

        doNothing().when(profileService).updateUserSkills(eq(TEST_USER_ID), eq(Arrays.asList(1L, 2L, 3L)));

        Result<Void> result = profileController.updateUserSkills(request, validToken);

        assertThat(result.isSuccess()).isTrue();
        verify(profileService).updateUserSkills(eq(TEST_USER_ID), eq(Arrays.asList(1L, 2L, 3L)));
    }

    @Test
    @DisplayName("updateUserSkills - 清空技能列表应返回成功")
    void updateUserSkills_emptyList_shouldReturnSuccess() {
        UpdateUserSkillsRequest request = new UpdateUserSkillsRequest();
        request.setSkillTagIds(Collections.emptyList());

        doNothing().when(profileService).updateUserSkills(eq(TEST_USER_ID), eq(Collections.emptyList()));

        Result<Void> result = profileController.updateUserSkills(request, validToken);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("updateUserSkills - 技能标签不存在应返回失败")
    void updateUserSkills_skillNotFound_shouldReturnFail() {
        UpdateUserSkillsRequest request = new UpdateUserSkillsRequest();
        request.setSkillTagIds(Arrays.asList(999L));

        doThrow(new NotFoundException(com.teammatch.common.ReasonCode.SKILL_TAG_NOT_FOUND))
                .when(profileService).updateUserSkills(eq(TEST_USER_ID), any());

        Result<Void> result = profileController.updateUserSkills(request, validToken);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SKILL_TAG_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("listSkillTags - 应返回技能标签列表")
    void listSkillTags_shouldReturnTagList() {
        SkillTag tag1 = new SkillTag();
        tag1.setId(1L);
        tag1.setName("Java");
        tag1.setCategory("language");

        SkillTag tag2 = new SkillTag();
        tag2.setId(2L);
        tag2.setName("Python");
        tag2.setCategory("language");

        List<SkillTag> tags = Arrays.asList(tag1, tag2);
        when(profileService.listActiveSkillTags()).thenReturn(tags);

        Result<List<SkillTag>> result = profileController.listSkillTags();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).getName()).isEqualTo("Java");
    }

    @Test
    @DisplayName("listSkillTags - 无技能标签时应返回空列表")
    void listSkillTags_emptyList_shouldReturnEmpty() {
        when(profileService.listActiveSkillTags()).thenReturn(Collections.emptyList());

        Result<List<SkillTag>> result = profileController.listSkillTags();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("addSkillTag - 管理员成功添加技能标签应返回成功")
    void addSkillTag_adminSuccess_shouldReturnSuccess() {
        AddSkillTagRequest request = new AddSkillTagRequest();
        request.setName("Spring Boot");
        request.setCategory("framework");

        lenient().doNothing().when(authUtil).requireAdmin(anyString());
        doNothing().when(profileService).addSkillTag(any(SkillTag.class));

        Result<Void> result = profileController.addSkillTag(request, validToken);

        assertThat(result.isSuccess()).isTrue();
        verify(profileService).addSkillTag(any(SkillTag.class));
    }

    @Test
    @DisplayName("addSkillTag - 非管理员应返回权限错误")
    void addSkillTag_nonAdmin_shouldReturnFail() {
        AddSkillTagRequest request = new AddSkillTagRequest();
        request.setName("Spring Boot");

        // Mock requireAdmin 抛出 AuthorizationException
        doThrow(new com.teammatch.exception.AuthorizationException(ReasonCode.ADMIN_REQUIRED))
            .when(authUtil).requireAdmin(anyString());

        Result<Void> result = profileController.addSkillTag(request, validToken);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.ADMIN_REQUIRED.getCode());
    }

    @Test
    @DisplayName("addSkillTag - 技能标签已存在应返回失败")
    void addSkillTag_duplicate_shouldReturnFail() {
        AddSkillTagRequest request = new AddSkillTagRequest();
        request.setName("Java");
        request.setCategory("language");

        lenient().doNothing().when(authUtil).requireAdmin(anyString());
        doThrow(new DuplicateDataException(ReasonCode.SKILL_TAG_ALREADY_EXISTS))
                .when(profileService).addSkillTag(any(SkillTag.class));

        Result<Void> result = profileController.addSkillTag(request, validToken);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SKILL_TAG_ALREADY_EXISTS.getCode());
    }

    @Test
    @DisplayName("updateProfile - 无效 token 应返回 M3000 错误")
    void updateProfile_invalidToken_shouldReturnM3000() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("NewNickname");

        // Mock requireUserId 抛出 AuthenticationException(M3000)
        doThrow(new com.teammatch.exception.AuthenticationException(ReasonCode.UNAUTHORIZED))
            .when(authUtil).requireUserId(anyString());

        Result<Void> result = profileController.updateProfile(request, "invalid_token");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ReasonCode.UNAUTHORIZED.getCode());
    }

    // ==================== getProfileDetail (VO 抽取后新接口) ====================

    @Test
    @DisplayName("getProfileDetail - 正常返回应包含正确的 VO 字段且不含敏感信息")
    void getProfileDetail_shouldReturnProfileDetailVO() {
        // Given: 构建一个完整的 User 实体
        User user = new User();
        user.setId(1L);
        user.setOpenid("sensitive_openid");
        user.setNickname("测试用户");
        user.setAvatarUrl("http://avatar.url");
        user.setEmail("test@test.com");
        user.setEmailVerified(true);
        user.setSchool("测试大学");
        user.setMajor("计算机");
        user.setGrade("2024");
        user.setBio("个人简介");
        user.setGithubUsername("testuser");
        user.setGithubClaimed(true);
        user.setFormalProfileCompleted(true);
        user.setCreditScore(100);
        user.setRole("user");
        user.setStatus("active");
        user.setUsername("testuser");
        user.setPasswordHash("should_not_expose");
        user.setCreatedAt(java.time.LocalDateTime.of(2026, 5, 25, 20, 13, 15));
        user.setUpdatedAt(java.time.LocalDateTime.of(2026, 5, 25, 20, 13, 15));

        when(profileService.getProfileById(TEST_USER_ID)).thenReturn(user);

        // When
        Result<ProfileDetailVO> result = profileController.getProfileDetail(validToken);

        // Then: 验证成功
        assertThat(result.isSuccess()).isTrue();
        
        // Then: 验证 VO 字段正确
        ProfileDetailVO vo = result.getData();
        assertThat(vo).isNotNull();
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getNickname()).isEqualTo("测试用户");
        assertThat(vo.getAvatarUrl()).isEqualTo("http://avatar.url");
        assertThat(vo.getEmail()).isEqualTo("test@test.com");
        assertThat(vo.getEmailVerified()).isTrue();
        assertThat(vo.getSchool()).isEqualTo("测试大学");
        assertThat(vo.getMajor()).isEqualTo("计算机");
        assertThat(vo.getGrade()).isEqualTo("2024");
        assertThat(vo.getBio()).isEqualTo("个人简介");
        assertThat(vo.getGithubUsername()).isEqualTo("testuser");
        assertThat(vo.getGithubClaimed()).isTrue();
        assertThat(vo.getFormalProfileCompleted()).isTrue();
        assertThat(vo.getCreditScore()).isEqualTo(100);
        assertThat(vo.getRole()).isEqualTo("user");
        assertThat(vo.getStatus()).isEqualTo("active");
        assertThat(vo.getUsername()).isEqualTo("testuser");
        assertThat(vo.getCreatedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 5, 25, 20, 13, 15));
        assertThat(vo.getUpdatedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 5, 25, 20, 13, 15));

        // Then: 验证敏感字段不在 VO 中（通过编译时类型安全保证）
        // ProfileDetailVO 没有 getOpenid() / getPasswordHash() 方法，编译即不通过
    }

    @Test
    @DisplayName("getProfileDetail - 用户不存在应返回 NOT_FOUND")
    void getProfileDetail_userNotFound_shouldReturnNotFound() {
        // Given
        when(profileService.getProfileById(TEST_USER_ID)).thenReturn(null);

        // When
        Result<ProfileDetailVO> result = profileController.getProfileDetail(validToken);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("getProfileDetail - 无效 token 应返回 M3000")
    void getProfileDetail_invalidToken_shouldReturnM3000() {
        // Given: 模拟无效 token，requireUserId 抛出 AuthenticationException(M3000)
        doThrow(new com.teammatch.exception.AuthenticationException(ReasonCode.UNAUTHORIZED))
            .when(authUtil).requireUserId(anyString());

        // When
        Result<ProfileDetailVO> result = profileController.getProfileDetail("invalid_token");

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.UNAUTHORIZED.getCode());
    }

    // ==================== getUserProfile (新接口) ====================

    @Test
    @DisplayName("getUserProfile - 指定 userId 存在时应返回 ProfileDetailVO")
    void getUserProfile_existingUser_shouldReturnProfile() {
        Long targetUserId = 2L;
        User user = new User();
        user.setId(targetUserId);
        user.setNickname("目标用户");
        user.setAvatarUrl("http://avatar.url");
        user.setRole("user");
        user.setStatus("active");

        when(profileService.getProfileById(targetUserId)).thenReturn(user);

        Result<ProfileDetailVO> result = profileController.getUserProfile(targetUserId, validToken);

        assertThat(result.isSuccess()).isTrue();
        ProfileDetailVO vo = result.getData();
        assertThat(vo).isNotNull();
        assertThat(vo.getId()).isEqualTo(targetUserId);
        assertThat(vo.getNickname()).isEqualTo("目标用户");
        assertThat(vo.getAvatarUrl()).isEqualTo("http://avatar.url");
        verify(profileService).getProfileById(targetUserId);
    }

    @Test
    @DisplayName("getUserProfile - 用户不存在应返回 NOT_FOUND")
    void getUserProfile_userNotFound_shouldReturnNotFound() {
        Long nonExistentId = 999L;
        when(profileService.getProfileById(nonExistentId)).thenReturn(null);

        Result<ProfileDetailVO> result = profileController.getUserProfile(nonExistentId, validToken);

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("getUserProfile - 无效 token 应返回 M3000")
    void getUserProfile_invalidToken_shouldReturnM3000() {
        doThrow(new com.teammatch.exception.AuthenticationException(ReasonCode.UNAUTHORIZED))
            .when(authUtil).requireUserId(anyString());

        Result<ProfileDetailVO> result = profileController.getUserProfile(1L, "invalid_token");

        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.UNAUTHORIZED.getCode());
    }
}
