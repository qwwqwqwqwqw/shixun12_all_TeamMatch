package com.teammatch.service;

import com.teammatch.entity.SkillTag;
import com.teammatch.entity.User;
import java.util.List;

/**
 * M3 档案服务接口
 */
public interface ProfileService {

    /**
     * 根据 ID 获取用户档案
     */
    User getProfileById(Long userId);

    /**
     * 更新用户档案
     */
    void updateProfile(Long userId, User profile);

    /**
     * 绑定 GitHub 用户名
     */
    void bindGithub(Long userId, String githubUsername);

    /**
     * 更新 GitHub 用户名（仅当已经绑定过时可修改）
     */
    void updateGithub(Long userId, String githubUsername);

    /**
     * 绑定 Gitee 用户名（手动绑定，非 OAuth 流程）
     */
    void bindGitee(Long userId, String giteeUsername);

    /**
     * 更新 Gitee 用户名（仅当已经绑定过时可修改）
     */
    void updateGitee(Long userId, String giteeUsername);

    /**
     * 为用户添加技能标签
     */
    void addSkill(Long userId, Long skillTagId);

    /**
     * 获取用户的技能标签 ID 列表
     */
    List<Long> getUserSkills(Long userId);

    /**
     * 添加新的技能标签到数据库
     */
    void addSkillTag(SkillTag skillTag);

    /**
     * 全量替换用户技能（先删除旧的，再插入新的）
     * 【实习要点3】全量替换模式：简化前端逻辑，减少网络请求
     */
    void updateUserSkills(Long userId, List<Long> skillTagIds);

    /**
     * 获取所有激活的技能标签列表
     * 【实习要点3】预置字典模式：提供可选的技能标签供前端展示
     */
    List<SkillTag> listActiveSkillTags();
}
