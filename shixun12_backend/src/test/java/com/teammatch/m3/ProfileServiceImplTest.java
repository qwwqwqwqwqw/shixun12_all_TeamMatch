package com.teammatch.m3;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.entity.SkillTag;
import com.teammatch.entity.User;
import com.teammatch.entity.UserSkill;
import com.teammatch.exception.DuplicateDataException;
import com.teammatch.exception.ValidationException;
import com.teammatch.mapper.SkillTagMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.mapper.UserSkillMapper;
import com.teammatch.service.TechProfileService;
import com.teammatch.service.impl.ProfileServiceImpl;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * M3 档案服务单元测试
 * 重点覆盖：技能标签组合查重、全量替换逻辑、GitHub 绑定校验
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("M3 档案服务测试")
class ProfileServiceImplTest {

    private static final Long TEST_USER_ID = 1L;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SkillTagMapper skillTagMapper;

    @Mock
    private UserSkillMapper userSkillMapper;

    @Mock
    private TechProfileService techProfileService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        // 基础桩数据，避免 NPE
        User baseUser = new User();
        baseUser.setId(TEST_USER_ID);
        baseUser.setEmailVerified(false);
        lenient().when(userMapper.selectById(TEST_USER_ID)).thenReturn(baseUser);
    }

    // ==================== 档案更新 (updateProfile) ====================

    @Test
    @DisplayName("更新档案 - 应成功更新所有字段并重新计算正式档案状态")
    void updateProfile_shouldUpdateFieldsAndRecalculateStatus() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmailVerified(true);
        user.setNickname("");
        user.setSchool("");
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        User profile = new User();
        profile.setNickname("NewNick");
        profile.setSchool("NewSchool");
        profile.setMajor("CS");

        profileService.updateProfile(TEST_USER_ID, profile);

        assertThat(user.getNickname()).isEqualTo("NewNick");
        assertThat(user.getSchool()).isEqualTo("NewSchool");
        assertThat(user.getMajor()).isEqualTo("CS");
        verify(userMapper).updateById(user);
    }

    @Test
    @DisplayName("更新档案 - 用户不存在时应静默返回")
    void updateProfile_userNotFound_shouldDoNothing() {
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(null);
        
        profileService.updateProfile(TEST_USER_ID, new User());
        
        verify(userMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("更新档案 - null 字段不应覆盖原有数据")
    void updateProfile_nullFields_shouldNotOverride() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setNickname("OldNick");
        user.setSchool("OldSchool");
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        User profile = new User();
        profile.setNickname(null);
        profile.setSchool(null);
        profile.setMajor("CS");

        profileService.updateProfile(TEST_USER_ID, profile);

        assertThat(user.getNickname()).isEqualTo("OldNick");
        assertThat(user.getSchool()).isEqualTo("OldSchool");
        assertThat(user.getMajor()).isEqualTo("CS");
    }

    // ==================== GitHub 绑定 (bindGithub) ====================

    @Test
    @DisplayName("绑定 GitHub - 应成功设置用户名并标记为已绑定")
    void bindGithub_shouldSuccess() {
        User user = new User();
        user.setId(TEST_USER_ID);
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("{\"login\":\"github-user\"}");

        profileService.bindGithub(TEST_USER_ID, "github-user");

        assertThat(user.getGithubUsername()).isEqualTo("github-user");
        assertThat(user.getGithubClaimed()).isTrue();
        verify(userMapper).updateById(user);
    }

    @Test
    @DisplayName("绑定 GitHub - 用户不存在时应静默返回")
    void bindGithub_userNotFound_shouldDoNothing() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback cb = invocation.getArgument(0);
            return cb.doInTransaction(null);
        });
        lenient().when(userMapper.selectById(TEST_USER_ID)).thenReturn(null);
        
        profileService.bindGithub(TEST_USER_ID, "github-user");
        
        verify(userMapper, never()).updateById(any());
    }

    // ==================== 获取用户技能 (getUserSkills) ====================

    @Test
    @DisplayName("获取用户技能 - 应返回技能 ID 列表")
    void getUserSkills_shouldReturnSkillIds() {
        UserSkill us1 = new UserSkill();
        us1.setSkillTagId(1L);
        UserSkill us2 = new UserSkill();
        us2.setSkillTagId(2L);
        
        when(userSkillMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(us1, us2));

        List<Long> skills = profileService.getUserSkills(TEST_USER_ID);

        assertThat(skills).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("获取用户技能 - 无技能时应返回空列表")
    void getUserSkills_noSkills_shouldReturnEmptyList() {
        when(userSkillMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<Long> skills = profileService.getUserSkills(TEST_USER_ID);

        assertThat(skills).isEmpty();
    }

    // ==================== 列出活跃技能标签 (listActiveSkillTags) ====================

    @Test
    @DisplayName("列出活跃技能标签 - 应返回状态为 active 的标签")
    void listActiveSkillTags_shouldReturnActiveTags() {
        SkillTag tag1 = new SkillTag();
        tag1.setId(1L);
        tag1.setName("Java");
        tag1.setStatus("active");
        SkillTag tag2 = new SkillTag();
        tag2.setId(2L);
        tag2.setName("Python");
        tag2.setStatus("active");

        when(skillTagMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(tag1, tag2));

        List<SkillTag> tags = profileService.listActiveSkillTags();

        assertThat(tags).hasSize(2);
        assertThat(tags.get(0).getName()).isEqualTo("Java");
    }

    // ==================== 技能标签管理 (addSkillTag) ====================

    @Test
    @DisplayName("添加技能标签 - 名称为空应抛出 ValidationException")
    void addSkillTag_emptyName_shouldThrowValidationException() {
        SkillTag tag = new SkillTag();
        tag.setName("");
        tag.setCategory("language");

        assertThatThrownBy(() -> profileService.addSkillTag(tag))
                .isInstanceOf(ValidationException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.SKILL_TAG_NAME_REQUIRED);
    }

    @Test
    @DisplayName("添加技能标签 - 同名同分类已存在应抛出 DuplicateDataException")
    void addSkillTag_duplicateCategoryName_shouldThrowDuplicateDataException() {
        SkillTag tag = new SkillTag();
        tag.setName("Java");
        tag.setCategory("language");

        SkillTag existing = new SkillTag();
        existing.setId(1L);
        existing.setName("Java");
        existing.setCategory("language");

        when(skillTagMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        assertThatThrownBy(() -> profileService.addSkillTag(tag))
                .isInstanceOf(DuplicateDataException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.SKILL_TAG_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("添加技能标签 - 不同分类同名标签应允许添加")
    void addSkillTag_differentCategory_sameName_shouldSuccess() {
        SkillTag tag = new SkillTag();
        tag.setName("Spring");
        tag.setCategory("framework");

        // 模拟数据库中只有 language/Spring，没有 framework/Spring
        when(skillTagMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        profileService.addSkillTag(tag);
        verify(skillTagMapper).insert(tag);
    }

    // ==================== 用户技能更新 (updateUserSkills) ====================

    @Test
    @DisplayName("更新用户技能 - 传入无效技能 ID 应抛出 NotFoundException")
    void updateUserSkills_invalidSkillId_shouldThrowNotFoundException() {
        Long invalidSkillId = 999L;
        
        when(skillTagMapper.selectById(invalidSkillId)).thenReturn(null);

        assertThatThrownBy(() -> profileService.updateUserSkills(TEST_USER_ID, Arrays.asList(invalidSkillId)))
                .isInstanceOf(com.teammatch.exception.NotFoundException.class)
                .hasFieldOrPropertyWithValue("reasonCode", ReasonCode.SKILL_TAG_NOT_FOUND);
    }

    @Test
    @DisplayName("更新用户技能 - 清空技能列表应删除所有旧技能")
    void updateUserSkills_emptyList_shouldDeleteAll() {
        profileService.updateUserSkills(TEST_USER_ID, Collections.emptyList());
        
        verify(userSkillMapper).delete(any(LambdaQueryWrapper.class));
        verify(userSkillMapper, never()).insert(any(UserSkill.class));
    }

    @Test
    @DisplayName("更新用户技能 - 正常替换应删除旧技能并插入新技能")
    void updateUserSkills_shouldReplaceSkills() {
        SkillTag tag1 = new SkillTag();
        tag1.setId(1L);
        tag1.setStatus("active");
        SkillTag tag2 = new SkillTag();
        tag2.setId(2L);
        tag2.setStatus("active");

        when(skillTagMapper.selectById(1L)).thenReturn(tag1);
        when(skillTagMapper.selectById(2L)).thenReturn(tag2);

        profileService.updateUserSkills(TEST_USER_ID, Arrays.asList(1L, 2L));

        verify(userSkillMapper).delete(any(LambdaQueryWrapper.class));
        verify(userSkillMapper, times(2)).insert(any(UserSkill.class));
    }

    // ==================== 为个人添加技能 (addSkill) ====================

    @Test
    @DisplayName("为个人添加技能 - 应成功添加")
    void addSkill_shouldSuccess() {
        SkillTag tag = new SkillTag();
        tag.setId(1L);
        tag.setStatus("active");

        when(skillTagMapper.selectById(1L)).thenReturn(tag);
        when(userSkillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        profileService.addSkill(TEST_USER_ID, 1L);

        verify(userSkillMapper).insert(any(UserSkill.class));
    }

    @Test
    @DisplayName("为个人添加技能 - 技能不存在应静默返回")
    void addSkill_tagNotFound_shouldDoNothing() {
        when(skillTagMapper.selectById(1L)).thenReturn(null);

        profileService.addSkill(TEST_USER_ID, 1L);

        verify(userSkillMapper, never()).insert(any(UserSkill.class));
    }

    @Test
    @DisplayName("为个人添加技能 - 技能非 active 状态应静默返回")
    void addSkill_tagNotActive_shouldDoNothing() {
        SkillTag tag = new SkillTag();
        tag.setId(1L);
        tag.setStatus("inactive");

        when(skillTagMapper.selectById(1L)).thenReturn(tag);

        profileService.addSkill(TEST_USER_ID, 1L);

        verify(userSkillMapper, never()).insert(any(UserSkill.class));
    }

    @Test
    @DisplayName("为个人添加技能 - 已关联应静默返回")
    void addSkill_alreadyLinked_shouldDoNothing() {
        SkillTag tag = new SkillTag();
        tag.setId(1L);
        tag.setStatus("active");

        when(skillTagMapper.selectById(1L)).thenReturn(tag);
        when(userSkillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        profileService.addSkill(TEST_USER_ID, 1L);

        verify(userSkillMapper, never()).insert(any(UserSkill.class));
    }

    // ==================== GitHub 用户名更新 (updateGithub) ====================

    @Test
    @DisplayName("更新 GitHub - 未绑定过直接更新应抛出异常")
    void updateGithub_notClaimed_shouldThrowException() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setGithubClaimed(false); // 未绑定
        
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        assertThatThrownBy(() -> profileService.updateGithub(TEST_USER_ID, "new-username"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(ReasonCode.GITHUB_NOT_BOUND.getMessage());
    }

    @Test
    @DisplayName("更新 GitHub - 已绑定用户正常更新")
    void updateGithub_claimed_shouldSuccess() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setGithubClaimed(true);
        user.setGithubUsername("old-github");
        
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback cb = invocation.getArgument(0);
            return cb.doInTransaction(null);
        });
        when(techProfileService.findByUsernameAndSource(anyString(), eq("github"))).thenReturn(null);
        when(techProfileService.claimProfile(anyString(), eq("github"), anyLong())).thenReturn(new com.teammatch.entity.TechProfile());
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("{\"login\":\"new-username\"}");

        profileService.updateGithub(TEST_USER_ID, "new-username");
        
        verify(userMapper).updateById(user);
        assertThat(user.getGithubUsername()).isEqualTo("new-username");
    }

    @Test
    @DisplayName("绑定 GitHub - 已绑定相同账号应静默返回")
    void bindGithub_sameAccount_shouldDoNothing() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setGithubClaimed(true);
        user.setGithubUsername("same-github");
        
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        profileService.bindGithub(TEST_USER_ID, "same-github");
        
        verify(userMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("绑定 GitHub - 已绑定不同账号应抛异常")
    void bindGithub_alreadyBound_shouldThrow() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setGithubClaimed(true);
        user.setGithubUsername("existing-github");
        
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        assertThatThrownBy(() -> profileService.bindGithub(TEST_USER_ID, "new-github"))
                .isInstanceOf(com.teammatch.exception.ValidationException.class);
    }

    @Test
    @DisplayName("更新 GitHub - 未绑定应抛异常")
    void updateGithub_notBound_shouldThrow() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setGithubClaimed(false);
        
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        assertThatThrownBy(() -> profileService.updateGithub(TEST_USER_ID, "any-github"))
                .isInstanceOf(com.teammatch.exception.ValidationException.class);
    }

    @Test
    @DisplayName("更新 GitHub - 相同账号应静默返回")
    void updateGithub_sameAccount_shouldDoNothing() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setGithubClaimed(true);
        user.setGithubUsername("same-github");
        
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        profileService.updateGithub(TEST_USER_ID, "same-github");
        
        verify(userMapper, never()).updateById(any());
    }

    // ==================== Gitee 绑定 (bindGitee) ====================

    @Test
    @DisplayName("绑定 Gitee - 应成功设置用户名并标记为已绑定")
    void bindGitee_shouldSuccess() {
        User user = new User();
        user.setId(TEST_USER_ID);
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("{\"login\":\"gitee-user\"}");

        profileService.bindGitee(TEST_USER_ID, "gitee-user");

        assertThat(user.getGiteeUsername()).isEqualTo("gitee-user");
        assertThat(user.getGiteeClaimed()).isTrue();
        verify(userMapper).updateById(user);
    }

    @Test
    @DisplayName("绑定 Gitee - 已绑定相同账号应静默返回")
    void bindGitee_sameAccount_shouldDoNothing() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setGiteeClaimed(true);
        user.setGiteeUsername("same-gitee");
        
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        profileService.bindGitee(TEST_USER_ID, "same-gitee");
        
        verify(userMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("绑定 Gitee - 已绑定不同账号应抛异常")
    void bindGitee_alreadyBound_shouldThrow() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setGiteeClaimed(true);
        user.setGiteeUsername("existing-gitee");
        
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        assertThatThrownBy(() -> profileService.bindGitee(TEST_USER_ID, "new-gitee"))
                .isInstanceOf(com.teammatch.exception.ValidationException.class);
    }

    // ==================== Gitee 用户名更新 (updateGitee) ====================

    @Test
    @DisplayName("更新 Gitee - 未绑定应抛异常")
    void updateGitee_notBound_shouldThrow() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setGiteeClaimed(false);
        
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        assertThatThrownBy(() -> profileService.updateGitee(TEST_USER_ID, "any-gitee"))
                .isInstanceOf(com.teammatch.exception.ValidationException.class);
    }

    @Test
    @DisplayName("更新 Gitee - 相同账号应静默返回")
    void updateGitee_sameAccount_shouldDoNothing() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setGiteeClaimed(true);
        user.setGiteeUsername("same-gitee");
        
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);

        profileService.updateGitee(TEST_USER_ID, "same-gitee");
        
        verify(userMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("更新 Gitee - 已绑定用户正常更新")
    void updateGitee_claimed_shouldSuccess() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setGiteeClaimed(true);
        user.setGiteeUsername("old-gitee");
        
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(user);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback cb = invocation.getArgument(0);
            return cb.doInTransaction(null);
        });
        when(techProfileService.findByUsernameAndSource(anyString(), eq("gitee"))).thenReturn(null);
        when(techProfileService.claimProfile(anyString(), eq("gitee"), anyLong())).thenReturn(new com.teammatch.entity.TechProfile());
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("{\"login\":\"new-gitee\"}");

        profileService.updateGitee(TEST_USER_ID, "new-gitee");
        
        verify(userMapper).updateById(user);
        assertThat(user.getGiteeUsername()).isEqualTo("new-gitee");
    }

    // ==================== 获取用户档案 (getProfileById) ====================

    @Test
    @DisplayName("获取档案 - 用户存在应返回 User 实体")
    void getProfileById_existingUser_shouldReturnUser() {
        // Given: setUp 已经 mock 了 userMapper.selectById(TEST_USER_ID) 返回 baseUser

        // When
        User result = profileService.getProfileById(TEST_USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_USER_ID);
        verify(userMapper).selectById(TEST_USER_ID);
    }

    @Test
    @DisplayName("获取档案 - 用户不存在应返回 null")
    void getProfileById_userNotFound_shouldReturnNull() {
        // Given
        Long nonExistentUserId = 999L;
        when(userMapper.selectById(nonExistentUserId)).thenReturn(null);

        // When
        User result = profileService.getProfileById(nonExistentUserId);

        // Then
        assertThat(result).isNull();
        verify(userMapper).selectById(nonExistentUserId);
    }
}
