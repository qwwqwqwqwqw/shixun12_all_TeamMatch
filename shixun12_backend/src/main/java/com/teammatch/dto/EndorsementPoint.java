package com.teammatch.dto;

import lombok.Data;

/**
 * 互评技术维度分加权点
 * 由 EvaluationMapper.findSkillScoresByTargets 批量查回,前端不感知,只在推荐精排层流转。
 */
@Data
public class EndorsementPoint {
    /** 互评技术维度分(1-5) */
    private Integer skillScore;

    /** 评价者的信誉分(用作权重) */
    private Integer evaluatorCreditScore;

    /** 评价者用户 ID(诊断用) */
    private Long evaluatorId;

    /** 被评价人用户 ID(批量查询后按此分组) */
    private Long targetUserId;
}
