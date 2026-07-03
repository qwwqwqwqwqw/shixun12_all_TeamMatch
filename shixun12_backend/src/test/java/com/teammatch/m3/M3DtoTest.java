package com.teammatch.m3;

import com.teammatch.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M3 模块 DTO 单元测试
 */
@DisplayName("M3 DTO 测试")
class M3DtoTest {

    @Test
    @DisplayName("UpdateProfileRequest - getter/setter 应正确工作")
    void updateProfileRequest_shouldWorkCorrectly() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("NewNickname");
        request.setAvatarUrl("http://example.com/avatar.jpg");
        request.setSchool("TestSchool");
        request.setMajor("Computer Science");
        request.setGrade("2024");
        request.setBio("Test bio");

        assertThat(request.getNickname()).isEqualTo("NewNickname");
        assertThat(request.getAvatarUrl()).isEqualTo("http://example.com/avatar.jpg");
        assertThat(request.getSchool()).isEqualTo("TestSchool");
        assertThat(request.getMajor()).isEqualTo("Computer Science");
        assertThat(request.getGrade()).isEqualTo("2024");
        assertThat(request.getBio()).isEqualTo("Test bio");
    }

    @Test
    @DisplayName("UpdateUserSkillsRequest - getter/setter 应正确工作")
    void updateUserSkillsRequest_shouldWorkCorrectly() {
        UpdateUserSkillsRequest request = new UpdateUserSkillsRequest();
        List<Long> skillIds = Arrays.asList(1L, 2L, 3L);
        request.setSkillTagIds(skillIds);

        assertThat(request.getSkillTagIds()).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("UpdateUserSkillsRequest - 应支持空列表")
    void updateUserSkillsRequest_emptyList_shouldWork() {
        UpdateUserSkillsRequest request = new UpdateUserSkillsRequest();
        request.setSkillTagIds(Arrays.asList());

        assertThat(request.getSkillTagIds()).isEmpty();
    }

    @Test
    @DisplayName("AddSkillTagRequest - getter/setter 应正确工作")
    void addSkillTagRequest_shouldWorkCorrectly() {
        AddSkillTagRequest request = new AddSkillTagRequest();
        request.setName("Spring Boot");
        request.setCategory("framework");
        request.setStatus("active");

        assertThat(request.getName()).isEqualTo("Spring Boot");
        assertThat(request.getCategory()).isEqualTo("framework");
        assertThat(request.getStatus()).isEqualTo("active");
    }

    @Test
    @DisplayName("LoginResponse - getter/setter 应正确工作")
    void loginResponse_shouldWorkCorrectly() {
        LoginResponse response = new LoginResponse();
        response.setId(1L);
        response.setNickname("TestUser");
        response.setAvatarUrl("http://example.com/avatar.jpg");
        response.setEmailVerified(true);
        response.setFormalProfileCompleted(true);
        response.setCreditScore(100);
        response.setToken("test-token-123");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNickname()).isEqualTo("TestUser");
        assertThat(response.getAvatarUrl()).isEqualTo("http://example.com/avatar.jpg");
        assertThat(response.getEmailVerified()).isTrue();
        assertThat(response.getFormalProfileCompleted()).isTrue();
        assertThat(response.getCreditScore()).isEqualTo(100);
        assertThat(response.getToken()).isEqualTo("test-token-123");
    }

    @Test
    @DisplayName("PasswordLoginRequest - getter/setter 应正确工作")
    void passwordLoginRequest_shouldWorkCorrectly() {
        PasswordLoginRequest request = new PasswordLoginRequest();
        request.setUsername("admin");
        request.setPassword("password123");

        assertThat(request.getUsername()).isEqualTo("admin");
        assertThat(request.getPassword()).isEqualTo("password123");
    }
}
