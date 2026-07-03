package com.teammatch.m4.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.m4.dto.ExitVoteCreateDTO;
import com.teammatch.m4.dto.ExitVoteSubmitDTO;
import com.teammatch.m4.dto.ExitVoteVO;
import com.teammatch.m4.service.ExitVoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * T-136~T-141: 退出流程 REST API
 * 路径前缀: /m4/projects/{projectId}/exit
 */
@RestController
@RequestMapping("/m4/projects/{projectId}/exit")
@RequiredArgsConstructor
public class ExitVoteController {

    private final ExitVoteService exitVoteService;

    /**
     * T-136: 成员主动退出
     * POST /m4/projects/{projectId}/exit/self?userId=xxx
     */
    @PostMapping("/self")
    public Result<Void> selfExit(
            @PathVariable("projectId") Long projectId,
            @RequestParam Long userId) {
        try {
            exitVoteService.selfExit(projectId, userId);
            return Result.success();
        } catch (Exception e) {
            return Result.fail(mapSelfExitReason(e));
        }
    }

    /**
     * T-137: 队长发起退出投票
     * POST /m4/projects/{projectId}/exit/votes
     */
    @PostMapping("/votes")
    public Result<ExitVoteVO> initiateVote(
            @PathVariable("projectId") Long projectId,
            @RequestBody ExitVoteCreateDTO dto) {
        try {
            return Result.success(exitVoteService.initiateVote(projectId, dto));
        } catch (Exception e) {
            return Result.fail(mapInitiateVoteReason(e));
        }
    }

    /**
     * 退出投票列表
     * GET /m4/projects/{projectId}/exit/votes
     */
    @GetMapping("/votes")
    public Result<List<ExitVoteVO>> getVoteList(
            @PathVariable("projectId") Long projectId) {
        return Result.success(exitVoteService.getVoteList(projectId));
    }

    /**
     * T-138: 投票详情
     * GET /m4/projects/{projectId}/exit/votes/{voteId}
     */
    @GetMapping("/votes/{voteId}")
    public Result<ExitVoteVO> getVoteDetail(
            @PathVariable("projectId") Long projectId,
            @PathVariable Long voteId) {
        try {
            ExitVoteVO vo = exitVoteService.getVoteDetail(voteId);
            if (vo == null) {
                return Result.fail(ReasonCode.M4_RESOURCE_NOT_FOUND);
            }
            return Result.success(vo);
        } catch (Exception e) {
            return Result.fail(ReasonCode.M4_RESOURCE_NOT_FOUND);
        }
    }

    /**
     * T-139: 提交投票
     * POST /m4/projects/{projectId}/exit/votes/{voteId}/submit
     */
    @PostMapping("/votes/{voteId}/submit")
    public Result<Void> submitVote(
            @PathVariable("projectId") Long projectId,
            @PathVariable Long voteId,
            @RequestBody ExitVoteSubmitDTO dto) {
        try {
            exitVoteService.submitVote(voteId, dto);
            return Result.success();
        } catch (Exception e) {
            return Result.fail(mapSubmitVoteReason(e));
        }
    }

    /**
     * T-140: 关闭投票并执行结果（T-141 事务+幂等在 Service 层保证）
     * POST /m4/projects/{projectId}/exit/votes/{voteId}/close?operatorId=xxx
     */
    @PostMapping("/votes/{voteId}/close")
    public Result<ExitVoteVO> closeVote(
            @PathVariable("projectId") Long projectId,
            @PathVariable Long voteId,
            @RequestParam Long operatorId) {
        try {
            return Result.success(exitVoteService.closeVote(voteId, operatorId));
        } catch (Exception e) {
            return Result.fail(mapCloseVoteReason(e));
        }
    }

    /** T-145: 队长取消进行中的投票
     * POST /m4/projects/{projectId}/exit/votes/{voteId}/cancel?operatorId=xxx
     */
    @PostMapping("/votes/{voteId}/cancel")
    public Result<Void> cancelVote(
            @PathVariable("projectId") Long projectId,
            @PathVariable Long voteId,
            @RequestParam Long operatorId) {
        try {
            exitVoteService.cancelVote(voteId, operatorId);
            return Result.success();
        } catch (Exception e) {
            return Result.fail(mapCancelVoteReason(e));
        }
    }

    private ReasonCode mapSelfExitReason(Exception e) {
        String message = e.getMessage();
        if ("项目不存在".equals(message)) {
            return ReasonCode.M4_PROJECT_NOT_FOUND;
        }
        if ("队长不能主动退出，请先转让队长身份".equals(message)) {
            return ReasonCode.M4_LEADER_CANNOT_EXIT;
        }
        if ("该用户不是项目的活跃成员".equals(message)) {
            return ReasonCode.M4_MEMBER_NOT_ACTIVE;
        }
        if ("TEAM_VOTE_CONFLICT".equals(message)) {
            return ReasonCode.M4_TEAM_VOTE_CONFLICT;
        }
        return ReasonCode.UNKNOWN_ERROR;
    }

    private ReasonCode mapInitiateVoteReason(Exception e) {
        String message = e.getMessage();
        if ("项目不存在".equals(message)) {
            return ReasonCode.M4_PROJECT_NOT_FOUND;
        }
        if ("只有进行中的项目才能发起退出投票".equals(message)) {
            return ReasonCode.M4_PROJECT_STATUS_INVALID;
        }
        if ("只有队长可以发起退出投票".equals(message)) {
            return ReasonCode.M4_NOT_LEADER;
        }
        if ("该成员已有进行中的退出投票".equals(message)) {
            return ReasonCode.M4_DUPLICATE_OPERATION;
        }
        if ("目标成员不是项目活跃成员".equals(message)) {
            return ReasonCode.M4_MEMBER_NOT_ACTIVE;
        }
        if ("不能对队长发起退出投票".equals(message)) {
            return ReasonCode.M4_CANNOT_VOTE_LEADER;
        }
        if ("INVALID_PENALTY_LEVEL".equals(message)) {
            return ReasonCode.INVALID_PENALTY_LEVEL;
        }
        return ReasonCode.UNKNOWN_ERROR;
    }

    private ReasonCode mapSubmitVoteReason(Exception e) {
        String message = e.getMessage();
        if ("投票不存在".equals(message)) {
            return ReasonCode.M4_RESOURCE_NOT_FOUND;
        }
        if ("投票已关闭".equals(message)) {
            return ReasonCode.M4_VOTE_ALREADY_CLOSED;
        }
        if ("DUPLICATE_VOTE".equals(message)) {
            return ReasonCode.M4_DUPLICATE_OPERATION;
        }
        if ("投票人不是项目活跃成员".equals(message)) {
            return ReasonCode.M4_MEMBER_NOT_ACTIVE;
        }
        if ("目标成员不能参与自己的退出投票".equals(message)) {
            return ReasonCode.M4_CANNOT_VOTE_SELF;
        }
        return ReasonCode.UNKNOWN_ERROR;
    }

    private ReasonCode mapCloseVoteReason(Exception e) {
        String message = e.getMessage();
        if ("投票不存在".equals(message)) {
            return ReasonCode.M4_RESOURCE_NOT_FOUND;
        }
        if ("只有发起人可以关闭投票".equals(message)) {
            return ReasonCode.M4_NOT_LEADER;
        }
        return ReasonCode.UNKNOWN_ERROR;
    }

    private ReasonCode mapCancelVoteReason(Exception e) {
        String message = e.getMessage();
        if ("投票不存在".equals(message)) {
            return ReasonCode.M4_RESOURCE_NOT_FOUND;
        }
        if ("投票已关闭".equals(message)) {
            return ReasonCode.M4_VOTE_ALREADY_CLOSED;
        }
        if ("只有发起人可以取消投票".equals(message)) {
            return ReasonCode.M4_NOT_LEADER;
        }
        return ReasonCode.UNKNOWN_ERROR;
    }
}
