package com.teammatch.dto;

import com.teammatch.entity.TechProfile;

import java.time.LocalDateTime;

/**
 * 技术画像响应 VO
 * 用于 GET /api/profile/tech-profile 接口返回
 */
public class TechProfileVO {

    private Long id;
    private String githubUsername;
    private String source;
    private Long claimedByUserId;
    private String claimedByUserNickname;
    private Integer totalStars;
    private Integer totalRepos;
    private Integer totalCommits;
    private Integer totalPrs;
    private Integer totalContributions;
    private String topLanguages;
    private Integer techScore;
    private String bio;
    private String avatarUrl;
    private LocalDateTime lastSyncedAt;
    private Boolean claimed;

    /**
     * 从 TechProfile 实体转换为 VO
     */
    public static TechProfileVO from(TechProfile profile) {
        if (profile == null) return null;
        TechProfileVO vo = new TechProfileVO();
        vo.setId(profile.getId());
        vo.setGithubUsername(profile.getGithubUsername());
        vo.setSource(profile.getSource());
        vo.setClaimedByUserId(profile.getClaimedByUserId());
        vo.setTotalStars(profile.getTotalStars());
        vo.setTotalRepos(profile.getTotalRepos());
        vo.setTotalCommits(profile.getTotalCommits());
        vo.setTotalPrs(profile.getTotalPrs());
        vo.setTotalContributions(profile.getTotalContributions());
        vo.setTopLanguages(profile.getTopLanguages());
        vo.setTechScore(profile.getTechScore());
        vo.setBio(profile.getBio());
        vo.setAvatarUrl(profile.getAvatarUrl());
        vo.setLastSyncedAt(profile.getLastSyncedAt());
        vo.setClaimed(profile.isClaimed());
        return vo;
    }

    // ==================== Getters & Setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGithubUsername() { return githubUsername; }
    public void setGithubUsername(String githubUsername) { this.githubUsername = githubUsername; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Long getClaimedByUserId() { return claimedByUserId; }
    public void setClaimedByUserId(Long claimedByUserId) { this.claimedByUserId = claimedByUserId; }

    public String getClaimedByUserNickname() { return claimedByUserNickname; }
    public void setClaimedByUserNickname(String claimedByUserNickname) { this.claimedByUserNickname = claimedByUserNickname; }

    public Integer getTotalStars() { return totalStars; }
    public void setTotalStars(Integer totalStars) { this.totalStars = totalStars; }

    public Integer getTotalRepos() { return totalRepos; }
    public void setTotalRepos(Integer totalRepos) { this.totalRepos = totalRepos; }

    public Integer getTotalCommits() { return totalCommits; }
    public void setTotalCommits(Integer totalCommits) { this.totalCommits = totalCommits; }

    public Integer getTotalPrs() { return totalPrs; }
    public void setTotalPrs(Integer totalPrs) { this.totalPrs = totalPrs; }

    public Integer getTotalContributions() { return totalContributions; }
    public void setTotalContributions(Integer totalContributions) { this.totalContributions = totalContributions; }

    public String getTopLanguages() { return topLanguages; }
    public void setTopLanguages(String topLanguages) { this.topLanguages = topLanguages; }

    public Integer getTechScore() { return techScore; }
    public void setTechScore(Integer techScore) { this.techScore = techScore; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public Boolean getClaimed() { return claimed; }
    public void setClaimed(Boolean claimed) { this.claimed = claimed; }
}