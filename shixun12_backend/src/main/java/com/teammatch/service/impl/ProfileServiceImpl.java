package com.teammatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.entity.SkillTag;
import com.teammatch.entity.User;
import com.teammatch.entity.UserSkill;
import com.teammatch.exception.DuplicateDataException;
import com.teammatch.exception.NotFoundException;
import com.teammatch.exception.ValidationException;
import com.teammatch.mapper.SkillTagMapper;
import com.teammatch.mapper.UserMapper;
import com.teammatch.mapper.UserSkillMapper;
import com.teammatch.service.ProfileService;
import com.teammatch.service.TechProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * M3 档案服务实现类
 */
@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SkillTagMapper skillTagMapper;

    @Autowired
    private UserSkillMapper userSkillMapper;

    @Autowired
    private TechProfileService techProfileService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public User getProfileById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, User profile) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        if (profile.getNickname() != null) user.setNickname(profile.getNickname());
        if (profile.getAvatarUrl() != null) user.setAvatarUrl(profile.getAvatarUrl());
        if (profile.getSchool() != null) user.setSchool(profile.getSchool());
        if (profile.getMajor() != null) user.setMajor(profile.getMajor());
        if (profile.getGrade() != null) user.setGrade(profile.getGrade());
        if (profile.getBio() != null) user.setBio(profile.getBio());

        // 【实习要点4】更新档案后，重新计算正式档案状态
        // 【实习要点4】更新正式档案完成状态
        user.updateFormalProfileCompleted();
        userMapper.updateById(user);
    }

    @Override
    public void bindGithub(Long userId, String githubUsername) {
        // 1. 检查用户是否已绑定 GitHub
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) return;

        if (Boolean.TRUE.equals(currentUser.getGithubClaimed())) {
            // 已绑定：如果同一个账号则视为已更新，否则提示用更新接口
            if (githubUsername.equals(currentUser.getGithubUsername())) {
                log.info("GitHub 账号未变更，无需重新绑定: userId={}, githubUsername={}", userId, githubUsername);
                return;
            }
            throw new com.teammatch.exception.ValidationException(ReasonCode.GITHUB_ALREADY_BOUND);
        }

        // 2. 检查 GitHub 用户是否存在
        if (!checkGitHubUserExists(githubUsername)) {
            throw new com.teammatch.exception.NotFoundException(ReasonCode.GITHUB_USER_NOT_FOUND);
        }

        // 2.5 检查该 GitHub 账号是否已被其他用户认领
        com.teammatch.entity.TechProfile existingGhProfile = techProfileService.findByUsernameAndSource(githubUsername, "github");
        if (existingGhProfile != null && existingGhProfile.isClaimed()
                && !existingGhProfile.getClaimedByUserId().equals(userId)) {
            throw new com.teammatch.exception.ValidationException(ReasonCode.TECH_PROFILE_ALREADY_CLAIMED);
        }

        // 3. 事务内执行数据库操作
        transactionTemplate.execute(status -> {
            User user = userMapper.selectById(userId);
            if (user == null) return null;

            user.setGithubUsername(githubUsername);
            user.setGithubClaimed(true);
            // 切换平台时清除旧平台绑定
            if (Boolean.TRUE.equals(user.getGiteeClaimed())) {
                user.setGiteeUsername(null);
                user.setGiteeClaimed(false);
            }
            userMapper.updateById(user);

            try {
                techProfileService.claimProfile(githubUsername, "github", userId);
                log.info("技术画像认领成功: userId={}, githubUsername={}", userId, githubUsername);
            } catch (Exception e) {
                log.error("技术画像认领失败: userId={}, githubUsername={}, error={}", userId, githubUsername, e.getMessage());
            }
            return null;
        });

        // 4. 触发异步同步
        triggerAsyncSyncQuietly(githubUsername, "github");
    }

    @Override
    public void updateGithub(Long userId, String githubUsername) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        // 检查是否已绑定 GitHub
        if (!Boolean.TRUE.equals(user.getGithubClaimed())) {
            throw new com.teammatch.exception.ValidationException(ReasonCode.GITHUB_NOT_BOUND);
        }

        // 同一账号换绑到同一个 → 无操作
        if (githubUsername.equals(user.getGithubUsername())) {
            log.info("GitHub 账号未变更: userId={}, githubUsername={}", userId, githubUsername);
            return;
        }

        // 检查新 GitHub 用户是否存在
        if (!checkGitHubUserExists(githubUsername)) {
            throw new com.teammatch.exception.NotFoundException(ReasonCode.GITHUB_USER_NOT_FOUND);
        }

        // 检查新 GitHub 账号是否已被其他用户认领（事务外提前拦截，避免部分更新）
        com.teammatch.entity.TechProfile existingProfile = techProfileService.findByUsernameAndSource(githubUsername, "github");
        if (existingProfile != null && existingProfile.isClaimed()
                && !existingProfile.getClaimedByUserId().equals(userId)) {
            throw new com.teammatch.exception.ValidationException(ReasonCode.TECH_PROFILE_ALREADY_CLAIMED);
        }

        String oldGithub = user.getGithubUsername();

        // 事务内操作：释放旧技术画像 → 更新用户 → 认领新技术画像
        transactionTemplate.execute(status -> {
            // 删除旧技术画像（老数据已无用，保留最新即可）
            com.teammatch.entity.TechProfile oldProfile = techProfileService.findByUsernameAndSource(oldGithub, "github");
            if (oldProfile != null) {
                techProfileService.deleteTechProfile(oldProfile);
            }

            // 更新用户 GitHub 用户名
            user.setGithubUsername(githubUsername);
            userMapper.updateById(user);

            // 认领新技术画像
            try {
                techProfileService.claimProfile(githubUsername, "github", userId);
                log.info("技术画像换绑成功: userId={}, from={}, to={}", userId, oldGithub, githubUsername);
            } catch (Exception e) {
                log.error("技术画像认领失败: userId={}, githubUsername={}, error={}", userId, githubUsername, e.getMessage());
            }
            return null;
        });

        // 触发异步同步
        triggerAsyncSyncQuietly(githubUsername, "github");
    }

    /**
     * 检查 GitHub 用户名是否存在
     */
    /**
     * 检查 GitHub 用户名是否存在
     * 使用配置了超时的 RestTemplate bean（连接 3s + 读取 5s），
     * 避免 GitHub API 响应慢时挂死 Web 线程池
     */
    private boolean checkGitHubUserExists(String username) {
        try {
            String url = String.format("https://api.github.com/users/%s", username);
            String response = restTemplate.getForObject(url, String.class);
            return response != null && !response.contains("Not Found");
        } catch (Exception e) {
            log.warn("GitHub 用户校验失败: username={}, error={}", username, e.getMessage());
            return false;
        }
    }

    @Override
    public void bindGitee(Long userId, String giteeUsername) {
        // 1. 检查用户是否已绑定 Gitee
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) return;

        if (Boolean.TRUE.equals(currentUser.getGiteeClaimed())) {
            if (giteeUsername.equals(currentUser.getGiteeUsername())) {
                log.info("Gitee 账号未变更，无需重新绑定: userId={}, giteeUsername={}", userId, giteeUsername);
                return;
            }
            throw new com.teammatch.exception.ValidationException(com.teammatch.common.ReasonCode.GITEE_ALREADY_BOUND);
        }

        // 2. 检查 Gitee 用户是否存在
        if (!checkGiteeUserExists(giteeUsername)) {
            throw new com.teammatch.exception.NotFoundException(com.teammatch.common.ReasonCode.GITEE_USER_NOT_FOUND);
        }

        // 2.5 检查该 Gitee 账号是否已被其他用户认领
        com.teammatch.entity.TechProfile existingProfile = techProfileService.findByUsernameAndSource(giteeUsername, "gitee");
        if (existingProfile != null && existingProfile.isClaimed()
                && !existingProfile.getClaimedByUserId().equals(userId)) {
            throw new com.teammatch.exception.ValidationException(com.teammatch.common.ReasonCode.TECH_PROFILE_ALREADY_CLAIMED);
        }

        // 3. 事务内执行数据库操作
        transactionTemplate.execute(status -> {
            User user = userMapper.selectById(userId);
            if (user == null) return null;

            user.setGiteeUsername(giteeUsername);
            user.setGiteeClaimed(true);
            // 切换平台时清除旧平台绑定
            if (Boolean.TRUE.equals(user.getGithubClaimed())) {
                user.setGithubUsername(null);
                user.setGithubClaimed(false);
            }
            userMapper.updateById(user);

            try {
                techProfileService.claimProfile(giteeUsername, "gitee", userId);
                log.info("Gitee 技术画像认领成功: userId={}, giteeUsername={}", userId, giteeUsername);
            } catch (Exception e) {
                log.error("Gitee 技术画像认领失败: userId={}, giteeUsername={}, error={}", userId, giteeUsername, e.getMessage());
            }
            return null;
        });

        // 4. 触发异步同步
        triggerAsyncSyncQuietly(giteeUsername, "gitee");
    }

    @Override
    public void updateGitee(Long userId, String giteeUsername) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        // 检查是否已绑定 Gitee
        if (!Boolean.TRUE.equals(user.getGiteeClaimed())) {
            throw new com.teammatch.exception.ValidationException(com.teammatch.common.ReasonCode.GITEE_NOT_BOUND);
        }

        // 同一账号换绑到同一个 → 无操作
        if (giteeUsername.equals(user.getGiteeUsername())) {
            log.info("Gitee 账号未变更: userId={}, giteeUsername={}", userId, giteeUsername);
            return;
        }

        // 检查新 Gitee 用户是否存在
        if (!checkGiteeUserExists(giteeUsername)) {
            throw new com.teammatch.exception.NotFoundException(com.teammatch.common.ReasonCode.GITEE_USER_NOT_FOUND);
        }

        // 检查新 Gitee 账号是否已被其他用户认领
        com.teammatch.entity.TechProfile existingProfile = techProfileService.findByUsernameAndSource(giteeUsername, "gitee");
        if (existingProfile != null && existingProfile.isClaimed()
                && !existingProfile.getClaimedByUserId().equals(userId)) {
            throw new com.teammatch.exception.ValidationException(com.teammatch.common.ReasonCode.TECH_PROFILE_ALREADY_CLAIMED);
        }

        String oldGitee = user.getGiteeUsername();

        // 事务内操作
        transactionTemplate.execute(status -> {
            com.teammatch.entity.TechProfile oldProfile = techProfileService.findByUsernameAndSource(oldGitee, "gitee");
            if (oldProfile != null) {
                techProfileService.deleteTechProfile(oldProfile);
            }

            user.setGiteeUsername(giteeUsername);
            userMapper.updateById(user);

            try {
                techProfileService.claimProfile(giteeUsername, "gitee", userId);
                log.info("Gitee 技术画像换绑成功: userId={}, from={}, to={}", userId, oldGitee, giteeUsername);
            } catch (Exception e) {
                log.error("Gitee 技术画像认领失败: userId={}, giteeUsername={}, error={}", userId, giteeUsername, e.getMessage());
            }
            return null;
        });

        triggerAsyncSyncQuietly(giteeUsername, "gitee");
    }

    /**
     * 检查 Gitee 用户名是否存在
     */
    private boolean checkGiteeUserExists(String username) {
        try {
            String url = String.format("https://gitee.com/api/v5/users/%s", username);
            String response = restTemplate.getForObject(url, String.class);
            return response != null && !response.contains("Not Found") && !response.contains("\"message\"");
        } catch (Exception e) {
            log.warn("Gitee 用户校验失败: username={}, error={}", username, e.getMessage());
            return false;
        }
    }

    /**
     * 静默触发异步同步
     */
    private void triggerAsyncSyncQuietly(String username, String source) {
        try {
            techProfileService.triggerAsyncSync(username, source);
            log.info("技术画像异步同步已触发: username={}, source={}", username, source);
        } catch (Exception e) {
            log.error("技术画像异步同步触发失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void addSkill(Long userId, Long skillTagId) {
        // 检查技能是否存在
        SkillTag tag = skillTagMapper.selectById(skillTagId);
        if (tag == null || !"active".equals(tag.getStatus())) return;

        // 检查是否已关联
        LambdaQueryWrapper<UserSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkill::getUserId, userId).eq(UserSkill::getSkillTagId, skillTagId);
        if (userSkillMapper.selectCount(wrapper) > 0) return;

        UserSkill userSkill = new UserSkill();
        userSkill.setUserId(userId);
        userSkill.setSkillTagId(skillTagId);
        userSkillMapper.insert(userSkill);
    }

    @Override
    public List<Long> getUserSkills(Long userId) {
        LambdaQueryWrapper<UserSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkill::getUserId, userId);
        List<UserSkill> userSkills = userSkillMapper.selectList(wrapper);
        return userSkills.stream().map(UserSkill::getSkillTagId).collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void addSkillTag(SkillTag skillTag) {
        if (skillTag.getName() == null || skillTag.getName().isEmpty()) {
            throw new ValidationException(ReasonCode.SKILL_TAG_NAME_REQUIRED, "技能标签名称不能为空");
        }
        
        // 检查是否已存在同名同分类技能标签（DB 约束是 (category, name) 组合唯一）
        LambdaQueryWrapper<SkillTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillTag::getName, skillTag.getName());
        if (skillTag.getCategory() != null) {
            wrapper.eq(SkillTag::getCategory, skillTag.getCategory());
        }
        SkillTag existing = skillTagMapper.selectOne(wrapper);
        if (existing != null) {
            throw new DuplicateDataException(ReasonCode.SKILL_TAG_ALREADY_EXISTS);
        }
        
        skillTag.setStatus("active");
        skillTagMapper.insert(skillTag);
    }

    @Override
    @Transactional
    public void updateUserSkills(Long userId, List<Long> skillTagIds) {
        // 【实习要点3】全量替换模式：先删除旧的技能，再插入新的
        
        // 1. 删除用户的所有旧技能
        LambdaQueryWrapper<UserSkill> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(UserSkill::getUserId, userId);
        userSkillMapper.delete(deleteWrapper);
        
        // 2. 如果传入空列表，表示清空所有技能，直接返回
        if (skillTagIds == null || skillTagIds.isEmpty()) {
            return;
        }
        
        // 3. 验证所有技能标签是否存在且为 active 状态
        for (Long skillTagId : skillTagIds) {
            SkillTag tag = skillTagMapper.selectById(skillTagId);
            if (tag == null || !"active".equals(tag.getStatus())) {
                throw new NotFoundException(ReasonCode.SKILL_TAG_NOT_FOUND);
            }
        }
        
        // 4. 批量插入新技能
        for (Long skillTagId : skillTagIds) {
            UserSkill userSkill = new UserSkill();
            userSkill.setUserId(userId);
            userSkill.setSkillTagId(skillTagId);
            userSkillMapper.insert(userSkill);
        }
    }

    @Override
    public List<SkillTag> listActiveSkillTags() {
        // 【实习要点3】预置字典模式：只返回激活状态的技能标签
        LambdaQueryWrapper<SkillTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillTag::getStatus, "active");
        wrapper.orderByAsc(SkillTag::getCategory, SkillTag::getName);
        return skillTagMapper.selectList(wrapper);
    }
}
