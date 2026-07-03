package com.teammatch.m6.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.exception.BusinessException;
import com.teammatch.m6.dto.PenaltyCreateDTO;
import com.teammatch.m6.dto.PenaltyRevokeDTO;
import com.teammatch.m6.entity.Penalty;
import com.teammatch.m6.service.PenaltyService;
import com.teammatch.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 处罚管理 Controller（管理端）
 *
 * 根据详细设计文档 7.2 节定义
 * 管理端接口前缀：/api/admin/penalties
 * 需要管理员权限
 */
@RestController
@RequestMapping("/admin/penalties")
@RequiredArgsConstructor
@Slf4j
public class PenaltyAdminController {

    private final PenaltyService penaltyService;
    private final AuthUtil authUtil;

    /**
     * 创建处罚
     * POST /api/admin/penalties
     */
    @PostMapping
    public Result<Penalty> createPenalty(@Valid @RequestBody PenaltyCreateDTO dto,
                                         @RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);
        Long adminId = authUtil.requireUserId(token);

        try {
            Penalty penalty = penaltyService.createPenalty(adminId, dto);
            return Result.success(penalty);
        } catch (BusinessException e) {
            return Result.of(e.getReasonCode().getCode(), e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            return Result.of(ReasonCode.PARAM_ERROR.getCode(), e.getMessage(), null);
        } catch (RuntimeException e) {
            log.error("createPenalty failed, adminId={}, targetUserId={}", adminId, dto.getUserId(), e);
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }

    /**
     * 获取处罚列表
     * GET /api/admin/penalties
     */
    @GetMapping
    public Result<List<Penalty>> getPenaltyList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);
        List<Penalty> penalties = penaltyService.getPenaltyList(status, type);
        return Result.success(penalties);
    }

    /**
     * 获取处罚详情
     * GET /api/admin/penalties/{id}
     */
    @GetMapping("/{id}")
    public Result<Penalty> getPenalty(@PathVariable Long id,
                                      @RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);

        Penalty penalty = penaltyService.getPenaltyById(id);
        if (penalty == null) {
            return Result.fail(ReasonCode.NOT_FOUND);
        }
        return Result.success(penalty);
    }

    /**
     * 撤销处罚
     * PUT /api/admin/penalties/{id}/revoke
     */
    @PutMapping("/{id}/revoke")
    public Result<Penalty> revokePenalty(@PathVariable Long id,
                                         @Valid @RequestBody PenaltyRevokeDTO dto,
                                         @RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);
        Long adminId = authUtil.requireUserId(token);

        try {
            Penalty penalty = penaltyService.revokePenalty(id, adminId, dto);
            return Result.success(penalty);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("不存在")) {
                return Result.of(ReasonCode.NOT_FOUND.getCode(), msg, null);
            }
            return Result.of(ReasonCode.PARAM_ERROR.getCode(), msg != null ? msg : ReasonCode.PARAM_ERROR.getMessage(), null);
        } catch (IllegalStateException e) {
            return Result.of(ReasonCode.STATUS_CONFLICT.getCode(), e.getMessage(), null);
        } catch (BusinessException e) {
            return Result.of(e.getReasonCode().getCode(), e.getMessage(), null);
        } catch (RuntimeException e) {
            log.error("revokePenalty failed, penaltyId={}, adminId={}", id, adminId, e);
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }
}
