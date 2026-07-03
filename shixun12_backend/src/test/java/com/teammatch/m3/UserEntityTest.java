package com.teammatch.m3;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.teammatch.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M3 模块 User 实体单元测试
 */
@DisplayName("M3 User 实体测试")
class UserEntityTest {

    private final IdentifierGenerator idGenerator = new DefaultIdentifierGenerator();

    @Test
    @DisplayName("Snowflake 生成的 ID 应始终为正数且 >= 10^15（15位以上）")
    void snowflakeId_shouldBeLargePositive() {
        for (int i = 0; i < 100; i++) {
            Long id = idGenerator.nextId(new User()).longValue();
            assertThat(id).isPositive();
            assertThat(id.toString().length()).isGreaterThanOrEqualTo(15);
        }
    }

    @Test
    @DisplayName("Snowflake 生成的 ID 不应重复")
    void snowflakeId_shouldNotCollide() {
        java.util.HashSet<Long> ids = new java.util.HashSet<>();
        for (int i = 0; i < 1000; i++) {
            Long id = idGenerator.nextId(new User()).longValue();
            assertThat(ids).doesNotContain(id);
            ids.add(id);
        }
    }

    @Test
    @DisplayName("updateFormalProfileCompleted - 邮箱已验证、昵称和学校不为空时应标记为完成")
    void updateFormalProfileCompleted_allFieldsFilled_shouldBeCompleted() {
        User user = new User();
        user.setEmailVerified(true);
        user.setNickname("TestUser");
        user.setSchool("TestSchool");

        user.updateFormalProfileCompleted();

        assertThat(user.getFormalProfileCompleted()).isTrue();
    }

    @Test
    @DisplayName("updateFormalProfileCompleted - 邮箱未验证时应标记为未完成")
    void updateFormalProfileCompleted_emailNotVerified_shouldBeUncompleted() {
        User user = new User();
        user.setEmailVerified(false);
        user.setNickname("TestUser");
        user.setSchool("TestSchool");

        user.updateFormalProfileCompleted();

        assertThat(user.getFormalProfileCompleted()).isFalse();
    }

    @Test
    @DisplayName("updateFormalProfileCompleted - 昵称为空时应标记为未完成")
    void updateFormalProfileCompleted_nicknameEmpty_shouldBeUncompleted() {
        User user = new User();
        user.setEmailVerified(true);
        user.setNickname("");
        user.setSchool("TestSchool");

        user.updateFormalProfileCompleted();

        assertThat(user.getFormalProfileCompleted()).isFalse();
    }

    @Test
    @DisplayName("updateFormalProfileCompleted - 学校为空时应标记为未完成")
    void updateFormalProfileCompleted_schoolEmpty_shouldBeUncompleted() {
        User user = new User();
        user.setEmailVerified(true);
        user.setNickname("TestUser");
        user.setSchool("");

        user.updateFormalProfileCompleted();

        assertThat(user.getFormalProfileCompleted()).isFalse();
    }

    @Test
    @DisplayName("updateFormalProfileCompleted - 昵称为 null 时应标记为未完成")
    void updateFormalProfileCompleted_nicknameNull_shouldBeUncompleted() {
        User user = new User();
        user.setEmailVerified(true);
        user.setNickname(null);
        user.setSchool("TestSchool");

        user.updateFormalProfileCompleted();

        assertThat(user.getFormalProfileCompleted()).isFalse();
    }

    @Test
    @DisplayName("updateFormalProfileCompleted - 学校为 null 时应标记为未完成")
    void updateFormalProfileCompleted_schoolNull_shouldBeUncompleted() {
        User user = new User();
        user.setEmailVerified(true);
        user.setNickname("TestUser");
        user.setSchool(null);

        user.updateFormalProfileCompleted();

        assertThat(user.getFormalProfileCompleted()).isFalse();
    }

    @Test
    @DisplayName("User getter/setter - 所有字段应能正确设置和获取")
    void userGettersSetters_shouldWorkCorrectly() {
        User user = new User();
        user.setId(1L);
        user.setOpenid("test-openid");
        user.setNickname("TestNickname");
        user.setAvatarUrl("http://example.com/avatar.jpg");
        user.setEmail("test@example.com");
        user.setEmailVerified(true);
        user.setSchool("TestSchool");
        user.setMajor("Computer Science");
        user.setGrade("2024");
        user.setBio("Test bio");
        user.setGithubUsername("testgithub");
        user.setGithubClaimed(true);
        user.setFormalProfileCompleted(true);
        user.setCreditScore(100);
        user.setRole("admin");
        user.setStatus("active");
        user.setUsername("testuser");
        user.setPasswordHash("hashedpassword");

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getOpenid()).isEqualTo("test-openid");
        assertThat(user.getNickname()).isEqualTo("TestNickname");
        assertThat(user.getAvatarUrl()).isEqualTo("http://example.com/avatar.jpg");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getSchool()).isEqualTo("TestSchool");
        assertThat(user.getMajor()).isEqualTo("Computer Science");
        assertThat(user.getGrade()).isEqualTo("2024");
        assertThat(user.getBio()).isEqualTo("Test bio");
        assertThat(user.getGithubUsername()).isEqualTo("testgithub");
        assertThat(user.getGithubClaimed()).isTrue();
        assertThat(user.getFormalProfileCompleted()).isTrue();
        assertThat(user.getCreditScore()).isEqualTo(100);
        assertThat(user.getRole()).isEqualTo("admin");
        assertThat(user.getStatus()).isEqualTo("active");
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getPasswordHash()).isEqualTo("hashedpassword");
    }
}
