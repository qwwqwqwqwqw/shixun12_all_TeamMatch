package com.teammatch.controller;

import com.teammatch.common.Result;
import com.teammatch.dto.EvaluationSubmitDTO;
import com.teammatch.dto.EvaluationSubmitResult;
import com.teammatch.dto.EvaluatableMemberDTO;
import com.teammatch.dto.ReceivedEvaluationVO;
import com.teammatch.dto.SubmitEvaluationRequest;
import com.teammatch.entity.Evaluation;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.mapper.EvaluationMapper;
import com.teammatch.service.EvaluationEligibilityService;
import com.teammatch.service.EvaluationSubmitService;
import com.teammatch.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * M5-8A 用户端互评接口控制器
 *
 * 提供 5 个用户端互评 API：
 * 1. 检查项目级互评资格
 * 2. 检查目标级互评资格
 * 3. 查询可评价成员列表
 * 4. 提交互评
 * 5. 查看我收到的互评（匿名）
 *
 * Controller 只负责鉴权注入、参数接收、DTO 转换、调用 Service、统一响应。
 * 不重写任何业务状态机。
 */
@RestController
@RequestMapping("/m5")
public class EvaluationController {

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private EvaluationEligibilityService evaluationEligibilityService;

    @Autowired
    private EvaluationSubmitService evaluationSubmitService;

    @Autowired
    private EvaluationMapper evaluationMapper;

    // ==================== 鉴权 ====================

    /**
     * 统一鉴权包装：所有用户端互评接口都从 token 注入 userId。
     *
     * @param authHeader Authorization header 值（required=false，可为 null）
     * @param action 鉴权通过后执行的业务逻辑
     * @return 业务 Result；鉴权失败时统一返回 UNAUTHORIZED
     */
    private <T> Result<T> withAuthenticatedUser(String authHeader, Function<Long, Result<T>> action) {
        try {
            Long userId = authUtil.requireUserId(authHeader);
            return action.apply(userId);
        } catch (AuthenticationException e) {
            return Result.fail(e.getReasonCode());
        }
    }

    // ==================== B1: 项目级互评资格 ====================

    /**
     * 检查当前用户能否进入该项目的互评页面。
     * 只判断项目级条件，不判断具体评价谁。
     */
    @GetMapping("/projects/{projectId}/evaluation-eligibility")
    public Result<Boolean> checkProjectEligibility(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return withAuthenticatedUser(authHeader,
                userId -> evaluationEligibilityService.checkProjectEligibility(userId, projectId));
    }

    // ==================== B2: 目标级互评资格 ====================

    /**
     * 检查当前用户能否评价某个具体成员。
     * 覆盖：不能自评、target 必须是 active 成员、不能重复评价、
     * 项目 ended、互评窗口未关闭。
     */
    @GetMapping("/projects/{projectId}/members/{targetId}/evaluation-eligibility")
    public Result<Boolean> checkTargetEligibility(
            @PathVariable Long projectId,
            @PathVariable Long targetId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return withAuthenticatedUser(authHeader, userId -> {
            Result<Void> result = evaluationEligibilityService.validateSubmission(userId, targetId, projectId);
            if (result.isSuccess()) {
                return Result.success(true);
            }
            // 直接透传原始 code/message，不经过 ReasonCode.fromCode 避免未知 code 降级
            return Result.of(result.getCode(), result.getMessage(), null);
        });
    }

    // ==================== B3: 可评价成员列表 ====================

    /**
     * 查询当前用户在该项目中可以评价的成员列表。
     * 不包含自己，标记是否已评价。
     */
    @GetMapping("/projects/{projectId}/evaluatable-members")
    public Result<List<EvaluatableMemberDTO>> getEvaluatableMembers(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return withAuthenticatedUser(authHeader,
                userId -> evaluationEligibilityService.getEvaluatableMembers(userId, projectId));
    }

    // ==================== B4: 提交互评 ====================

    /**
     * 提交对某个项目成员的互评。
     * 请求体不含 evaluatorId，评价人身份由 Controller 从 token 注入。
     */
    @PostMapping("/evaluations")
    public Result<EvaluationSubmitResult> submitEvaluation(
            @RequestBody SubmitEvaluationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return withAuthenticatedUser(authHeader, userId -> {
            // 将前端请求 DTO 转为已有 Service DTO，强制注入 evaluatorId
            EvaluationSubmitDTO dto = new EvaluationSubmitDTO();
            dto.setEvaluatorId(userId);   // P0: 评价人身份只能来自 token
            dto.setTargetId(request.getTargetId());
            dto.setProjectId(request.getProjectId());
            dto.setCommunicationScore(request.getCommunicationScore());
            dto.setTaskScore(request.getTaskScore());
            dto.setSkillScore(request.getSkillScore());
            dto.setResponsibilityScore(request.getResponsibilityScore());
            dto.setComment(request.getComment());
            dto.setPositiveTags(request.getPositiveTags());
            dto.setNegativeTags(request.getNegativeTags());

            return evaluationSubmitService.submit(dto);
        });
    }

    // ==================== B5: 查看我收到的互评 ====================

    /**
     * 查看当前用户收到的互评（匿名，不暴露评价者身份）。
     * projectId 可选：不传返回全部，传入则只返回该项目的评价。
     * 过滤 voided 评价，pending_review 和 kept_no_credit 正常展示。
     */
    @GetMapping("/evaluations/received")
    public Result<List<ReceivedEvaluationVO>> getReceivedEvaluations(
            @RequestParam(required = false) Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return withAuthenticatedUser(authHeader, userId -> {
            List<Evaluation> evaluations = evaluationMapper.findByTargetId(userId);

            List<ReceivedEvaluationVO> result = evaluations.stream()
                    // 过滤 voided 评价（不展示给被评价者，V2.1 §6.8）
                    .filter(e -> !Objects.equals(e.getStatus(), Evaluation.STATUS_VOIDED))
                    // 若 projectId 非空，按 projectId 二次过滤
                    .filter(e -> projectId == null
                            || Objects.equals(e.getProjectId(), projectId))
                    // 转换为匿名 VO（不暴露 evaluatorId）
                    .map(this::toReceivedEvaluationVO)
                    .collect(Collectors.toList());

            return Result.success(result);
        });
    }

    /**
     * Evaluation 实体 → ReceivedEvaluationVO（匿名，剥离 evaluatorId）
     */
    private ReceivedEvaluationVO toReceivedEvaluationVO(Evaluation e) {
        ReceivedEvaluationVO vo = new ReceivedEvaluationVO();
        vo.setEvaluationId(e.getId());
        vo.setProjectId(e.getProjectId());
        vo.setCommunicationScore(e.getCommunicationScore());
        vo.setTaskScore(e.getTaskScore());
        vo.setSkillScore(e.getSkillScore());
        vo.setResponsibilityScore(e.getResponsibilityScore());
        vo.setAverageScore(e.getAverageScore());
        vo.setComment(e.getComment());
        vo.setStatus(e.getStatus());
        vo.setCreatedAt(e.getCreatedAt());
        // 注意：不设置 evaluatorId，保证匿名
        return vo;
    }
}
