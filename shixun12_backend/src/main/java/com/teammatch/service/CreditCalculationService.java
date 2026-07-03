package com.teammatch.service;

import com.teammatch.dto.CreditCalculationResult;
import com.teammatch.entity.Evaluation;

/**
 * M5-3 信誉分计算编排服务
 * 负责协调 CreditRuleService 完成互评信誉分的完整计算流程
 */
public interface CreditCalculationService {

    /**
     * 计算互评信誉分变化
     * 输入：已通过资格和内容校验的互评对象
     * 输出：结构化计算结果（包含 averageScore、rawDelta、existingProjectDelta、cappedDelta、projectTotalAfterApplied、capped）
     *
     * 本方法不落库、不写 credit_change、不更新 user.credit_score
     */
    CreditCalculationResult calculate(Evaluation evaluation);
}
