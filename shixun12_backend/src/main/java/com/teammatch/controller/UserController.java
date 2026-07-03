package com.teammatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teammatch.common.Result;
import com.teammatch.dto.ProfileDetailVO;
import com.teammatch.entity.User;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.storage.OssService;
import com.teammatch.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * M3 用户控制器
 * 提供用户列表查询等通用接口
 */
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private OssService ossService;

    /**
     * 获取用户列表（分页）
     * GET /api/users?page=1&size=20&keyword=张三
     *
     * 返回 ProfileDetailVO 排除 openid、passwordHash 等敏感字段
     */
    @GetMapping
    public Result<Page<ProfileDetailVO>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestHeader("Authorization") String token) {

        // 必须登录
        authUtil.requireUserId(token);

        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;

        // 构建查询条件（不指定 select 列，VO 转换时会自动排除敏感字段）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 支持按昵称关键字搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(User::getNickname, keyword.trim());
        }

        wrapper.orderByDesc(User::getCreatedAt);

        // 分页查询
        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);

        // 转换为 VO，排除敏感字段
        Page<ProfileDetailVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream()
                .map(u -> {
                    ProfileDetailVO vo = ProfileDetailVO.from(u);
                    vo.setAvatarUrl(ossService.resolveAvatarUrl(vo.getAvatarUrl()));
                    return vo;
                })
                .collect(java.util.stream.Collectors.toList()));

        return Result.success(voPage);
    }
}
