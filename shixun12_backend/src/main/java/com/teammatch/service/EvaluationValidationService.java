package com.teammatch.service;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.config.ValidationConfig;
import com.teammatch.dto.EvaluationSubmitDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 互评内容校验服务
 * 实现 M5-2 互评提交内容基础合法性校验
 */
@Service
@RequiredArgsConstructor
public class EvaluationValidationService {

    private final ValidationConfig validationConfig;

    /**
     * 校验互评提交内容的合法性
     * 实现 M5-2 的 7 条校验规则
     *
     * @param dto 互评提交 DTO
     * @return Result<Void> - 成功返回 success，失败返回对应错误码
     */
    public Result<Void> validateSubmission(EvaluationSubmitDTO dto) {
        // 规则 1: 检查四个评分是否都存在
        Result<Void> scoreExistenceCheck = checkScoreExistence(dto);
        if (scoreExistenceCheck.isFail()) {
            return scoreExistenceCheck;
        }

        // 规则 2: 检查四个评分是否都在 1-5 之间
        Result<Void> scoreRangeCheck = checkScoreRange(dto);
        if (scoreRangeCheck.isFail()) {
            return scoreRangeCheck;
        }

        // 规则 3: 检查 comment 是否包含违规词
        Result<Void> commentViolationCheck = checkCommentViolation(dto);
        if (commentViolationCheck.isFail()) {
            return commentViolationCheck;
        }

        // 规则 4: 检查高分（5分）是否有合法 positive tag
        Result<Void> highScoreTagCheck = checkHighScorePositiveTag(dto);
        if (highScoreTagCheck.isFail()) {
            return highScoreTagCheck;
        }

        // 规则 5: 检查低分（1-2分）是否有合法 negative tag
        Result<Void> lowScoreTagCheck = checkLowScoreNegativeTag(dto);
        if (lowScoreTagCheck.isFail()) {
            return lowScoreTagCheck;
        }

        // 规则 6: 检查低分（1-2分）comment 是否不少于 20 字
        Result<Void> lowScoreCommentCheck = checkLowScoreComment(dto);
        if (lowScoreCommentCheck.isFail()) {
            return lowScoreCommentCheck;
        }

        // 规则 7: 低分不能只有正向标签（由规则 5 自然覆盖，不单独实现）

        // 所有校验通过
        return Result.success();
    }

    /**
     * 规则 1: 检查四个评分是否都存在
     */
    private Result<Void> checkScoreExistence(EvaluationSubmitDTO dto) {
        if (dto.getCommunicationScore() == null ||
            dto.getTaskScore() == null ||
            dto.getSkillScore() == null ||
            dto.getResponsibilityScore() == null) {
            return Result.fail(ReasonCode.SCORE_FIELD_MISSING);
        }
        return Result.success();
    }

    /**
     * 规则 2: 检查四个评分是否都在 1-5 之间
     */
    private Result<Void> checkScoreRange(EvaluationSubmitDTO dto) {
        if (!isScoreInRange(dto.getCommunicationScore()) ||
            !isScoreInRange(dto.getTaskScore()) ||
            !isScoreInRange(dto.getSkillScore()) ||
            !isScoreInRange(dto.getResponsibilityScore())) {
            return Result.fail(ReasonCode.SCORE_OUT_OF_RANGE);
        }
        return Result.success();
    }

    /**
     * 检查单个评分是否在 1-5 之间
     */
    private boolean isScoreInRange(Integer score) {
        return score != null && score >= 1 && score <= 5;
    }

    /**
     * 规则 3: 检查 comment 是否包含违规词
     */
    private Result<Void> checkCommentViolation(EvaluationSubmitDTO dto) {
        String comment = dto.getComment();
        if (comment != null && !comment.isEmpty() && validationConfig.containsViolation(comment)) {
            return Result.fail(ReasonCode.COMMENT_CONTAINS_VIOLATION);
        }
        return Result.success();
    }

    /**
     * 规则 4: 检查高分（5分）是否有合法 positive tag
     * 只要出现 5 分，就必须有至少一个 P0 正向标签，且不能混入未知标签
     */
    private Result<Void> checkHighScorePositiveTag(EvaluationSubmitDTO dto) {
        boolean hasHighScore = hasScore(dto, 5);
        List<String> positiveTags = dto.getPositiveTags();
        if (positiveTags != null && !positiveTags.isEmpty()
                && !validationConfig.allPositiveTagsValid(positiveTags)) {
            return Result.fail(ReasonCode.HIGH_SCORE_MISSING_POSITIVE_TAG);
        }

        if (hasHighScore) {
            if (positiveTags == null || positiveTags.isEmpty()
                    || !validationConfig.containsAnyValidPositiveTag(positiveTags)) {
                return Result.fail(ReasonCode.HIGH_SCORE_MISSING_POSITIVE_TAG);
            }
        }
        return Result.success();
    }

    /**
     * 规则 5: 检查低分（1-2分）是否有合法 negative tag
     * 只要出现 1 分或 2 分，就必须有至少一个 P0 负向标签，且不能混入未知标签
     */
    private Result<Void> checkLowScoreNegativeTag(EvaluationSubmitDTO dto) {
        boolean hasLowScore = hasScore(dto, 1) || hasScore(dto, 2);
        List<String> negativeTags = dto.getNegativeTags();
        if (negativeTags != null && !negativeTags.isEmpty()
                && !validationConfig.allNegativeTagsValid(negativeTags)) {
            return Result.fail(ReasonCode.LOW_SCORE_MISSING_NEGATIVE_TAG);
        }

        if (hasLowScore) {
            if (negativeTags == null || negativeTags.isEmpty()
                    || !validationConfig.containsAnyValidNegativeTag(negativeTags)) {
                return Result.fail(ReasonCode.LOW_SCORE_MISSING_NEGATIVE_TAG);
            }
        }
        return Result.success();
    }

    /**
     * 规则 6: 检查低分（1-2分）comment 是否不少于 20 字
     * 只要出现 1 分或 2 分，comment.trim() 必须不少于 20 字
     */
    private Result<Void> checkLowScoreComment(EvaluationSubmitDTO dto) {
        boolean hasLowScore = hasScore(dto, 1) || hasScore(dto, 2);
        if (hasLowScore) {
            String comment = dto.getComment();
            if (comment == null || comment.trim().length() < 20) {
                return Result.fail(ReasonCode.LOW_SCORE_COMMENT_TOO_SHORT);
            }
        }
        return Result.success();
    }

    /**
     * 检查四个评分中是否有指定分数
     */
    private boolean hasScore(EvaluationSubmitDTO dto, int targetScore) {
        return dto.getCommunicationScore() == targetScore ||
               dto.getTaskScore() == targetScore ||
               dto.getSkillScore() == targetScore ||
               dto.getResponsibilityScore() == targetScore;
    }
}
