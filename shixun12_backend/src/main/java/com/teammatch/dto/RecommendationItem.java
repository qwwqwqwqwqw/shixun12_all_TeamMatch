package com.teammatch.dto;

import lombok.Data;

import java.util.List;

/**
 * 推荐结果 VO
 * 透传给前端的诊断数据,卡片直接展示:
 *   [技能匹配 85% (Java, Vue)]  [3次互评认证 | 加权信誉94]  综合 0.56
 */
@Data
public class RecommendationItem {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer creditScore;
    private Integer techScore;

    private Double jaccardSimilarity;
    private Double techAuthority;
    private Double trustFactor;
    private Double finalScore;

    private List<String> matchedSkills;
    private Boolean passedHardFilter;
    /** 如 "3次互评认证,加权信誉94" / "暂无互评数据" */
    private String authorityBreakdown;
}
