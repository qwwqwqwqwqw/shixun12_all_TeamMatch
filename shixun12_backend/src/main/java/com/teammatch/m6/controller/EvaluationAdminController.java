package com.teammatch.m6.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teammatch.common.Result;
import com.teammatch.dto.EvaluationReviewCommand;
import com.teammatch.dto.EvaluationReviewResult;
import com.teammatch.entity.Evaluation;
import com.teammatch.m6.dto.EvaluationReviewRequest;
import com.teammatch.m6.dto.PendingEvaluationVO;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.service.EvaluationReviewService;
import com.teammatch.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 评价复核 Controller（管理端）。
 *
 * 提供待复核评价列表查询和执行评价复核两个管理端 HTTP 入口。
 * 业务逻辑全部委托给 M5-6 的 {@link EvaluationReviewService}。
 */
@RestController
@RequestMapping("/admin/evaluations")
@RequiredArgsConstructor
public class EvaluationAdminController {

    private final AuthUtil authUtil;
    private final EvaluationReviewService evaluationReviewService;
    private final EvaluationMapper evaluationMapper;

    /**
     * 查询评价复核列表（支持按状态筛选，行为与申诉/举报列表一致）。
     *
     * GET /api/admin/evaluations?status={optional}&projectId={optional}
     *
     * status 为空或 all 时不按状态过滤；可传 pending_review / normal / voided / kept_no_credit。
     */
    @GetMapping
    public Result<List<PendingEvaluationVO>> listEvaluations(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long projectId) {
        authUtil.requireAdmin(token);
        return Result.success(queryEvaluations(status, projectId, false));
    }

    /**
     * 查询待复核评价列表（兼容旧路径）。
     *
     * GET /api/admin/evaluations/pending?status={optional}&projectId={optional}
     *
     * 未传 status 时默认仅返回 pending_review；传空字符串或 all 时返回全部状态。
     */
    @GetMapping("/pending")
    public Result<List<PendingEvaluationVO>> getPendingEvaluations(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long projectId) {
        authUtil.requireAdmin(token);
        return Result.success(queryEvaluations(status, projectId, true));
    }

    private List<PendingEvaluationVO> queryEvaluations(String status, Long projectId, boolean pendingEndpoint) {
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
        if (hasStatusFilter(status)) {
            wrapper.eq(Evaluation::getStatus, status);
        } else if (pendingEndpoint && status == null) {
            wrapper.eq(Evaluation::getStatus, Evaluation.STATUS_PENDING_REVIEW);
        }
        if (projectId != null) {
            wrapper.eq(Evaluation::getProjectId, projectId);
        }
        wrapper.orderByDesc(Evaluation::getCreatedAt);

        return evaluationMapper.selectList(wrapper).stream()
                .map(this::toPendingEvaluationVO)
                .collect(Collectors.toList());
    }

    private boolean hasStatusFilter(String status) {
        return status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status);
    }

    /**
     * 执行评价复核。
     *
     * POST /api/admin/evaluations/{id}/review
     */
    @PostMapping("/{id}/review")
    public Result<EvaluationReviewResult> reviewEvaluation(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long id,
            @RequestBody EvaluationReviewRequest request) {
        authUtil.requireAdmin(token);
        Long adminId = authUtil.requireUserId(token);

        EvaluationReviewCommand command = new EvaluationReviewCommand();
        command.setEvaluationId(id);
        command.setReviewerId(adminId);
        command.setAction(request.getAction());
        command.setReviewNote(request.getReviewNote());

        return evaluationReviewService.review(command);
    }

    private PendingEvaluationVO toPendingEvaluationVO(Evaluation evaluation) {
        PendingEvaluationVO vo = new PendingEvaluationVO();
        vo.setEvaluationId(evaluation.getId());
        vo.setProjectId(evaluation.getProjectId());
        vo.setEvaluatorId(evaluation.getEvaluatorId());
        vo.setTargetId(evaluation.getTargetId());
        vo.setCommunicationScore(evaluation.getCommunicationScore());
        vo.setTaskScore(evaluation.getTaskScore());
        vo.setSkillScore(evaluation.getSkillScore());
        vo.setResponsibilityScore(evaluation.getResponsibilityScore());
        vo.setAverageScore(evaluation.getAverageScore());
        vo.setComment(evaluation.getComment());
        vo.setStatus(evaluation.getStatus());
        vo.setCreatedAt(evaluation.getCreatedAt());
        return vo;
    }
}
