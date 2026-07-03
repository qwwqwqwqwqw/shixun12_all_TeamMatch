package com.teammatch.m4.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.m4.dto.TeamRequestDTO;
import com.teammatch.m4.service.TeamRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/m4/team-requests")
@RequiredArgsConstructor
public class TeamRequestController {

    private final TeamRequestService teamRequestService;

    /** T-122 队长发送邀请 */
    @PostMapping("/invite")
    public Result<Void> sendInvite(@RequestBody TeamRequestDTO dto) {
        try {
            teamRequestService.sendRequest(dto, "invite");
            return Result.success();
        } catch (Exception e) {
            return Result.fail(mapRequestReason(e));
        }
    }

    /** T-123 用户申请加入项目 */
    @PostMapping("/apply")
    public Result<Void> sendApply(@RequestBody TeamRequestDTO dto) {
        try {
            teamRequestService.sendRequest(dto, "apply");
            return Result.success();
        } catch (Exception e) {
            return Result.fail(mapRequestReason(e));
        }
    }

    /** T-124 接受请求 */
    @PostMapping("/{id}/accept")
    public Result<Void> acceptRequest(@PathVariable Long id, @RequestParam Long operatorId) {
        try {
            teamRequestService.acceptRequest(id, operatorId);
            return Result.success();
        } catch (Exception e) {
            return Result.fail(mapRequestReason(e));
        }
    }

    /** T-129 拒绝请求 */
    @PostMapping("/{id}/reject")
    public Result<Void> rejectRequest(@PathVariable Long id, @RequestParam Long operatorId) {
        try {
            teamRequestService.rejectRequest(id, operatorId);
            return Result.success();
        } catch (Exception e) {
            return Result.fail(mapRequestReason(e));
        }
    }

    /** T-130 取消请求 */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelRequest(@PathVariable Long id, @RequestParam Long operatorId) {
        try {
            teamRequestService.cancelRequest(id, operatorId);
            return Result.success();
        } catch (Exception e) {
            return Result.fail(mapRequestReason(e));
        }
    }

    /** T-131 收到/发出的请求列表 */
    @GetMapping
    public Result<?> getRequestList(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "received") String direction) {
        return Result.success(teamRequestService.getRequestList(userId, direction));
    }

    private ReasonCode mapRequestReason(Exception e) {
        String message = e.getMessage();
        if ("该项目不存在".equals(message)) {
            return ReasonCode.M4_PROJECT_NOT_FOUND;
        }
        if ("PROJECT_NOT_RECRUITING".equals(message)) {
            return ReasonCode.M4_PROJECT_STATUS_INVALID;
        }
        if ("PROJECT_FULL".equals(message)) {
            return ReasonCode.M4_TEAM_FULL;
        }
        if ("只有队长可以发送邀请".equals(message)
                || "只有队长可以接受申请".equals(message)
                || "只有队长可以拒绝申请".equals(message)) {
            return ReasonCode.M4_NOT_LEADER;
        }
        if ("DUPLICATE_PENDING_REQUEST".equals(message)) {
            return ReasonCode.M4_DUPLICATE_PENDING_REQUEST;
        }
        if ("USER_ALREADY_IN_PROJECT".equals(message)) {
            return ReasonCode.M4_USER_ALREADY_IN_PROJECT;
        }
        if ("请求不存在".equals(message)) {
            return ReasonCode.M4_RESOURCE_NOT_FOUND;
        }
        if ("该请求已处理".equals(message) || "该请求已处理，无法取消".equals(message)) {
            return ReasonCode.M4_REQUEST_ALREADY_HANDLED;
        }
        if ("无权操作此请求".equals(message) || "只有发送方才能取消请求".equals(message)) {
            return ReasonCode.M4_UNAUTHORIZED_REQUEST;
        }
        return ReasonCode.UNKNOWN_ERROR;
    }
}

