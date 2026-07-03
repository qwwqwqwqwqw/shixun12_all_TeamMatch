package com.teammatch.dto;

/**
 * 排行榜条目 VO
 * 用于 GET /api/leaderboard 接口返回
 */
public class LeaderboardEntryVO {

    private Integer rank;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String school;
    private String githubUsername;
    private Integer techScore;
    private Integer totalStars;
    private Integer totalRepos;
    private Integer totalCommits;
    private Integer totalPrs;
    private Integer totalContributions;
    private String topLanguages;
    private String bio;
    private String source;
    private Boolean claimed;

    // ==================== Getters & Setters ====================

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getGithubUsername() { return githubUsername; }
    public void setGithubUsername(String githubUsername) { this.githubUsername = githubUsername; }

    public Integer getTechScore() { return techScore; }
    public void setTechScore(Integer techScore) { this.techScore = techScore; }

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

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Boolean getClaimed() { return claimed; }
    public void setClaimed(Boolean claimed) { this.claimed = claimed; }
}