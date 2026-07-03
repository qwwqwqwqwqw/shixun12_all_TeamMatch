package com.teammatch.m4.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teammatch.entity.CreditChange;
import com.teammatch.entity.Evaluation;
import com.teammatch.m4.service.CreditRuleService;
import com.teammatch.mapper.CreditChangeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditRuleServiceImpl implements CreditRuleService {

    private final CreditChangeMapper creditChangeMapper;

    /**
     * V2.1 八区间映射算法:
     * [1.0, 1.5) -> -5
     * [1.5, 2.0) -> -3
     * [2.0, 2.5) -> -2
     * [2.5, 3.0) -> -1
     * [3.0, 3.5) -> 0
     * [3.5, 4.0) -> +2
     * [4.0, 4.5) -> +3
     * [4.5, 5.0] -> +5
     */
    @Override
    public int calculateDeltaFromEvaluation(Evaluation evaluation) {
        int totalScore = evaluation.getCommunicationScore() +
                         evaluation.getTaskScore() +
                         evaluation.getSkillScore() +
                         evaluation.getResponsibilityScore();
        double avg = totalScore / 4.0;
        return mapAverageScoreToDelta(avg);
    }

    @Override
    public int mapAverageScoreToDelta(double avg) {
        if (avg >= 4.5) return 5;
        if (avg >= 4.0) return 3;
        if (avg >= 3.5) return 2;
        if (avg >= 3.0) return 0;
        if (avg >= 2.5) return -1;
        if (avg >= 2.0) return -2;
        if (avg >= 1.5) return -3;
        return -5;
    }

    @Override
    public int getExistingProjectDelta(Long userId, Long projectId) {
        List<CreditChange> changes = creditChangeMapper.selectList(new LambdaQueryWrapper<CreditChange>()
                .eq(CreditChange::getUserId, userId)
                .eq(CreditChange::getProjectId, projectId)
                .eq(CreditChange::getChangeType, "evaluation")
                .eq(CreditChange::getEffective, true));

        int total = 0;
        for (CreditChange cc : changes) {
            total += cc.getChangeValue();
        }
        return total;
    }

    @Override
    public int calculateCappedDelta(Long userId, Long projectId, int proposedDelta) {
        int existing = getExistingProjectDelta(userId, projectId);
        return calculateCappedDeltaFromExisting(existing, proposedDelta);
    }

    @Override
    public int calculateCappedDeltaFromExisting(int existingProjectDelta, int proposedDelta) {
        if (proposedDelta == 0) return 0;

        int maxAllowed = 10;
        int targetTotal = existingProjectDelta + proposedDelta;

        if (targetTotal > maxAllowed) {
            return Math.max(0, maxAllowed - existingProjectDelta);
        } else if (targetTotal < -maxAllowed) {
            return Math.min(0, -maxAllowed - existingProjectDelta);
        }

        return proposedDelta;
    }
}
