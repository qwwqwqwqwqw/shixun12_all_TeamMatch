package com.teammatch.m6.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.entity.Appeal;
import com.teammatch.m6.dto.AppealCreateDTO;
import com.teammatch.m6.dto.AppealableEvaluationVO;
import com.teammatch.m6.dto.AppealablePenaltyVO;
import com.teammatch.m6.service.AppealService;
import com.teammatch.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 申诉 Controller（用户端）
 *
 * 根据详细设计文档 7.5 节定义
 * 用户端接口前缀：/api/appeals
 */
@RestController
@RequestMapping("/appeals")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;
    private final AuthUtil authUtil;

    /**
     * 提交申诉
     * POST /api/appeals
     */
    @PostMapping
    public Result<Appeal> createAppeal(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AppealCreateDTO dto) {
        Long userId = authUtil.requireUserId(token);

        try {
            Appeal appeal = appealService.createAppeal(userId, dto);
            return Result.success(appeal);
        } catch (IllegalArgumentException e) {
            return Result.of(ReasonCode.PARAM_ERROR.getCode(), e.getMessage(), null);
        } catch (IllegalStateException e) {
            return Result.of(ReasonCode.STATUS_CONFLICT.getCode(), e.getMessage(), null);
        }
    }

    /**
     * 申诉页：可申诉的评价列表
     * GET /api/appeals/appealable/evaluations
     */
    @GetMapping("/appealable/evaluations")
    public Result<List<AppealableEvaluationVO>> listAppealableEvaluations(
            @RequestHeader("Authorization") String token) {
        Long userId = authUtil.requireUserId(token);
        return Result.success(appealService.listAppealableEvaluations(userId));
    }

    /**
     * 申诉页：可申诉的生效中处罚列表
     * GET /api/appeals/appealable/penalties
     */
    @GetMapping("/appealable/penalties")
    public Result<List<AppealablePenaltyVO>> listAppealablePenalties(
            @RequestHeader("Authorization") String token) {
        Long userId = authUtil.requireUserId(token);
        return Result.success(appealService.listAppealablePenalties(userId));
    }

    /**
     * 获取我的申诉列表
     * GET /api/appeals/my
     */
    @GetMapping("/my")
    public Result<List<Appeal>> getMyAppeals(@RequestHeader("Authorization") String token) {
        Long userId = authUtil.requireUserId(token);
        List<Appeal> appeals = appealService.getMyAppeals(userId);
        return Result.success(appeals);
    }

    /**
     * 获取申诉详情
     * GET /api/appeals/{id}
     */
    @GetMapping("/{id}")
    public Result<Appeal> getAppealById(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long id) {
        Long userId = authUtil.requireUserId(token);

        Appeal appeal = appealService.getAppealById(id);
        if (appeal == null) {
            return Result.fail(ReasonCode.NOT_FOUND);
        }

        if (!appeal.getUserId().equals(userId)) {
            return Result.fail(ReasonCode.FORBIDDEN);
        }

        return Result.success(appeal);
    }
}
