package com.teammatch.service;

import com.teammatch.common.Result;
import com.teammatch.dto.EvaluationReviewCommand;
import com.teammatch.dto.EvaluationReviewResult;

/**
 * M5-6 评价复核 Service
 * 对 pending_review 状态的评价执行 approve / void / keep_no_credit 复核操作
 */
public interface EvaluationReviewService {

    /**
     * 执行评价复核
     *
     * @param command 复核命令（evaluationId, reviewerId, action, reviewNote）
     * @return 复核结果（含状态变化、信誉分变化信息）
     */
    Result<EvaluationReviewResult> review(EvaluationReviewCommand command);
}
