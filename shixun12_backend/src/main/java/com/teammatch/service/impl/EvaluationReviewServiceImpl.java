package com.teammatch.service.impl;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.common.ReviewAction;
import com.teammatch.dto.EvaluationReviewCommand;
import com.teammatch.dto.EvaluationReviewResult;
import com.teammatch.entity.CreditChange;
import com.teammatch.entity.Evaluation;
import com.teammatch.exception.BusinessException;
import com.teammatch.mapper.CreditChangeMapper;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.EvaluationReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * M5-6 评价复核 Service 实现
 * 对 pending_review 状态的评价执行 approve / void / keep_no_credit 复核操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationReviewServiceImpl implements EvaluationReviewService {

    private final EvaluationMapper evaluationMapper;
    private final CreditChangeMapper creditChangeMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<EvaluationReviewResult> review(EvaluationReviewCommand command) {
        // Step 1: 校验 action
        ReviewAction action = ReviewAction.fromString(command.getAction());
        if (action == null) {
            return Result.fail(ReasonCode.INVALID_REVIEW_ACTION);
        }

        // Step 2: 校验 reviewNote 长度（最大 500）
        if (command.getReviewNote() != null && command.getReviewNote().length() > 500) {
            return Result.fail(ReasonCode.REVIEW_NOTE_TOO_LONG);
        }

        // Step 3: 查询 evaluation
        Evaluation evaluation = evaluationMapper.selectById(command.getEvaluationId());
        if (evaluation == null) {
            return Result.fail(ReasonCode.EVALUATION_NOT_FOUND);
        }

        String oldStatus = evaluation.getStatus();

        // Step 4: 检查 evaluation.status 必须是 pending_review
        if (!Evaluation.STATUS_PENDING_REVIEW.equals(oldStatus)) {
            return Result.fail(ReasonCode.STATUS_CONFLICT);
        }

        // Step 5: 查询对应的 suspended credit_change（限定完整条件）
        CreditChange cc = creditChangeMapper.findSuspendedOne(
                "evaluation", command.getEvaluationId(), "evaluation", evaluation.getTargetId());
        if (cc == null || !Boolean.FALSE.equals(cc.getEffective())) {
            return Result.fail(ReasonCode.CREDIT_CHANGE_NOT_FOUND);
        }

        // Step 6: 根据 action 执行分支逻辑
        LocalDateTime now = LocalDateTime.now();
        boolean creditEffectiveChanged = false;

        switch (action) {
            case APPROVE:
                creditEffectiveChanged = executeApprove(command, evaluation, cc, now);
                break;
            case VOID:
                executeVoid(command, evaluation, now);
                break;
            case KEEP_NO_CREDIT:
                executeKeepNoCredit(command, evaluation, cc, now);
                break;
        }

        // Step 7: 构建返回结果
        EvaluationReviewResult result = new EvaluationReviewResult();
        result.setEvaluationId(evaluation.getId());
        result.setOldStatus(oldStatus);
        result.setNewStatus(action == ReviewAction.APPROVE ? Evaluation.STATUS_NORMAL
                : action == ReviewAction.VOID ? Evaluation.STATUS_VOIDED
                : Evaluation.STATUS_KEPT_NO_CREDIT);
        result.setTargetId(evaluation.getTargetId());
        result.setCreditDelta(cc.getChangeValue());
        result.setCreditEffectiveChanged(creditEffectiveChanged);

        return Result.success(result);
    }

    private boolean executeApprove(EvaluationReviewCommand command, Evaluation evaluation,
                                   CreditChange cc, LocalDateTime now) {
        log.info("Review approve evaluationId={} targetId={} creditDelta={}",
                evaluation.getId(), evaluation.getTargetId(), cc.getChangeValue());

        // 条件更新 evaluation: 仅当 status='pending_review' 时生效
        int updatedEval = evaluationMapper.updateReviewIfPending(evaluation.getId(),
                Evaluation.STATUS_NORMAL, command.getReviewerId(), command.getReviewNote(), now);
        if (updatedEval != 1) {
            throw new BusinessException(ReasonCode.STATUS_CONFLICT, "评价状态已变更，无法重复复核");
        }

        // 原子性翻转 credit_change.effective 0→1（完整条件守卫）
        int updatedCc = creditChangeMapper.updateEffectiveToTrue(
                cc.getId(), "evaluation", evaluation.getId(), "evaluation", evaluation.getTargetId());
        if (updatedCc != 1) {
            throw new BusinessException(ReasonCode.STATUS_CONFLICT, "信誉分流水已生效，无法重复操作");
        }

        // 更新 user.credit_score：增量加回 change_value
        updateCreditScoreOrThrow(evaluation.getTargetId(), cc.getChangeValue(), "更新用户信誉分失败");

        return true;
    }

    private void executeVoid(EvaluationReviewCommand command, Evaluation evaluation,
                             LocalDateTime now) {
        log.info("Review void evaluationId={}", evaluation.getId());

        int updatedEval = evaluationMapper.updateReviewIfPending(evaluation.getId(),
                Evaluation.STATUS_VOIDED, command.getReviewerId(), command.getReviewNote(), now);
        if (updatedEval != 1) {
            throw new BusinessException(ReasonCode.STATUS_CONFLICT, "评价状态已变更，无法重复复核");
        }
    }

    private void executeKeepNoCredit(EvaluationReviewCommand command, Evaluation evaluation,
                                      CreditChange cc, LocalDateTime now) {
        log.info("Review keep_no_credit evaluationId={}", evaluation.getId());

        int updatedEval = evaluationMapper.updateReviewIfPending(evaluation.getId(),
                Evaluation.STATUS_KEPT_NO_CREDIT, command.getReviewerId(), command.getReviewNote(), now);
        if (updatedEval != 1) {
            throw new BusinessException(ReasonCode.STATUS_CONFLICT, "评价状态已变更，无法重复复核");
        }

        String desc = "项目互评信誉变化 [复核: 保留但不计分" +
                (command.getReviewNote() != null && !command.getReviewNote().isEmpty()
                        ? ", 备注: " + command.getReviewNote() : "") + "]";
        int updatedCc = creditChangeMapper.updateDescription(cc.getId(), desc);
        if (updatedCc != 1) {
            throw new BusinessException(ReasonCode.UNKNOWN_ERROR, "更新信誉流水描述失败");
        }
    }

    private void updateCreditScoreOrThrow(Long userId, int delta, String failureMessage) {
        int updatedScore = userMapper.updateCreditScore(userId, delta);
        if (updatedScore != 1) {
            log.error("Update user credit score failed userId={} delta={} rows={}",
                    userId, delta, updatedScore);
            throw new BusinessException(ReasonCode.UNKNOWN_ERROR, failureMessage);
        }
    }
}
