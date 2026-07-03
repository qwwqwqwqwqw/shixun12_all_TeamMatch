package com.teammatch.service;

import com.teammatch.dto.EndorsementPoint;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 技术能力可信度计算服务
 *
 * <p>核心思想:互评里的技术维度分(skill_score)被评价者信誉分(credit_score)加权,
 * 作为该用户"技术能力"的真实信号。新用户没有互评数据,用 tech_profile 先验顶住,
 * 互评积累后先验自动退场(贝叶斯混合)。
 *
 * <p>当前实现:skill_score 是四维评价里的整体技术能力评分(不分 Java/Python),
 * 所有命中技能共享同一个权威分。
 */
@Service
public class SkillAuthorityService {

    /** 先验等价互评权重:越大,先验对最终分影响越持久 */
    private static final double PRIOR_EQUIVALENT_WEIGHT = 5.0;

    /** 评价者信誉底线:低于此分的评价者被丢弃(防小号刷分) */
    private static final int MIN_EVALUATOR_CREDIT = 30;

    /** 完全无数据时的兜底先验 */
    private static final double DEFAULT_PRIOR = 0.3;

    /**
     * 计算用户的技术能力可信度
     *
     * @param points              该用户收到的所有互评技术维度打分(调用方批量查好传入,避免 N+1)
     * @param overlappingSkillIds 与项目需求重叠的技能 ID 列表(仅用于判定是否冷启动)
     * @param techPriorNormalized techScore 归一化值 [0,1];null 表示无 GitHub 数据
     * @return [0,1] 区间的可信度分数
     */
    public double computeAuthority(List<EndorsementPoint> points,
                                    List<Long> overlappingSkillIds,
                                    Double techPriorNormalized) {
        if (overlappingSkillIds == null || overlappingSkillIds.isEmpty()) {
            return DEFAULT_PRIOR;
        }
        return computeSingle(points, techPriorNormalized);
    }

    /**
     * 公开此方法供外部复用(如 buildBreakdown 诊断文案)
     */
    public double computeSingle(List<EndorsementPoint> points, Double techPrior) {
        if (points == null) points = Collections.emptyList();

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (EndorsementPoint p : points) {
            if (p.getEvaluatorCreditScore() == null
                    || p.getSkillScore() == null
                    || p.getEvaluatorCreditScore() < MIN_EVALUATOR_CREDIT) {
                continue;
            }
            double weight = p.getEvaluatorCreditScore() / 100.0;
            weightedSum += weight * (p.getSkillScore() / 5.0);
            totalWeight += weight;
        }

        if (totalWeight == 0.0) {
            return techPrior != null ? techPrior : DEFAULT_PRIOR;
        }

        if (techPrior != null) {
            // 贝叶斯混合:互评越多,先验权重相对越低
            return (weightedSum + techPrior * PRIOR_EQUIVALENT_WEIGHT)
                    / (totalWeight + PRIOR_EQUIVALENT_WEIGHT);
        }
        return weightedSum / totalWeight;
    }
}
