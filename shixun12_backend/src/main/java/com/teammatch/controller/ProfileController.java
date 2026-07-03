package com.teammatch.controller;

import com.teammatch.common.Result;
import com.teammatch.dto.AddSkillTagRequest;
import com.teammatch.dto.FileUploadResultVO;
import com.teammatch.dto.ProfileDetailVO;
import com.teammatch.dto.UpdateProfileRequest;
import com.teammatch.dto.UpdateUserSkillsRequest;
import com.teammatch.entity.SkillTag;
import com.teammatch.entity.User;
import com.teammatch.exception.DuplicateDataException;
import com.teammatch.exception.NotFoundException;
import com.teammatch.exception.ValidationException;
import com.teammatch.service.AuthService;
import com.teammatch.service.ProfileService;
import com.teammatch.service.storage.FileCategory;
import com.teammatch.service.storage.OssService;
import com.teammatch.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * M3 个人档案控制器
 */
@Slf4j
@RestController
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private AuthService authService;

    
    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private OssService ossService;

    /**
     * 获取指定用户的档案详情（供项目详情页显示队长/成员名称）
     * 路径: GET /api/profile/detail/{userId}
     * 需登录认证
     */
    @GetMapping("/detail/{userId}")
    public Result<ProfileDetailVO> getUserProfile(@PathVariable Long userId,
                                                  @RequestHeader("Authorization") String token) {
        try {
            // 必须登录
            authUtil.requireUserId(token);
            
            User profile = profileService.getProfileById(userId);
            if (profile == null) {
                return Result.fail(com.teammatch.common.ReasonCode.NOT_FOUND);
            }
            // 转换为 VO，只暴露安全字段
            return Result.success(toProfileVO(profile));
        } catch (com.teammatch.exception.AuthenticationException e) {
            return Result.fail(e.getReasonCode());
        }
    }

    /**
     * 获取当前用户档案详情
     * 路径: GET /api/profile/detail
     * 
     * 使用 ProfileDetailVO 而非直接返回 User 实体，避免暴露敏感字段（openid、passwordHash）
     */
    @GetMapping("/detail")
    public Result<ProfileDetailVO> getProfileDetail(@RequestHeader("Authorization") String token) {
        try {
            // 必须登录
            Long userId = authUtil.requireUserId(token);
            
            User profile = profileService.getProfileById(userId);
            if (profile == null) {
                return Result.fail(com.teammatch.common.ReasonCode.NOT_FOUND);
            }
            
            // 转换为 VO，只暴露安全字段
            return Result.success(toProfileVO(profile));
        } catch (com.teammatch.exception.AuthenticationException e) {
            return Result.fail(e.getReasonCode());
        }
    }

    /**
     * D. 更新个人档案
     * 路径: PUT /api/profile/update
     */
    @PutMapping("/update")
    public Result<Void> updateProfile(@RequestBody UpdateProfileRequest request,
                                      @RequestHeader("Authorization") String token) {
        try {
            // 必须登录，失败会抛出 AuthenticationException(M3000)
            Long userId = authUtil.requireUserId(token);

            User profile = new User();
            profile.setNickname(request.getNickname());
            profile.setAvatarUrl(request.getAvatarUrl());
            profile.setSchool(request.getSchool());
            profile.setMajor(request.getMajor());
            profile.setGrade(request.getGrade());
            profile.setBio(request.getBio());

            profileService.updateProfile(userId, profile);
            return Result.success(null);
        } catch (com.teammatch.exception.AuthenticationException e) {
            return Result.fail(e.getReasonCode());
        }
    }

    /**
     * 上传头像并更新档案
     * POST /api/profile/avatar
     */
    @PostMapping("/avatar")
    public Result<FileUploadResultVO> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                   @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtil.requireUserId(token);
            FileUploadResultVO uploadResult = ossService.upload(FileCategory.AVATAR, userId, file);

            User profile = new User();
            profile.setAvatarUrl(uploadResult.getStoredUrl());
            profileService.updateProfile(userId, profile);
            return Result.success(uploadResult);
        } catch (com.teammatch.exception.AuthenticationException e) {
            return Result.fail(e.getReasonCode());
        } catch (ValidationException e) {
            return Result.of(e.getReasonCode().getCode(), e.getMessage(), null);
        }
    }

    private ProfileDetailVO toProfileVO(User profile) {
        ProfileDetailVO vo = ProfileDetailVO.from(profile);
        if (vo != null && vo.getAvatarUrl() != null) {
            try {
                vo.setAvatarUrl(ossService.resolveAccessibleUrl(vo.getAvatarUrl()));
            } catch (ValidationException e) {
                log.warn("头像 presign 失败，返回原始 URL: userId={}", profile.getId());
            }
        }
        return vo;
    }

    /**
     * E. 绑定 GitHub 账号
     * 路径: POST /api/profile/github/bind
     */
    @PostMapping("/github/bind")
    public Result<Void> bindGithub(@RequestBody java.util.Map<String, String> params,
                                   @RequestHeader("Authorization") String token) {
        // 必须登录
        Long userId = authUtil.requireUserId(token);
        
        String githubUsername = params.get("githubUsername");
        profileService.bindGithub(userId, githubUsername);
        return Result.success(null);
    }

    /**
     * E1. 更新 GitHub 账号
     * 路径: PUT /api/profile/github/update
     * 说明: 仅当已绑定过 GitHub 账号时可修改，否则返回 M3012 错误
     */
    @PutMapping("/github/update")
    public Result<Void> updateGithub(@RequestBody java.util.Map<String, String> params,
                                     @RequestHeader("Authorization") String token) {
        try {
            // 必须登录
            Long userId = authUtil.requireUserId(token);
            
            String githubUsername = params.get("githubUsername");
            profileService.updateGithub(userId, githubUsername);
            return Result.success(null);
        } catch (NotFoundException e) {
            // GitHub 账号未绑定
            return Result.fail(e.getReasonCode());
        } catch (com.teammatch.exception.ValidationException e) {
            return Result.fail(e.getReasonCode());
        } catch (Exception e) {
            // 未知异常
            return Result.fail(com.teammatch.common.ReasonCode.UNKNOWN_ERROR);
        }
    }

    /**
     * E2. 绑定 Gitee 账号（手动绑定，非 OAuth 流程）
     * 路径: POST /api/profile/gitee/bind
     * 说明: 推荐使用 OAuth 流程 GET /api/profile/gitee/auth，手动绑定无法获取真实 commits/PRs 数据
     */
    @PostMapping("/gitee/bind")
    public Result<Void> bindGitee(@RequestBody java.util.Map<String, String> params,
                                  @RequestHeader("Authorization") String token) {
        // 必须登录
        Long userId = authUtil.requireUserId(token);
        
        String giteeUsername = params.get("giteeUsername");
        profileService.bindGitee(userId, giteeUsername);
        return Result.success(null);
    }

    /**
     * E3. 更新 Gitee 账号
     * 路径: PUT /api/profile/gitee/update
     * 说明: 仅当已绑定过 Gitee 账号时可修改，否则返回 M3033 错误
     */
    @PutMapping("/gitee/update")
    public Result<Void> updateGitee(@RequestBody java.util.Map<String, String> params,
                                    @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtil.requireUserId(token);
            
            String giteeUsername = params.get("giteeUsername");
            profileService.updateGitee(userId, giteeUsername);
            return Result.success(null);
        } catch (NotFoundException e) {
            return Result.fail(e.getReasonCode());
        } catch (com.teammatch.exception.ValidationException e) {
            return Result.fail(e.getReasonCode());
        } catch (Exception e) {
            return Result.fail(com.teammatch.common.ReasonCode.UNKNOWN_ERROR);
        }
    }

    /**
     * F. 添加技能标签（旧接口，保留兼容）
     * 路径: POST /api/profile/skills/add
     */
    @PostMapping("/skills/add")
    public Result<Void> addSkill(@RequestBody java.util.Map<String, Long> params,
                                 @RequestHeader("Authorization") String token) {
        // 必须登录
        Long userId = authUtil.requireUserId(token);
        
        Long skillTagId = params.get("skillTagId");
        profileService.addSkill(userId, skillTagId);
        return Result.success(null);
    }

    /**
     * F1. 全量替换用户技能（新接口，推荐使用）
     * 路径: PUT /api/profile/skills
     * 
     * 【实习要点3】全量替换模式：前端提交最终的技能列表，后端先删除旧的再插入新的
     */
    @PutMapping("/skills")
    public Result<Void> updateUserSkills(@Valid @RequestBody UpdateUserSkillsRequest request,
                                         @RequestHeader("Authorization") String token) {
        // 必须登录
        Long userId = authUtil.requireUserId(token);
        
        try {
            profileService.updateUserSkills(userId, request.getSkillTagIds());
            return Result.success(null);
        } catch (NotFoundException e) {
            // 技能标签不存在
            return Result.fail(e.getReasonCode());
        } catch (Exception e) {
            // 未知异常
            return Result.fail(com.teammatch.common.ReasonCode.UNKNOWN_ERROR);
        }
    }

    /**
     * F2. 获取所有可用的技能标签列表
     * 路径: GET /api/profile/skills/tags
     * 
     * 【实习要点3】预置字典模式：技能标签由系统预置，用户只能从中选择
     */
    @GetMapping("/skills/tags")
    public Result<List<SkillTag>> listSkillTags() {
        List<SkillTag> tags = profileService.listActiveSkillTags();
        return Result.success(tags);
    }

    /**
     * G. 添加新的技能标签到数据库（管理员功能）
     * 路径: POST /api/profile/skills/tag/add
     */
    @PostMapping("/skills/tag/add")
    public Result<Void> addSkillTag(@RequestBody AddSkillTagRequest request,
                                    @RequestHeader("Authorization") String token) {
        try {
            // 必须是管理员，失败会抛出 AuthenticationException(M3000) 或 AuthorizationException(M3009)
            authUtil.requireAdmin(token);
        } catch (com.teammatch.exception.AuthenticationException e) {
            return Result.fail(e.getReasonCode());
        } catch (com.teammatch.exception.AuthorizationException e) {
            return Result.fail(e.getReasonCode());
        }
        
        SkillTag skillTag = new SkillTag();
        skillTag.setName(request.getName());
        skillTag.setCategory(request.getCategory());
        skillTag.setStatus("active"); // 强制设为 active
        try {
            profileService.addSkillTag(skillTag);
            return Result.success(null);
        } catch (DuplicateDataException e) {
            // 技能标签已存在
            return Result.fail(e.getReasonCode());
        }
    }
}
