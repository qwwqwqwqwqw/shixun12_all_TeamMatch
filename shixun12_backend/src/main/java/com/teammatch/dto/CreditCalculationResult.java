package com.teammatch.dto;

import java.math.BigDecimal;

/**
 * M5-3 信誉分计算结果 DTO
 * 封装互评信誉分计算的完整过程和结果
 */
public class CreditCalculationResult {

    /** 四维评分平均值 */
    private BigDecimal averageScore;

    /** 根据 V2.1 八区间映射得到的原始 delta */
    private Integer rawDelta;

    /** 用户在当前项目已有的 evaluation 类 effective credit_change 累计值 */
    private Integer existingProjectDelta;

    /** 应用单项目 ±10 封顶后的实际 delta */
    private Integer cappedDelta;

    /** 应用本次 delta 后的项目累计值 */
    private Integer projectTotalAfterApplied;

    /** 是否触发了封顶 */
    private Boolean capped;

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    public Integer getRawDelta() {
        return rawDelta;
    }

    public void setRawDelta(Integer rawDelta) {
        this.rawDelta = rawDelta;
    }

    public Integer getExistingProjectDelta() {
        return existingProjectDelta;
    }

    public void setExistingProjectDelta(Integer existingProjectDelta) {
        this.existingProjectDelta = existingProjectDelta;
    }

    public Integer getCappedDelta() {
        return cappedDelta;
    }

    public void setCappedDelta(Integer cappedDelta) {
        this.cappedDelta = cappedDelta;
    }

    public Integer getProjectTotalAfterApplied() {
        return projectTotalAfterApplied;
    }

    public void setProjectTotalAfterApplied(Integer projectTotalAfterApplied) {
        this.projectTotalAfterApplied = projectTotalAfterApplied;
    }

    public Boolean getCapped() {
        return capped;
    }

    public void setCapped(Boolean capped) {
        this.capped = capped;
    }
}
