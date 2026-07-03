package com.teammatch.service;

import com.teammatch.dto.CreditCalculationResult;
import com.teammatch.entity.Evaluation;
import com.teammatch.m4.service.CreditRuleService;
import com.teammatch.service.impl.CreditCalculationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class CreditCalculationServiceTest {

    @Mock
    private CreditRuleService creditRuleService;

    @InjectMocks
    private CreditCalculationServiceImpl creditCalculationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void calculate_full_flow_no_cap() {
        Evaluation eval = createEvaluation(1L, 1L, 5, 5, 5, 5);

        when(creditRuleService.mapAverageScoreToDelta(5.0)).thenReturn(5);
        when(creditRuleService.getExistingProjectDelta(1L, 1L)).thenReturn(0);
        when(creditRuleService.calculateCappedDeltaFromExisting(0, 5)).thenReturn(5);

        CreditCalculationResult result = creditCalculationService.calculate(eval);

        assertThat(result).isNotNull();
        assertThat(result.getAverageScore()).isEqualTo(new BigDecimal("5.00"));
        assertThat(result.getRawDelta()).isEqualTo(5);
        assertThat(result.getExistingProjectDelta()).isEqualTo(0);
        assertThat(result.getCappedDelta()).isEqualTo(5);
        assertThat(result.getProjectTotalAfterApplied()).isEqualTo(5);
        assertThat(result.getCapped()).isFalse();
    }

    @Test
    void calculate_full_flow_with_cap() {
        Evaluation eval = createEvaluation(1L, 1L, 5, 5, 5, 5);

        when(creditRuleService.mapAverageScoreToDelta(5.0)).thenReturn(5);
        when(creditRuleService.getExistingProjectDelta(1L, 1L)).thenReturn(8);
        when(creditRuleService.calculateCappedDeltaFromExisting(8, 5)).thenReturn(2);

        CreditCalculationResult result = creditCalculationService.calculate(eval);

        assertThat(result).isNotNull();
        assertThat(result.getAverageScore()).isEqualTo(new BigDecimal("5.00"));
        assertThat(result.getRawDelta()).isEqualTo(5);
        assertThat(result.getExistingProjectDelta()).isEqualTo(8);
        assertThat(result.getCappedDelta()).isEqualTo(2);
        assertThat(result.getProjectTotalAfterApplied()).isEqualTo(10);
        assertThat(result.getCapped()).isTrue();
    }

    @Test
    void calculate_negative_cap() {
        Evaluation eval = createEvaluation(1L, 1L, 1, 1, 1, 1);

        when(creditRuleService.mapAverageScoreToDelta(1.0)).thenReturn(-5);
        when(creditRuleService.getExistingProjectDelta(1L, 1L)).thenReturn(-8);
        when(creditRuleService.calculateCappedDeltaFromExisting(-8, -5)).thenReturn(-2);

        CreditCalculationResult result = creditCalculationService.calculate(eval);

        assertThat(result).isNotNull();
        assertThat(result.getAverageScore()).isEqualTo(new BigDecimal("1.00"));
        assertThat(result.getRawDelta()).isEqualTo(-5);
        assertThat(result.getExistingProjectDelta()).isEqualTo(-8);
        assertThat(result.getCappedDelta()).isEqualTo(-2);
        assertThat(result.getProjectTotalAfterApplied()).isEqualTo(-10);
        assertThat(result.getCapped()).isTrue();
    }

    @Test
    void calculate_zero_existing() {
        Evaluation eval = createEvaluation(1L, 1L, 4, 4, 4, 4);

        when(creditRuleService.mapAverageScoreToDelta(4.0)).thenReturn(3);
        when(creditRuleService.getExistingProjectDelta(1L, 1L)).thenReturn(0);
        when(creditRuleService.calculateCappedDeltaFromExisting(0, 3)).thenReturn(3);

        CreditCalculationResult result = creditCalculationService.calculate(eval);

        assertThat(result).isNotNull();
        assertThat(result.getAverageScore()).isEqualTo(new BigDecimal("4.00"));
        assertThat(result.getRawDelta()).isEqualTo(3);
        assertThat(result.getExistingProjectDelta()).isEqualTo(0);
        assertThat(result.getCappedDelta()).isEqualTo(3);
        assertThat(result.getProjectTotalAfterApplied()).isEqualTo(3);
        assertThat(result.getCapped()).isFalse();
    }

    @Test
    void calculate_already_at_cap() {
        Evaluation eval = createEvaluation(1L, 1L, 5, 5, 5, 5);

        when(creditRuleService.mapAverageScoreToDelta(5.0)).thenReturn(5);
        when(creditRuleService.getExistingProjectDelta(1L, 1L)).thenReturn(10);
        when(creditRuleService.calculateCappedDeltaFromExisting(10, 5)).thenReturn(0);

        CreditCalculationResult result = creditCalculationService.calculate(eval);

        assertThat(result).isNotNull();
        assertThat(result.getAverageScore()).isEqualTo(new BigDecimal("5.00"));
        assertThat(result.getRawDelta()).isEqualTo(5);
        assertThat(result.getExistingProjectDelta()).isEqualTo(10);
        assertThat(result.getCappedDelta()).isEqualTo(0);
        assertThat(result.getProjectTotalAfterApplied()).isEqualTo(10);
        assertThat(result.getCapped()).isTrue();
    }

    @Test
    void calculate_boundary_4_49() {
        Evaluation eval = createEvaluation(1L, 1L, 4, 5, 4, 4);

        when(creditRuleService.mapAverageScoreToDelta(4.25)).thenReturn(3);
        when(creditRuleService.getExistingProjectDelta(1L, 1L)).thenReturn(0);
        when(creditRuleService.calculateCappedDeltaFromExisting(0, 3)).thenReturn(3);

        CreditCalculationResult result = creditCalculationService.calculate(eval);

        assertThat(result).isNotNull();
        assertThat(result.getAverageScore()).isEqualTo(new BigDecimal("4.25"));
        assertThat(result.getRawDelta()).isEqualTo(3);
        assertThat(result.getExistingProjectDelta()).isEqualTo(0);
        assertThat(result.getCappedDelta()).isEqualTo(3);
        assertThat(result.getProjectTotalAfterApplied()).isEqualTo(3);
        assertThat(result.getCapped()).isFalse();
    }

    @Test
    void calculate_neutral_score() {
        Evaluation eval = createEvaluation(1L, 1L, 3, 3, 3, 3);

        when(creditRuleService.mapAverageScoreToDelta(3.0)).thenReturn(0);
        when(creditRuleService.getExistingProjectDelta(1L, 1L)).thenReturn(5);
        when(creditRuleService.calculateCappedDeltaFromExisting(5, 0)).thenReturn(0);

        CreditCalculationResult result = creditCalculationService.calculate(eval);

        assertThat(result).isNotNull();
        assertThat(result.getAverageScore()).isEqualTo(new BigDecimal("3.00"));
        assertThat(result.getRawDelta()).isEqualTo(0);
        assertThat(result.getExistingProjectDelta()).isEqualTo(5);
        assertThat(result.getCappedDelta()).isEqualTo(0);
        assertThat(result.getProjectTotalAfterApplied()).isEqualTo(5);
        assertThat(result.getCapped()).isFalse();
    }

    @Test
    void calculate_positive_negative_offset() {
        Evaluation eval = createEvaluation(1L, 1L, 1, 2, 1, 2);

        when(creditRuleService.mapAverageScoreToDelta(1.5)).thenReturn(-3);
        when(creditRuleService.getExistingProjectDelta(1L, 1L)).thenReturn(7);
        when(creditRuleService.calculateCappedDeltaFromExisting(7, -3)).thenReturn(-3);

        CreditCalculationResult result = creditCalculationService.calculate(eval);

        assertThat(result).isNotNull();
        assertThat(result.getAverageScore()).isEqualTo(new BigDecimal("1.50"));
        assertThat(result.getRawDelta()).isEqualTo(-3);
        assertThat(result.getExistingProjectDelta()).isEqualTo(7);
        assertThat(result.getCappedDelta()).isEqualTo(-3);
        assertThat(result.getProjectTotalAfterApplied()).isEqualTo(4);
        assertThat(result.getCapped()).isFalse();
    }

    private Evaluation createEvaluation(Long targetId, Long projectId,
                                       int comm, int task, int skill, int resp) {
        Evaluation eval = new Evaluation();
        eval.setTargetId(targetId);
        eval.setProjectId(projectId);
        eval.setCommunicationScore(comm);
        eval.setTaskScore(task);
        eval.setSkillScore(skill);
        eval.setResponsibilityScore(resp);
        return eval;
    }
}