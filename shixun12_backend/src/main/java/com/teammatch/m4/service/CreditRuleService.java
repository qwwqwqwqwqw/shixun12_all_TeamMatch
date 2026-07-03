package com.teammatch.m4.service;

import java.util.List;
import com.teammatch.entity.Evaluation;

public interface CreditRuleService {

    /**
     * T-088 评分到 delta 映射服务
     * 将四维评分映射为最终的信誉分增减 (-5 ~ +5)
     */
    int calculateDeltaFromEvaluation(Evaluation evaluation);

    /**
     * T-089 单项目 ±10 封顶服务
     * 校验本次变动加上当前项目已有的信誉变动，是否超出了 ±10 分的封顶限制，并返回截断后的有效信誉分变动
     */
    int calculateCappedDelta(Long userId, Long projectId, int proposedDelta);

    /**
     * M5-3 平均分到 delta 映射（V2.1 八区间）
     * 将四维评分平均值映射为信誉分增减 (-5 ~ +5)
     */
    int mapAverageScoreToDelta(double avg);

    /**
     * M5-3 查询用户在指定项目的已有 evaluation 类 effective credit_change 累计值
     */
    int getExistingProjectDelta(Long userId, Long projectId);

    /**
     * M5-3 基于已有项目累计值做封顶截断，纯函数不查 DB
     * @param existingProjectDelta 已生效的 evaluation 类 credit_change 累计值
     * @param proposedDelta 本次待应用的 rawDelta
     * @return 截断后的有效 delta
     */
    int calculateCappedDeltaFromExisting(int existingProjectDelta, int proposedDelta);
}
