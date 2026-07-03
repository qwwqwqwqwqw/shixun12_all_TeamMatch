package com.teammatch.m6.dto;

import java.io.Serializable;

/**
 * 评价复核请求 DTO（管理员端）。
 *
 * 不包含 reviewerId — 管理员身份从 token 解析注入。
 * 不包含 evaluationId — 从 URL 路径变量获取。
 */
public class EvaluationReviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String action;

    private String reviewNote;

    public EvaluationReviewRequest() {
    }

    public EvaluationReviewRequest(String action, String reviewNote) {
        this.action = action;
        this.reviewNote = reviewNote;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }
}
