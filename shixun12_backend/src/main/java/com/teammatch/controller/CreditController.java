package com.teammatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.dto.CreditChangePageVO;
import com.teammatch.dto.CreditChangeVO;
import com.teammatch.dto.CreditScoreVO;
import com.teammatch.entity.CreditChange;
import com.teammatch.entity.User;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.mapper.CreditChangeMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * M5-8B 用户端信誉查询接口控制器。
 *
 * Controller 只负责鉴权注入、只读查询、VO 转换和统一响应。
 */
@RestController
@RequestMapping("/m5")
public class CreditController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_CHANGE_TYPES = new HashSet<>(Arrays.asList(
            "evaluation",
            "exit_vote",
            "self_exit",
            "penalty",
            "penalty_restore",
            "appeal_restore"
    ));

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CreditChangeMapper creditChangeMapper;

    /**
     * 查看我的信誉分。
     */
    @GetMapping("/credit/score")
    public Result<CreditScoreVO> getMyCreditScore(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId;
        try {
            userId = authUtil.requireUserId(authHeader);
        } catch (AuthenticationException e) {
            return Result.fail(e.getReasonCode());
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail(ReasonCode.NOT_FOUND);
        }

        CreditScoreVO vo = new CreditScoreVO();
        vo.setUserId(userId);
        vo.setCreditScore(user.getCreditScore());
        return Result.success(vo);
    }

    /**
     * 查看我的信誉流水。
     */
    @GetMapping("/credit/changes")
    public Result<CreditChangePageVO> getMyCreditChanges(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String changeType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId;
        try {
            userId = authUtil.requireUserId(authHeader);
        } catch (AuthenticationException e) {
            return Result.fail(e.getReasonCode());
        }

        if (changeType != null && !changeType.isBlank() && !ALLOWED_CHANGE_TYPES.contains(changeType)) {
            return Result.fail(ReasonCode.PARAM_ERROR);
        }

        int normalizedPage = page < 1 ? DEFAULT_PAGE : page;
        int normalizedPageSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<CreditChange> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditChange::getUserId, userId);
        if (projectId != null) {
            wrapper.eq(CreditChange::getProjectId, projectId);
        }
        if (changeType != null && !changeType.isBlank()) {
            wrapper.eq(CreditChange::getChangeType, changeType);
        }
        wrapper.orderByDesc(CreditChange::getCreatedAt);

        List<CreditChangeVO> allVo = creditChangeMapper.selectList(wrapper).stream()
                .map(this::toCreditChangeVO)
                .collect(Collectors.toList());

        long total = allVo.size();
        long offset = (long) (normalizedPage - 1) * normalizedPageSize;
        int fromIndex = offset >= allVo.size() ? allVo.size() : (int) offset;
        int toIndex = Math.min(fromIndex + normalizedPageSize, allVo.size());

        CreditChangePageVO pageVO = new CreditChangePageVO();
        pageVO.setList(allVo.subList(fromIndex, toIndex));
        pageVO.setTotal(total);
        pageVO.setPage(normalizedPage);
        pageVO.setPageSize(normalizedPageSize);
        return Result.success(pageVO);
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private CreditChangeVO toCreditChangeVO(CreditChange creditChange) {
        CreditChangeVO vo = new CreditChangeVO();
        vo.setId(creditChange.getId());
        vo.setUserId(creditChange.getUserId());
        vo.setProjectId(creditChange.getProjectId());
        vo.setChangeType(creditChange.getChangeType());
        vo.setChangeValue(creditChange.getChangeValue());
        boolean effective = Boolean.TRUE.equals(creditChange.getEffective());
        vo.setEffective(effective);
        vo.setSuspended(!effective);
        vo.setDescription(creditChange.getDescription());
        vo.setCreatedAt(creditChange.getCreatedAt());
        return vo;
    }
}
