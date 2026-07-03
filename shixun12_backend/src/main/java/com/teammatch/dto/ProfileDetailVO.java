package com.teammatch.dto;

import com.teammatch.entity.User;

import java.time.LocalDateTime;

/**
 * 用户档案详情 VO
 * 用于 GET /api/profile/detail 接口返回，只暴露安全的非敏感字段
 *
 * 相比 User 实体，VO 排除了以下敏感/内部字段：
 * - openid（微信 openid，敏感信息）
 * - passwordHash（密码哈希，内部敏感信息）
 */
public class ProfileDetailVO {

    private Long id;
    private String nickname;
    private String avatarUrl;
    private String email;
    private Boolean emailVerified;
    private String school;
    private String major;
    private String grade;
    private String bio;
    private String githubUsername;
    private Boolean githubClaimed;
    private String giteeUsername;
    private Boolean giteeClaimed;
    private Boolean formalProfileCompleted;
    private Integer creditScore;
    private String role;
    private String status;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== 工厂方法 ====================

    /**
     * 从 User 实体转换为 ProfileDetailVO
     * 只复制安全字段，排除 openid 和 passwordHash 等敏感信息
     */
    public static ProfileDetailVO from(User user) {
        if (user == null) {
            return null;
        }
        ProfileDetailVO vo = new ProfileDetailVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setEmail(user.getEmail());
        vo.setEmailVerified(user.getEmailVerified());
        vo.setSchool(user.getSchool());
        vo.setMajor(user.getMajor());
        vo.setGrade(user.getGrade());
        vo.setBio(user.getBio());
        vo.setGithubUsername(user.getGithubUsername());
        vo.setGithubClaimed(user.getGithubClaimed());
        vo.setGiteeUsername(user.getGiteeUsername());
        vo.setGiteeClaimed(user.getGiteeClaimed());
        vo.setFormalProfileCompleted(user.getFormalProfileCompleted());
        vo.setCreditScore(user.getCreditScore());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setUsername(user.getUsername());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }

    // ==================== Getters & Setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getGithubUsername() { return githubUsername; }
    public void setGithubUsername(String githubUsername) { this.githubUsername = githubUsername; }

    public Boolean getGithubClaimed() { return githubClaimed; }
    public void setGithubClaimed(Boolean githubClaimed) { this.githubClaimed = githubClaimed; }

    public String getGiteeUsername() { return giteeUsername; }
    public void setGiteeUsername(String giteeUsername) { this.giteeUsername = giteeUsername; }

    public Boolean getGiteeClaimed() { return giteeClaimed; }
    public void setGiteeClaimed(Boolean giteeClaimed) { this.giteeClaimed = giteeClaimed; }

    public Boolean getFormalProfileCompleted() { return formalProfileCompleted; }
    public void setFormalProfileCompleted(Boolean formalProfileCompleted) { this.formalProfileCompleted = formalProfileCompleted; }

    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
