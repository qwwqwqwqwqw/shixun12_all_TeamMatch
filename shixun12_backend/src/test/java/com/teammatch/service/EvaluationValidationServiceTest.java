package com.teammatch.service;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.config.ValidationConfig;
import com.teammatch.dto.EvaluationSubmitDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * EvaluationValidationService 单元测试
 * 测试 M5-2 互评提交内容基础合法性校验的 7 条规则
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("互评内容校验服务测试")
class EvaluationValidationServiceTest {

    // ==================== Mock 对象 ====================

    @Spy
    private ValidationConfig validationConfig = new ValidationConfig();

    @InjectMocks
    private EvaluationValidationService service;

    // ==================== 测试数据 ====================

    private EvaluationSubmitDTO validDto;

    @BeforeEach
    void setUp() {
        // 准备一个完全合法的 DTO，用于正向测试
        validDto = new EvaluationSubmitDTO();
        validDto.setEvaluatorId(1L);
        validDto.setTargetId(2L);
        validDto.setProjectId(100L);
        validDto.setCommunicationScore(4);
        validDto.setTaskScore(4);
        validDto.setSkillScore(4);
        validDto.setResponsibilityScore(4);
        validDto.setComment("合作愉快，技术能力强");
        validDto.setPositiveTags(Arrays.asList("沟通积极", "技术可靠"));
        validDto.setNegativeTags(Collections.emptyList());
    }

    // ==================== 规则 1: 评分字段缺失 ====================

    @Test
    @DisplayName("规则1 - 评分字段缺失 - communicationScore 为 null")
    void testValidateSubmission_ScoreFieldMissing_CommunicationScore() {
        // Given
        validDto.setCommunicationScore(null);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SCORE_FIELD_MISSING.getCode());
    }

    @Test
    @DisplayName("规则1 - 评分字段缺失 - taskScore 为 null")
    void testValidateSubmission_ScoreFieldMissing_TaskScore() {
        // Given
        validDto.setTaskScore(null);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SCORE_FIELD_MISSING.getCode());
    }

    @Test
    @DisplayName("规则1 - 评分字段缺失 - skillScore 为 null")
    void testValidateSubmission_ScoreFieldMissing_SkillScore() {
        // Given
        validDto.setSkillScore(null);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SCORE_FIELD_MISSING.getCode());
    }

    @Test
    @DisplayName("规则1 - 评分字段缺失 - responsibilityScore 为 null")
    void testValidateSubmission_ScoreFieldMissing_ResponsibilityScore() {
        // Given
        validDto.setResponsibilityScore(null);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SCORE_FIELD_MISSING.getCode());
    }

    // ==================== 规则 2: 评分超出范围 ====================

    @Test
    @DisplayName("规则2 - 评分超出范围 - communicationScore 为 0")
    void testValidateSubmission_ScoreOutOfRange_Zero() {
        // Given
        validDto.setCommunicationScore(0);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SCORE_OUT_OF_RANGE.getCode());
    }

    @Test
    @DisplayName("规则2 - 评分超出范围 - taskScore 为 6")
    void testValidateSubmission_ScoreOutOfRange_Six() {
        // Given
        validDto.setTaskScore(6);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SCORE_OUT_OF_RANGE.getCode());
    }

    @Test
    @DisplayName("规则2 - 评分超出范围 - skillScore 为 -1")
    void testValidateSubmission_ScoreOutOfRange_Negative() {
        // Given
        validDto.setSkillScore(-1);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.SCORE_OUT_OF_RANGE.getCode());
    }

    @Test
    @DisplayName("规则2 - 评分范围正常 - 边界值 1")
    void testValidateSubmission_ScoreInRange_BoundaryOne() {
        // Given
        validDto.setCommunicationScore(1);
        validDto.setTaskScore(1);
        validDto.setSkillScore(1);
        validDto.setResponsibilityScore(1);
        validDto.setNegativeTags(Arrays.asList("沟通差"));
        validDto.setComment("沟通协作严重不足，需要持续改进任务反馈节奏");

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("规则2 - 评分范围正常 - 边界值 5")
    void testValidateSubmission_ScoreInRange_BoundaryFive() {
        // Given
        validDto.setCommunicationScore(5);
        validDto.setTaskScore(5);
        validDto.setSkillScore(5);
        validDto.setResponsibilityScore(5);
        validDto.setPositiveTags(Arrays.asList("责任心强"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    // ==================== 规则 3: 评论包含违规词 ====================

    @Test
    @DisplayName("规则3 - 评论包含违规词")
    void testValidateSubmission_CommentContainsViolation() {
        // Given
        validDto.setComment("这个人真是傻逼");
        when(validationConfig.containsViolation("这个人真是傻逼")).thenReturn(true);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.COMMENT_CONTAINS_VIOLATION.getCode());
        verify(validationConfig, times(1)).containsViolation("这个人真是傻逼");
    }

    @Test
    @DisplayName("规则3 - 评论不包含违规词")
    void testValidateSubmission_CommentNoViolation() {
        // Given
        validDto.setComment("合作愉快");
        when(validationConfig.containsViolation("合作愉快")).thenReturn(false);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(validationConfig, times(1)).containsViolation("合作愉快");
    }

    @Test
    @DisplayName("规则3 - 评论为 null 不算违规")
    void testValidateSubmission_CommentNull() {
        // Given
        validDto.setComment(null);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(validationConfig, never()).containsViolation(any());
    }

    @Test
    @DisplayName("规则3 - 评论为空字符串不算违规")
    void testValidateSubmission_CommentEmpty() {
        // Given
        validDto.setComment("");

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(validationConfig, never()).containsViolation(any());
    }

    @Test
    @DisplayName("规则3 - 真实配置测试 - 操作熟练不违规")
    void testValidateSubmission_RealConfig_OperationNotViolation() {
        // Given
        validDto.setComment("操作熟练，技术能力强");
        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("规则3 - 真实配置测试 - 操场沟通正常不违规")
    void testValidateSubmission_RealConfig_PlaygroundNotViolation() {
        // Given
        validDto.setComment("在操场沟通项目需求，配合默契");
        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("规则3 - 真实配置测试 - 明确脏话命中")
    void testValidateSubmission_RealConfig_ExplicitViolation() {
        // Given
        validDto.setComment("这个人真是傻逼");
        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.COMMENT_CONTAINS_VIOLATION.getCode());
    }

    // ==================== 规则 4: 高分缺少正向标签 ====================

    @Test
    @DisplayName("规则4 - 高分缺少正向标签 - 有 5 分但 positiveTags 为 null")
    void testValidateSubmission_HighScoreMissingPositiveTag_Null() {
        // Given
        validDto.setCommunicationScore(5);
        validDto.setPositiveTags(null);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.HIGH_SCORE_MISSING_POSITIVE_TAG.getCode());
    }

    @Test
    @DisplayName("规则4 - 高分缺少正向标签 - 有 5 分但 positiveTags 为空列表")
    void testValidateSubmission_HighScoreMissingPositiveTag_Empty() {
        // Given
        validDto.setTaskScore(5);
        validDto.setPositiveTags(Collections.emptyList());

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.HIGH_SCORE_MISSING_POSITIVE_TAG.getCode());
    }

    @Test
    @DisplayName("规则4 - 高分有正向标签 - 通过")
    void testValidateSubmission_HighScoreWithPositiveTag() {
        // Given
        validDto.setSkillScore(5);
        validDto.setPositiveTags(Arrays.asList("技术可靠"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("规则4 - 高分正向标签不在白名单 - 失败")
    void testValidateSubmission_HighScoreUnknownPositiveTag() {
        // Given
        validDto.setCommunicationScore(5);
        validDto.setPositiveTags(Arrays.asList("非常优秀"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.HIGH_SCORE_MISSING_POSITIVE_TAG.getCode());
    }

    @Test
    @DisplayName("规则4 - 高分正向标签混入未知值 - 失败")
    void testValidateSubmission_HighScoreMixedUnknownPositiveTag() {
        // Given
        validDto.setTaskScore(5);
        validDto.setPositiveTags(Arrays.asList("沟通积极", "非常优秀"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.HIGH_SCORE_MISSING_POSITIVE_TAG.getCode());
    }

    @Test
    @DisplayName("规则4 - 没有 5 分不需要正向标签")
    void testValidateSubmission_NoHighScore() {
        // Given
        validDto.setCommunicationScore(4);
        validDto.setTaskScore(4);
        validDto.setSkillScore(4);
        validDto.setResponsibilityScore(4);
        validDto.setPositiveTags(null);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    // ==================== 规则 5: 低分缺少负向标签 ====================

    @Test
    @DisplayName("规则5 - 低分缺少负向标签 - 有 1 分但 negativeTags 为 null")
    void testValidateSubmission_LowScoreMissingNegativeTag_Null() {
        // Given
        validDto.setCommunicationScore(1);
        validDto.setNegativeTags(null);
        validDto.setComment("需要改进");

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.LOW_SCORE_MISSING_NEGATIVE_TAG.getCode());
    }

    @Test
    @DisplayName("规则5 - 低分缺少负向标签 - 有 2 分但 negativeTags 为空列表")
    void testValidateSubmission_LowScoreMissingNegativeTag_Empty() {
        // Given
        validDto.setTaskScore(2);
        validDto.setNegativeTags(Collections.emptyList());
        validDto.setComment("需要改进");

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.LOW_SCORE_MISSING_NEGATIVE_TAG.getCode());
    }

    @Test
    @DisplayName("规则5 - 低分有负向标签 - 通过")
    void testValidateSubmission_LowScoreWithNegativeTag() {
        // Given
        validDto.setSkillScore(1);
        validDto.setNegativeTags(Arrays.asList("质量低"));
        validDto.setComment("技术实现质量偏低，需要补齐测试和交付标准");

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("规则5 - 低分负向标签不在白名单 - 失败")
    void testValidateSubmission_LowScoreUnknownNegativeTag() {
        // Given
        validDto.setCommunicationScore(1);
        validDto.setNegativeTags(Arrays.asList("任务拖延"));
        validDto.setComment("任务推进存在延期，需要更早暴露风险并同步进度");

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.LOW_SCORE_MISSING_NEGATIVE_TAG.getCode());
    }

    @Test
    @DisplayName("规则5 - 低分负向标签混入未知值 - 失败")
    void testValidateSubmission_LowScoreMixedUnknownNegativeTag() {
        // Given
        validDto.setTaskScore(2);
        validDto.setNegativeTags(Arrays.asList("延期", "任务拖延"));
        validDto.setComment("任务推进存在延期，需要更早暴露风险并同步进度");

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.LOW_SCORE_MISSING_NEGATIVE_TAG.getCode());
    }

    @Test
    @DisplayName("规则5 - 没有低分不需要负向标签")
    void testValidateSubmission_NoLowScore() {
        // Given
        validDto.setCommunicationScore(3);
        validDto.setTaskScore(3);
        validDto.setSkillScore(3);
        validDto.setResponsibilityScore(3);
        validDto.setNegativeTags(null);

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    // ==================== 规则 6: 低分评论为空 ====================

    @Test
    @DisplayName("规则6 - 低分评论为空 - 有 1 分但 comment 为 null")
    void testValidateSubmission_LowScoreCommentEmpty_Null() {
        // Given
        validDto.setCommunicationScore(1);
        validDto.setComment(null);
        validDto.setNegativeTags(Arrays.asList("沟通差"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.LOW_SCORE_COMMENT_TOO_SHORT.getCode());
    }

    @Test
    @DisplayName("规则6 - 低分评论为空 - 有 2 分但 comment 为空字符串")
    void testValidateSubmission_LowScoreCommentEmpty_EmptyString() {
        // Given
        validDto.setTaskScore(2);
        validDto.setComment("");
        validDto.setNegativeTags(Arrays.asList("延期"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.LOW_SCORE_COMMENT_TOO_SHORT.getCode());
    }

    @Test
    @DisplayName("规则6 - 低分评论为空 - 有 1 分但 comment 只有空格")
    void testValidateSubmission_LowScoreCommentEmpty_OnlySpaces() {
        // Given
        validDto.setSkillScore(1);
        validDto.setComment("   ");
        validDto.setNegativeTags(Arrays.asList("质量低"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.LOW_SCORE_COMMENT_TOO_SHORT.getCode());
    }

    @Test
    @DisplayName("规则6 - 低分有评论 - 通过")
    void testValidateSubmission_LowScoreWithComment() {
        // Given
        validDto.setResponsibilityScore(2);
        validDto.setComment("责任心表现不稳定，需要主动同步进度并按时完成");
        validDto.setNegativeTags(Arrays.asList("失联"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("规则6 - 低分评论 19 字 - 失败")
    void testValidateSubmission_LowScoreCommentLengthNineteen() {
        // Given
        validDto.setCommunicationScore(1);
        validDto.setComment("一".repeat(19));
        validDto.setNegativeTags(Arrays.asList("沟通差"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.LOW_SCORE_COMMENT_TOO_SHORT.getCode());
    }

    @Test
    @DisplayName("规则6 - 低分评论 20 字 - 通过")
    void testValidateSubmission_LowScoreCommentLengthTwenty() {
        // Given
        validDto.setCommunicationScore(1);
        validDto.setComment("一".repeat(20));
        validDto.setNegativeTags(Arrays.asList("沟通差"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("规则6 - 低分评论 21 字 - 通过")
    void testValidateSubmission_LowScoreCommentLengthTwentyOne() {
        // Given
        validDto.setTaskScore(2);
        validDto.setComment("一".repeat(21));
        validDto.setNegativeTags(Arrays.asList("延期"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    // ==================== 规则 7: 低分不能只有正向标签（由规则 5 覆盖）====================

    @Test
    @DisplayName("规则7 - 低分只有正向标签 - 被规则5拦截")
    void testValidateSubmission_LowScoreOnlyPositiveTag() {
        // Given
        validDto.setCommunicationScore(1);
        validDto.setPositiveTags(Arrays.asList("沟通积极"));
        validDto.setNegativeTags(Collections.emptyList());
        validDto.setComment("沟通响应明显不足，需要及时同步问题并主动确认");

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getCode()).isEqualTo(ReasonCode.LOW_SCORE_MISSING_NEGATIVE_TAG.getCode());
    }

    // ==================== 正向场景：所有规则都满足 ====================

    @Test
    @DisplayName("正向场景 - 所有规则都满足 - 中等评分")
    void testValidateSubmission_Success_MediumScore() {
        // Given: validDto 已经是合法的中等评分（4分）

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("正向场景 - 所有规则都满足 - 高分场景")
    void testValidateSubmission_Success_HighScore() {
        // Given
        validDto.setCommunicationScore(5);
        validDto.setTaskScore(5);
        validDto.setSkillScore(5);
        validDto.setResponsibilityScore(5);
        validDto.setComment("非常优秀，合作愉快");
        validDto.setPositiveTags(Arrays.asList("沟通积极", "技术可靠", "责任心强"));
        validDto.setNegativeTags(Collections.emptyList());

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("正向场景 - 所有规则都满足 - 低分场景")
    void testValidateSubmission_Success_LowScore() {
        // Given
        validDto.setCommunicationScore(1);
        validDto.setTaskScore(2);
        validDto.setSkillScore(1);
        validDto.setResponsibilityScore(2);
        validDto.setComment("沟通不畅，任务拖延，技术能力不足，责任心需要提升");
        validDto.setPositiveTags(Collections.emptyList());
        validDto.setNegativeTags(Arrays.asList("沟通差", "延期", "质量低", "失联"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("正向场景 - 所有规则都满足 - 混合评分场景")
    void testValidateSubmission_Success_MixedScore() {
        // Given
        validDto.setCommunicationScore(5);
        validDto.setTaskScore(1);
        validDto.setSkillScore(4);
        validDto.setResponsibilityScore(3);
        validDto.setComment("沟通表现很好，但任务完成度不够，需要明确交付节奏");
        validDto.setPositiveTags(Arrays.asList("沟通积极"));
        validDto.setNegativeTags(Arrays.asList("延期"));

        // When
        Result<Void> result = service.validateSubmission(validDto);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }
}
