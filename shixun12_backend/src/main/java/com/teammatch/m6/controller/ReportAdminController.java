package com.teammatch.m6.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.exception.BusinessException;
import com.teammatch.m6.dto.ReportHandleDTO;
import com.teammatch.m6.entity.Report;
import com.teammatch.m6.service.ReportService;
import com.teammatch.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 举报管理 Controller（管理端）
 *
 * 根据详细设计文档 7.2 节定义
 * 管理端接口前缀：/api/admin/reports
 * 需要管理员权限
 */
@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportAdminController {

    private final ReportService reportService;
    private final AuthUtil authUtil;

    /**
     * 获取举报列表
     * GET /api/admin/reports
     */
    @GetMapping
    public Result<List<Report>> getReportList(
            @RequestParam(required = false) String status,
            @RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);
        List<Report> reports = reportService.getReportList(status);
        return Result.success(reports);
    }

    /**
     * 获取举报详情
     * GET /api/admin/reports/{id}
     */
    @GetMapping("/{id}")
    public Result<Report> getReport(@PathVariable Long id,
                                    @RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);

        Report report = reportService.getReportById(id);
        if (report == null) {
            return Result.fail(ReasonCode.NOT_FOUND);
        }
        return Result.success(report);
    }

    /**
     * 处理举报
     * PUT /api/admin/reports/{id}/handle
     */
    @PutMapping("/{id}/handle")
    public Result<Report> handleReport(@PathVariable Long id,
                                       @Valid @RequestBody ReportHandleDTO dto,
                                       @RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);
        Long handlerId = authUtil.requireUserId(token);

        try {
            Report report = reportService.handleReport(id, handlerId, dto);
            return Result.success(report);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("举报不存在")) {
                return Result.fail(ReasonCode.NOT_FOUND);
            }
            return Result.of(ReasonCode.PARAM_ERROR.getCode(), msg != null ? msg : ReasonCode.PARAM_ERROR.getMessage(), null);
        } catch (IllegalStateException e) {
            return Result.of(ReasonCode.STATUS_CONFLICT.getCode(),
                    e.getMessage() != null ? e.getMessage() : ReasonCode.STATUS_CONFLICT.getMessage(), null);
        } catch (BusinessException e) {
            return Result.of(e.getReasonCode().getCode(), e.getMessage(), null);
        } catch (RuntimeException e) {
            log.error("handleReport failed, reportId={}, handlerId={}", id, handlerId, e);
            return Result.fail(ReasonCode.UNKNOWN_ERROR);
        }
    }
}
