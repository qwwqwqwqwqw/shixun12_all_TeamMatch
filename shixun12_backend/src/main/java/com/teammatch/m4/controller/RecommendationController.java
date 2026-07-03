package com.teammatch.m4.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.RecommendationItem;
import com.teammatch.m4.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * M4 推荐接口
 * <p>GET /m4/projects/{projectId}/recommendations?limit=20</p>
 */
@RestController
@RequestMapping("/m4/projects")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{projectId}/recommendations")
    public Result<List<RecommendationItem>> recommend(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "20") int limit) {
        if (projectId == null || projectId <= 0) {
            return Result.fail(ReasonCode.PARAM_ERROR);
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        try {
            return Result.success(recommendationService.recommend(projectId, safeLimit));
        } catch (Exception e) {
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }
}
