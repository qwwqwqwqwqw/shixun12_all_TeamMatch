package com.teammatch.dto;

import java.io.Serializable;

/**
 * 可评价成员 DTO
 * 用于 M5-1B 待评价成员列表
 */
public class EvaluatableMemberDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像 URL
     */
    private String avatarUrl;

    /**
     * 是否已评价
     * true - 当前用户已经评价过该成员
     * false - 当前用户尚未评价该成员
     */
    private Boolean evaluated;

    // ==================== 构造方法 ====================

    public EvaluatableMemberDTO() {
    }

    public EvaluatableMemberDTO(Long userId, String nickname, String avatarUrl, Boolean evaluated) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.evaluated = evaluated;
    }

    // ==================== Getter/Setter ====================

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Boolean getEvaluated() {
        return evaluated;
    }

    public void setEvaluated(Boolean evaluated) {
        this.evaluated = evaluated;
    }

    @Override
    public String toString() {
        return "EvaluatableMemberDTO{" +
                "userId=" + userId +
                ", nickname='" + nickname + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", evaluated=" + evaluated +
                '}';
    }
}