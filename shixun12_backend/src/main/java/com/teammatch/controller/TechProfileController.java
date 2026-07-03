package com.teammatch.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.LeaderboardEntryVO;
import com.teammatch.dto.TechProfileVO;
import com.teammatch.service.TechProfileService;
import com.teammatch.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * M3 技术画像 & 排行榜控制器
 * 栏目：板块二 冷启动 / 技术画像 / 档案认领
 */
@RestController
public class TechProfileController {

    @Autowired
    private TechProfileService techProfileService;

    @Autowired
    private AuthUtil authUtil;

    /**
     * 获取当前登录用户的技术画像
     * GET /api/profile/tech-profile
     */
    @GetMapping("/profile/tech-profile")
    public Result<TechProfileVO> getMyTechProfile(@RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtil.requireUserId(token);
            TechProfileVO vo = techProfileService.getProfileByUserId(userId);
            if (vo == null) {
                return Result.fail(ReasonCode.TECH_PROFILE_NOT_FOUND);
            }
            return Result.success(vo);
        } catch (com.teammatch.exception.AuthenticationException e) {
            return Result.fail(e.getReasonCode());
        }
    }

    /**
     * 获取指定用户的技术画像
     * GET /api/profile/tech-profile/{userId}
     */
    @GetMapping("/profile/tech-profile/{userId}")
    public Result<TechProfileVO> getUserTechProfile(@PathVariable Long userId) {
        TechProfileVO vo = techProfileService.getProfileByUserId(userId);
        if (vo == null) {
            return Result.fail(ReasonCode.TECH_PROFILE_NOT_FOUND);
        }
        return Result.success(vo);
    }

    /**
     * 获取排行榜（按 tech_score 降序）
     * GET /api/leaderboard?page=1&size=20
     */
    @GetMapping("/leaderboard")
    public Result<List<LeaderboardEntryVO>> getLeaderboard(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;

        List<LeaderboardEntryVO> entries = techProfileService.getLeaderboard(page, size);
        return Result.success(entries);
    }

    /**
     * 获取排行榜总数
     * GET /api/leaderboard/count
     */
    @GetMapping("/leaderboard/count")
    public Result<Long> getLeaderboardCount() {
        long count = techProfileService.getLeaderboardCount();
        return Result.success(count);
    }
}