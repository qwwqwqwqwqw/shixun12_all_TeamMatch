package com.teammatch.dto;

/**
 * 用户端信誉分响应 VO。
 */
public class CreditScoreVO {
    private Long userId;
    private Integer creditScore;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }
}
