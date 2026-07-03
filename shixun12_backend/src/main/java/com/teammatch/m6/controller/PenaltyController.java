package com.teammatch.m6.controller;

import com.teammatch.common.Result;
import com.teammatch.m6.entity.Penalty;
import com.teammatch.m6.service.PenaltyService;
import com.teammatch.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 处罚 Controller（用户端）
 *
 * 根据详细设计文档 7.2 节定义
 * 用户端接口前缀：/api/penalties
 * 仅提供查询功能，创建和撤销由管理端处理
 */
@RestController
@RequestMapping("/penalties")
@RequiredArgsConstructor
public class PenaltyController {

    private final PenaltyService penaltyService;
    private final AuthUtil authUtil;

    /**
     * 获取我的处罚记录列表
     * GET /api/penalties/my
     */
    @GetMapping("/my")
    public Result<List<Penalty>> getMyPenalties(@RequestHeader("Authorization") String token) {
        Long userId = authUtil.requireUserId(token);
        List<Penalty> penalties = penaltyService.getUserPenalties(userId);
        return Result.success(penalties);
    }

    /**
     * 获取我生效中的处罚记录
     * GET /api/penalties/my/active
     */
    @GetMapping("/my/active")
    public Result<List<Penalty>> getMyActivePenalties(@RequestHeader("Authorization") String token) {
        Long userId = authUtil.requireUserId(token);
        List<Penalty> penalties = penaltyService.getUserActivePenalties(userId);
        return Result.success(penalties);
    }
}
