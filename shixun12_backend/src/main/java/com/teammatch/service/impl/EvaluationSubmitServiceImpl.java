package com.teammatch.service.impl;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.AnomalyDetectionResult;
import com.teammatch.dto.CreditCalculationResult;
import com.teammatch.dto.EvaluationSubmitDTO;
import com.teammatch.dto.EvaluationSubmitResult;
import com.teammatch.entity.CreditChange;
import com.teammatch.entity.Evaluation;
import com.teammatch.entity.EvaluationTag;
import com.teammatch.exception.BusinessException;
import com.teammatch.mapper.CreditChangeMapper;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.mapper.EvaluationTagMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.AnomalyDetectionService;
import com.teammatch.service.CreditCalculationService;
import com.teammatch.service.EvaluationEligibilityService;
import com.teammatch.service.EvaluationSubmitService;
import com.teammatch.service.EvaluationValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * M5-5 互评提交最小事务编排实现
 * 串联资格校验、内容校验、信誉计算、异常检测，在同一事务内完成
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationSubmitServiceImpl implements EvaluationSubmitService {

    private final EvaluationEligibilityService eligibilityService;

    private final EvaluationValidationService validationService;

    private final CreditCalculationService creditCalculationService;

    private final AnomalyDetectionService anomalyDetectionService;

    private final EvaluationMapper evaluationMapper;

    private final EvaluationTagMapper evaluationTagMapper;

    private final CreditChangeMapper creditChangeMapper;

    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<EvaluationSubmitResult> submit(EvaluationSubmitDTO dto) {
        Long evaluatorId = dto.getEvaluatorId();
        Long targetId = dto.getTargetId();
        Long projectId = dto.getProjectId();
        log.debug("Submit evaluation start evaluatorId={} targetId={} projectId={}",
                evaluatorId, targetId, projectId);

        // Step 1: 资格兜底校验（未落库，失败直接返回）
        Result<Void> eligibilityResult = eligibilityService.validateSubmission(
                evaluatorId, targetId, projectId);
        if (eligibilityResult.isFail()) {
            ReasonCode reasonCode = ReasonCode.fromCode(eligibilityResult.getCode());
            return Result.fail(reasonCode);
        }

        // Step 2: 内容校验（未落库，失败直接返回）
        Result<Void> validationResult = validationService.validateSubmission(dto);
        if (validationResult.isFail()) {
            ReasonCode reasonCode = ReasonCode.fromCode(validationResult.getCode());
            return Result.fail(reasonCode);
        }

        // ====== 以下步骤落库，失败抛异常由 @Transactional 回滚 ======

        // Step 3: 插入 evaluation（捕获唯一键冲突映射为 ALREADY_EVALUATED）
        Evaluation evaluation = buildEvaluation(dto);
        int inserted;
        try {
            inserted = evaluationMapper.insert(evaluation);
        } catch (DuplicateKeyException e) {
            log.warn("Duplicate evaluation insert evaluatorId={} targetId={} projectId={}",
                    evaluatorId, targetId, projectId);
            return Result.fail(ReasonCode.ALREADY_EVALUATED);
        }
        if (inserted != 1) {
            throw new BusinessException(ReasonCode.UNKNOWN_ERROR, "插入评价记录失败");
        }
        Long evaluationId = evaluation.getId();
        if (evaluationId == null) {
            throw new BusinessException(ReasonCode.UNKNOWN_ERROR, "评价记录ID未回填");
        }

        // Step 4: 插入 evaluation_tag
        insertTags(evaluationId, "positive", dto.getPositiveTags());
        insertTags(evaluationId, "negative", dto.getNegativeTags());

        // Step 5: 信誉分计算
        CreditCalculationResult creditResult = creditCalculationService.calculate(evaluation);

        // Step 6: 异常检测（同一事务内，新插入的 evaluation 已被 findByEvaluatorAndProject 查询到）
        AnomalyDetectionResult anomalyResult = anomalyDetectionService.detect(evaluatorId, projectId);

        // Step 7-8: 根据异常检测结果分支处理
        if (anomalyResult.isTriggered()) {
            return handleAnomalyTriggered(evaluation, creditResult, anomalyResult);
        } else {
            return handleNormal(evaluation, creditResult, anomalyResult);
        }
    }

    /**
     * 正常分支：evaluation.status=normal, credit_change.effective=1, 更新 credit_score
     */
    private Result<EvaluationSubmitResult> handleNormal(Evaluation evaluation,
                                                         CreditCalculationResult creditResult,
                                                         AnomalyDetectionResult anomalyResult) {
        CreditChange cc = buildCreditChange(evaluation, creditResult, true);
        int insertedCc = creditChangeMapper.insert(cc);
        if (insertedCc != 1) {
            throw new BusinessException(ReasonCode.UNKNOWN_ERROR, "插入信誉分流水失败");
        }

        updateCreditScoreOrThrow(evaluation.getTargetId(), creditResult.getCappedDelta(),
                "更新用户信誉分失败");

        EvaluationSubmitResult result = new EvaluationSubmitResult();
        result.setEvaluationId(evaluation.getId());
        result.setEvaluationStatus(evaluation.getStatus());
        result.setEffective(true);
        result.setCreditResult(creditResult);
        result.setAnomalyResult(anomalyResult);
        return Result.success(result);
    }

    /**
     * 异常分支：批量 pending_review + 撤销历史已生效流水 + 当前 effective=false
     */
    private Result<EvaluationSubmitResult> handleAnomalyTriggered(Evaluation evaluation,
                                                                    CreditCalculationResult creditResult,
                                                                    AnomalyDetectionResult anomalyResult) {
        List<Long> affectedEvaluationIds = anomalyResult.getAffectedEvaluationIds();
        log.warn("Evaluation anomaly triggered evaluatorId={} projectId={} reason={} affectedEvaluationIds={}",
                evaluation.getEvaluatorId(), evaluation.getProjectId(),
                anomalyResult.getReason(), affectedEvaluationIds);

        if (!affectedEvaluationIds.isEmpty()) {
            // 查询 affected evaluations 中已存在且 effective=1 的 credit_change
            // 当前新建 evaluation 的 credit_change 尚未插入，不会被查到
            List<CreditChange> existingEffectiveChanges = creditChangeMapper
                    .findBySourceTypeAndSourceIds("evaluation", affectedEvaluationIds);

            if (!existingEffectiveChanges.isEmpty()) {
                List<Long> creditChangeIdsToRevert = new ArrayList<>();
                Map<Long, Integer> userDeltaSums = new LinkedHashMap<>();

                for (CreditChange cc : existingEffectiveChanges) {
                    creditChangeIdsToRevert.add(cc.getId());
                    userDeltaSums.merge(cc.getUserId(), -cc.getChangeValue(), Integer::sum);
                }

                // 反向修正已生效的 credit_score
                for (Map.Entry<Long, Integer> entry : userDeltaSums.entrySet()) {
                    updateCreditScoreOrThrow(entry.getKey(), entry.getValue(),
                            "撤销历史信誉分失败，userId=" + entry.getKey());
                }

                // 批量将已生效流水置为 effective=0
                int updatedCc = creditChangeMapper.batchUpdateEffective(creditChangeIdsToRevert, false);
                if (updatedCc != creditChangeIdsToRevert.size()) {
                    throw new BusinessException(ReasonCode.UNKNOWN_ERROR, "批量更新 credit_change.effective 失败");
                }
            }

            // 批量将 affected evaluation 状态改为 pending_review
            int updatedEval = evaluationMapper.batchUpdateStatus(affectedEvaluationIds,
                    Evaluation.STATUS_PENDING_REVIEW);
            if (updatedEval != affectedEvaluationIds.size()) {
                throw new BusinessException(ReasonCode.UNKNOWN_ERROR, "批量更新 evaluation.status 失败");
            }
        }

        // 插入当前 evaluation 的 credit_change，effective=false
        CreditChange cc = buildCreditChange(evaluation, creditResult, false);
        int insertedCc = creditChangeMapper.insert(cc);
        if (insertedCc != 1) {
            throw new BusinessException(ReasonCode.UNKNOWN_ERROR, "插入异常评价信誉分流水失败");
        }

        EvaluationSubmitResult result = new EvaluationSubmitResult();
        result.setEvaluationId(evaluation.getId());
        result.setEvaluationStatus(Evaluation.STATUS_PENDING_REVIEW);
        result.setEffective(false);
        result.setCreditResult(creditResult);
        result.setAnomalyResult(anomalyResult);
        return Result.success(result);
    }

    private Evaluation buildEvaluation(EvaluationSubmitDTO dto) {
        Evaluation evaluation = new Evaluation();
        evaluation.setProjectId(dto.getProjectId());
        evaluation.setEvaluatorId(dto.getEvaluatorId());
        evaluation.setTargetId(dto.getTargetId());
        evaluation.setCommunicationScore(dto.getCommunicationScore());
        evaluation.setTaskScore(dto.getTaskScore());
        evaluation.setSkillScore(dto.getSkillScore());
        evaluation.setResponsibilityScore(dto.getResponsibilityScore());
        evaluation.setComment(dto.getComment());
        evaluation.setStatus(Evaluation.STATUS_NORMAL);

        int total = dto.getCommunicationScore() + dto.getTaskScore()
                + dto.getSkillScore() + dto.getResponsibilityScore();
        BigDecimal avg = BigDecimal.valueOf(total / 4.0).setScale(2, RoundingMode.HALF_UP);
        evaluation.setAverageScore(avg);

        return evaluation;
    }

    private void updateCreditScoreOrThrow(Long userId, int delta, String failureMessage) {
        int updatedScore = userMapper.updateCreditScore(userId, delta);
        if (updatedScore != 1) {
            log.error("Update user credit score failed userId={} delta={} rows={}",
                    userId, delta, updatedScore);
            throw new BusinessException(ReasonCode.UNKNOWN_ERROR, failureMessage);
        }
    }

    private void insertTags(Long evaluationId, String tagType, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (String tagName : tags) {
            EvaluationTag tag = new EvaluationTag();
            tag.setEvaluationId(evaluationId);
            tag.setTagName(tagName);
            tag.setTagType(tagType);
            int inserted = evaluationTagMapper.insert(tag);
            if (inserted != 1) {
                throw new BusinessException(ReasonCode.UNKNOWN_ERROR, "插入评价标签失败");
            }
        }
    }

    private CreditChange buildCreditChange(Evaluation evaluation,
                                           CreditCalculationResult creditResult,
                                           boolean effective) {
        CreditChange cc = new CreditChange();
        cc.setUserId(evaluation.getTargetId());
        cc.setProjectId(evaluation.getProjectId());
        cc.setChangeType("evaluation");
        cc.setChangeValue(creditResult.getCappedDelta());
        cc.setEffective(effective);
        cc.setSourceType("evaluation");
        cc.setSourceId(evaluation.getId());
        int delta = creditResult.getCappedDelta();
        cc.setDescription("项目" + evaluation.getProjectId() + "互评: " + (delta >= 0 ? "+" : "") + delta + "分");
        return cc;
    }
}
