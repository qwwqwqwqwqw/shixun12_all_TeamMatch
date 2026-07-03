package com.teammatch.m4.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teammatch.entity.CreditChange;
import com.teammatch.entity.Evaluation;
import com.teammatch.m4.service.impl.CreditRuleServiceImpl;
import com.teammatch.mapper.CreditChangeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreditRuleServiceImplTest {

    @Mock
    private CreditChangeMapper creditChangeMapper;

    @InjectMocks
    private CreditRuleServiceImpl creditRuleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void mapAverageScoreToDelta_boundary_1_00() {
        assertThat(creditRuleService.mapAverageScoreToDelta(1.00)).isEqualTo(-5);
    }

    @Test
    void mapAverageScoreToDelta_boundary_1_50() {
        assertThat(creditRuleService.mapAverageScoreToDelta(1.50)).isEqualTo(-3);
    }

    @Test
    void mapAverageScoreToDelta_boundary_2_00() {
        assertThat(creditRuleService.mapAverageScoreToDelta(2.00)).isEqualTo(-2);
    }

    @Test
    void mapAverageScoreToDelta_boundary_2_50() {
        assertThat(creditRuleService.mapAverageScoreToDelta(2.50)).isEqualTo(-1);
    }

    @Test
    void mapAverageScoreToDelta_boundary_3_00() {
        assertThat(creditRuleService.mapAverageScoreToDelta(3.00)).isEqualTo(0);
    }

    @Test
    void mapAverageScoreToDelta_boundary_3_50() {
        assertThat(creditRuleService.mapAverageScoreToDelta(3.50)).isEqualTo(2);
    }

    @Test
    void mapAverageScoreToDelta_boundary_4_00() {
        assertThat(creditRuleService.mapAverageScoreToDelta(4.00)).isEqualTo(3);
    }

    @Test
    void mapAverageScoreToDelta_boundary_4_50() {
        assertThat(creditRuleService.mapAverageScoreToDelta(4.50)).isEqualTo(5);
    }

    @Test
    void mapAverageScoreToDelta_boundary_5_00() {
        assertThat(creditRuleService.mapAverageScoreToDelta(5.00)).isEqualTo(5);
    }

    @Test
    void mapAverageScoreToDelta_avg_4_25() {
        assertThat(creditRuleService.mapAverageScoreToDelta(4.25)).isEqualTo(3);
    }

    @Test
    void mapAverageScoreToDelta_avg_2_75() {
        assertThat(creditRuleService.mapAverageScoreToDelta(2.75)).isEqualTo(-1);
    }

    @Test
    void calculateDeltaFromEvaluation_all_5() {
        Evaluation eval = new Evaluation();
        eval.setCommunicationScore(5);
        eval.setTaskScore(5);
        eval.setSkillScore(5);
        eval.setResponsibilityScore(5);
        assertThat(creditRuleService.calculateDeltaFromEvaluation(eval)).isEqualTo(5);
    }

    @Test
    void calculateDeltaFromEvaluation_avg_4_50() {
        Evaluation eval = new Evaluation();
        eval.setCommunicationScore(5);
        eval.setTaskScore(5);
        eval.setSkillScore(4);
        eval.setResponsibilityScore(4);
        assertThat(creditRuleService.calculateDeltaFromEvaluation(eval)).isEqualTo(5);
    }

    @Test
    void calculateDeltaFromEvaluation_all_4() {
        Evaluation eval = new Evaluation();
        eval.setCommunicationScore(4);
        eval.setTaskScore(4);
        eval.setSkillScore(4);
        eval.setResponsibilityScore(4);
        assertThat(creditRuleService.calculateDeltaFromEvaluation(eval)).isEqualTo(3);
    }

    @Test
    void calculateDeltaFromEvaluation_all_3() {
        Evaluation eval = new Evaluation();
        eval.setCommunicationScore(3);
        eval.setTaskScore(3);
        eval.setSkillScore(3);
        eval.setResponsibilityScore(3);
        assertThat(creditRuleService.calculateDeltaFromEvaluation(eval)).isEqualTo(0);
    }

    @Test
    void calculateDeltaFromEvaluation_avg_1_50() {
        Evaluation eval = new Evaluation();
        eval.setCommunicationScore(1);
        eval.setTaskScore(2);
        eval.setSkillScore(1);
        eval.setResponsibilityScore(2);
        assertThat(creditRuleService.calculateDeltaFromEvaluation(eval)).isEqualTo(-3);
    }

    @Test
    void calculateCappedDelta_no_cap() {
        when(creditChangeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(createCreditChanges(5));
        int result = creditRuleService.calculateCappedDelta(1L, 1L, 3);
        assertThat(result).isEqualTo(3);
    }

    @Test
    void calculateCappedDelta_positive_cap() {
        when(creditChangeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(createCreditChanges(8));
        int result = creditRuleService.calculateCappedDelta(1L, 1L, 5);
        assertThat(result).isEqualTo(2);
    }

    @Test
    void calculateCappedDelta_already_at_positive_cap() {
        when(creditChangeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(createCreditChanges(10));
        int result = creditRuleService.calculateCappedDelta(1L, 1L, 3);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void calculateCappedDelta_no_negative_cap() {
        when(creditChangeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(createCreditChanges(0));
        int result = creditRuleService.calculateCappedDelta(1L, 1L, -5);
        assertThat(result).isEqualTo(-5);
    }

    @Test
    void calculateCappedDelta_negative_cap() {
        when(creditChangeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(createCreditChanges(-8));
        int result = creditRuleService.calculateCappedDelta(1L, 1L, -5);
        assertThat(result).isEqualTo(-2);
    }

    @Test
    void calculateCappedDelta_already_at_negative_cap() {
        when(creditChangeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(createCreditChanges(-10));
        int result = creditRuleService.calculateCappedDelta(1L, 1L, -3);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void calculateCappedDelta_positive_negative_offset() {
        when(creditChangeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(createCreditChanges(7));
        int result = creditRuleService.calculateCappedDelta(1L, 1L, -3);
        assertThat(result).isEqualTo(-3);
    }

    @Test
    void calculateCappedDelta_negative_positive_offset() {
        when(creditChangeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(createCreditChanges(-3));
        int result = creditRuleService.calculateCappedDelta(1L, 1L, 5);
        assertThat(result).isEqualTo(5);
    }

    @Test
    void calculateCappedDelta_zero_delta() {
        when(creditChangeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(createCreditChanges(5));
        int result = creditRuleService.calculateCappedDelta(1L, 1L, 0);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void calculateCappedDeltaFromExisting_no_cap() {
        assertThat(creditRuleService.calculateCappedDeltaFromExisting(5, 3)).isEqualTo(3);
    }

    @Test
    void calculateCappedDeltaFromExisting_positive_cap() {
        assertThat(creditRuleService.calculateCappedDeltaFromExisting(8, 5)).isEqualTo(2);
    }

    @Test
    void calculateCappedDeltaFromExisting_already_at_positive_cap() {
        assertThat(creditRuleService.calculateCappedDeltaFromExisting(10, 3)).isEqualTo(0);
    }

    @Test
    void calculateCappedDeltaFromExisting_no_negative_cap() {
        assertThat(creditRuleService.calculateCappedDeltaFromExisting(0, -5)).isEqualTo(-5);
    }

    @Test
    void calculateCappedDeltaFromExisting_negative_cap() {
        assertThat(creditRuleService.calculateCappedDeltaFromExisting(-8, -5)).isEqualTo(-2);
    }

    @Test
    void calculateCappedDeltaFromExisting_already_at_negative_cap() {
        assertThat(creditRuleService.calculateCappedDeltaFromExisting(-10, -3)).isEqualTo(0);
    }

    @Test
    void calculateCappedDeltaFromExisting_positive_negative_offset() {
        assertThat(creditRuleService.calculateCappedDeltaFromExisting(7, -3)).isEqualTo(-3);
    }

    @Test
    void calculateCappedDeltaFromExisting_negative_positive_offset() {
        assertThat(creditRuleService.calculateCappedDeltaFromExisting(-3, 5)).isEqualTo(5);
    }

    @Test
    void calculateCappedDeltaFromExisting_zero_delta() {
        assertThat(creditRuleService.calculateCappedDeltaFromExisting(5, 0)).isEqualTo(0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getExistingProjectDelta_verifyQueryConditions() {
        when(creditChangeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>());

        creditRuleService.getExistingProjectDelta(100L, 200L);

        verify(creditChangeMapper).selectList(any(LambdaQueryWrapper.class));
    }

    private List<CreditChange> createCreditChanges(int totalValue) {
        List<CreditChange> changes = new ArrayList<>();
        if (totalValue != 0) {
            CreditChange change = new CreditChange();
            change.setChangeValue(totalValue);
            changes.add(change);
        }
        return changes;
    }
}