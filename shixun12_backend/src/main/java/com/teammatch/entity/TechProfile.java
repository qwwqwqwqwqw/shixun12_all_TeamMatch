package com.teammatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 技术画像实体类
 * 对应数据库表：tech_profile
 *
 * 用于存储通过 GitHub API 分析得到的用户技术画像数据，
 * 是冷启动排行榜的数据源。
 */
@Data
@TableName("tech_profile")
public class TechProfile {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** GitHub 用户名 */
    private String githubUsername;

    /** 数据来源：github / gitee */
    private String source;

    /** 认领该画像的用户 ID（NULL 表示未认领） */
    private Long claimedByUserId;

    /** 所有仓库的 Star 总数 */
    private Integer totalStars;

    /** 公开仓库数量 */
    private Integer totalRepos;

    /** 总提交数（估算值） */
    private Integer totalCommits;

    /** Pull Request 总数 */
    private Integer totalPrs;

    /** 最近一年贡献数 */
    private Integer totalContributions;

    /** 主力编程语言，JSON 数组字符串 */
    private String topLanguages;

    /** 综合技术评分 */
    private Integer techScore;

    /** GitHub 个人简介 */
    private String bio;

    /** GitHub 头像 URL */
    private String avatarUrl;

    /** 同步状态: pending 待同步 / synced 已同步 / failed 同步失败 */
    private String syncStatus;

    /** 最后一次从 GitHub 同步数据的时间 */
    private LocalDateTime lastSyncedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    // ==================== 业务方法 ====================

    /**
     * 计算综合技术评分
     * 公式：stars×10 + commits×2 + prs×5 + repos×3 + contributions×1
     */
    public void computeTechScore() {
        int stars = this.totalStars != null ? this.totalStars : 0;
        int commits = this.totalCommits != null ? this.totalCommits : 0;
        int prs = this.totalPrs != null ? this.totalPrs : 0;
        int repos = this.totalRepos != null ? this.totalRepos : 0;
        int contributions = this.totalContributions != null ? this.totalContributions : 0;

        this.techScore = stars * 10 + commits * 2 + prs * 5 + repos * 3 + contributions * 1;
    }

    /**
     * 判断该技术画像是否已被认领
     */
    public boolean isClaimed() {
        return this.claimedByUserId != null;
    }
}