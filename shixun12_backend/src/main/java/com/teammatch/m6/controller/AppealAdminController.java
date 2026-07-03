package com.teammatch.m6.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.entity.Appeal;
import com.teammatch.exception.BusinessException;
import com.teammatch.m6.dto.AppealHandleDTO;
import com.teammatch.m6.service.AppealService;
import com.teammatch.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 申诉 Controller（管理端）
 *
 * 根据详细设计文档 7.5 节定义
 * 管理端接口前缀：/api/admin/appeals
 */
@RestController
@RequestMapping("/admin/appeals")
@RequiredArgsConstructor
@Slf4j
public class AppealAdminController {

    private final AppealService appealService;
    private final AuthUtil authUtil;

    /**
     * 获取申诉列表
     * GET /api/admin/appeals?status=&targetType=
     */
    @GetMapping
    public Result<List<Appeal>> getAppealList(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType) {
        authUtil.requireAdmin(token);
        List<Appeal> appeals = appealService.getAppealList(status, targetType);
        return Result.success(appeals);
    }

    /**
     * 获取申诉详情
     * GET /api/admin/appeals/{id}
     */
    @GetMapping("/{id}")
    public Result<Appeal> getAppealById(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long id) {
        authUtil.requireAdmin(token);

        Appeal appeal = appealService.getAppealById(id);
        if (appeal == null) {
            return Result.fail(ReasonCode.NOT_FOUND);
        }

        return Result.success(appeal);
    }

    /**
     * 处理申诉
     * PUT /api/admin/appeals/{id}/handle
     */
    @PutMapping("/{id}/handle")
    public Result<Appeal> handleAppeal(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long id,
            @Valid @RequestBody AppealHandleDTO dto) {
        authUtil.requireAdmin(token);
        Long handlerId = authUtil.requireUserId(token);

        try {
            Appeal appeal = appealService.handleAppeal(id, handlerId, dto);
            return Result.success(appeal);
        } catch (IllegalArgumentException e) {
            return Result.of(ReasonCode.PARAM_ERROR.getCode(), e.getMessage(), null);
        } catch (IllegalStateException e) {
            return Result.of(ReasonCode.STATUS_CONFLICT.getCode(), e.getMessage(), null);
        } catch (BusinessException e) {
            return Result.of(e.getReasonCode().getCode(), e.getMessage(), null);
        } catch (RuntimeException e) {
            log.error("handleAppeal failed, appealId={}, handlerId={}", id, handlerId, e);
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }
}
