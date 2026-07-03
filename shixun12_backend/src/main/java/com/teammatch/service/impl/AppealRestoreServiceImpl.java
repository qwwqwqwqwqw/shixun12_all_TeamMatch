package com.teammatch.service.impl;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.AppealRestoreCommand;
import com.teammatch.dto.AppealRestoreResult;
import com.teammatch.entity.Appeal;
import com.teammatch.entity.CreditChange;
import com.teammatch.entity.Evaluation;
import com.teammatch.exception.BusinessException;
import com.teammatch.mapper.AppealMapper;
import com.teammatch.mapper.CreditChangeMapper;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.AppealRestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * M5-7 申诉恢复 Service 实现
 * 对已批准的 evaluation 类申诉执行信誉恢复：
 * 追加 appeal_restore 流水并回写 user.credit_score
 * <p>
 * function_limit 申诉恢复由 M6 单独处理，本服务不做
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppealRestoreServiceImpl implements AppealRestoreService {

    private static final String TARGET_TYPE_EVALUATION = "evaluation";
    private static final String STATUS_APPROVED = "approved";
    private static final String CHANGE_TYPE_APPEAL_RESTORE = "appeal_restore";

    private final AppealMapper appealMapper;
    private final EvaluationMapper evaluationMapper;
    private final CreditChangeMapper creditChangeMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<AppealRestoreResult> restore(AppealRestoreCommand command) {

        if (command == null || command.getAppealId() == null) {
            return Result.fail(ReasonCode.PARAM_ERROR);
        }

        Long appealId = command.getAppealId();

        // 步骤 1: 查询申诉
        Appeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            return Result.fail(ReasonCode.APPEAL_NOT_FOUND);
        }

        // 步骤 2: 校验申诉已批准
        if (!STATUS_APPROVED.equals(appeal.getStatus())) {
            return Result.fail(ReasonCode.APPEAL_NOT_APPROVED);
        }

        // 步骤 3: 仅处理 evaluation 类型
        if (!TARGET_TYPE_EVALUATION.equals(appeal.getTargetType())) {
            return Result.fail(ReasonCode.INVALID_APPEAL_TARGET_TYPE);
        }

        // 步骤 4: SELECT FOR UPDATE 锁定 evaluation（V2.1 8.7 D-11/D-13）
        Evaluation evaluation = evaluationMapper.selectByIdForUpdate(appeal.getTargetId());
        if (evaluation == null) {
            return Result.fail(ReasonCode.EVALUATION_NOT_FOUND);
        }

        // userId 一致性校验：申诉用户必须是评价的被评价人
        if (!Objects.equals(appeal.getUserId(), evaluation.getTargetId())) {
            log.warn("Appeal user mismatch: appealId={} appeal.userId={} evaluationId={} evaluation.targetId={}",
                    appealId, appeal.getUserId(), evaluation.getId(), evaluation.getTargetId());
            return Result.fail(ReasonCode.PARAM_ERROR);
        }

        // voided 评价已判无效，直接拒绝
        // kept_no_credit / pending_review 评价原流水 effective=0，在步骤 6 走 no-op success，不视为无效评价
        if (Evaluation.STATUS_VOIDED.equals(evaluation.getStatus())) {
            return Result.fail(ReasonCode.EVALUATION_ALREADY_INVALIDATED);
        }

        // 步骤 5: 查询原评价信用流水
        CreditChange original = creditChangeMapper.findBySourceTypeAndSourceIdAndChangeType(
                TARGET_TYPE_EVALUATION, evaluation.getId(), TARGET_TYPE_EVALUATION, appeal.getUserId());
        if (original == null) {
            return Result.fail(ReasonCode.CREDIT_CHANGE_NOT_FOUND);
        }

        AppealRestoreResult result = new AppealRestoreResult();
        result.setAppealId(appealId);
        result.setEvaluationId(evaluation.getId());
        result.setTargetUserId(appeal.getUserId());
        result.setOriginalChangeValue(original.getChangeValue());

        // 步骤 6: 原流水 effective=0 → 跳过恢复（no-op success）
        if (Boolean.FALSE.equals(original.getEffective())) {
            log.info("Appeal restore skipped: original credit_change effective=0, appealId={}", appealId);
            result.setSkipped(true);
            result.setSkipReason("原评价流水未生效");
            return Result.success(result);
        }

        // 步骤 7: 幂等检查
        CreditChange existing = creditChangeMapper.findAppealRestoreExists(
                TARGET_TYPE_EVALUATION, evaluation.getId(), CHANGE_TYPE_APPEAL_RESTORE, appeal.getUserId());
        if (existing != null) {
            log.info("Appeal restore already executed: appealId={} existingCcId={}", appealId, existing.getId());
            result.setAlreadyRestored(true);
            result.setRestoreValue(existing.getChangeValue());
            return Result.success(result);
        }

        // 步骤 8: 插入 appeal_restore 流水
        int restoreValue = -original.getChangeValue();
        CreditChange restore = new CreditChange();
        restore.setUserId(appeal.getUserId());
        restore.setProjectId(evaluation.getProjectId());
        restore.setChangeType(CHANGE_TYPE_APPEAL_RESTORE);
        restore.setChangeValue(restoreValue);
        restore.setEffective(true);
        restore.setSourceType(TARGET_TYPE_EVALUATION);
        restore.setSourceId(evaluation.getId());
        restore.setDescription("原评价流水#" + original.getId() + "扣分撤回");
        int inserted = creditChangeMapper.insert(restore);
        if (inserted != 1) {
            throw new BusinessException(ReasonCode.UNKNOWN_ERROR, "插入appeal_restore流水失败");
        }

        // 步骤 9: 更新 user.credit_score
        int rows = userMapper.updateCreditScore(appeal.getUserId(), restoreValue);
        if (rows != 1) {
            log.error("Update user credit_score failed: userId={} delta={} rows={}",
                    appeal.getUserId(), restoreValue, rows);
            throw new BusinessException(ReasonCode.UNKNOWN_ERROR, "更新用户信誉分失败");
        }

        result.setRestoreValue(restoreValue);
        log.info("Appeal restore completed: appealId={} evaluationId={} userId={} originalValue={} restoreValue={}",
                appealId, evaluation.getId(), appeal.getUserId(), original.getChangeValue(), restoreValue);
        return Result.success(result);
    }
}
