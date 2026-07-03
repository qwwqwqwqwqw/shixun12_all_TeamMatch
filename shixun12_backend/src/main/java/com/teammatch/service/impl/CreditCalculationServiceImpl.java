package com.teammatch.service.impl;

import com.teammatch.dto.CreditCalculationResult;
import com.teammatch.entity.Evaluation;
import com.teammatch.m4.service.CreditRuleService;
import com.teammatch.service.CreditCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CreditCalculationServiceImpl implements CreditCalculationService {

    private final CreditRuleService creditRuleService;

    @Override
    public CreditCalculationResult calculate(Evaluation evaluation) {
        int totalScore = evaluation.getCommunicationScore() +
                         evaluation.getTaskScore() +
                         evaluation.getSkillScore() +
                         evaluation.getResponsibilityScore();
        double avg = totalScore / 4.0;
        BigDecimal averageScore = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

        int rawDelta = creditRuleService.mapAverageScoreToDelta(avg);

        int existingProjectDelta = creditRuleService.getExistingProjectDelta(
                evaluation.getTargetId(),
                evaluation.getProjectId()
        );

        int cappedDelta = creditRuleService.calculateCappedDeltaFromExisting(existingProjectDelta, rawDelta);

        int projectTotalAfterApplied = existingProjectDelta + cappedDelta;
        boolean capped = (cappedDelta != rawDelta);

        CreditCalculationResult result = new CreditCalculationResult();
        result.setAverageScore(averageScore);
        result.setRawDelta(rawDelta);
        result.setExistingProjectDelta(existingProjectDelta);
        result.setCappedDelta(cappedDelta);
        result.setProjectTotalAfterApplied(projectTotalAfterApplied);
        result.setCapped(capped);

        return result;
    }
}
