package com.teammatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.dto.LeaderboardEntryVO;
import com.teammatch.dto.TechProfileVO;
import com.teammatch.entity.TechProfile;
import com.teammatch.entity.User;
import com.teammatch.mapper.TechProfileMapper;
import com.teammatch.mapper.UserMapper;

import com.teammatch.service.TechProfileService;
import com.teammatch.service.storage.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;


import java.util.*;

/**
 * M3 技术画像服务实现
 * 通过 GitHub REST API (unauthenticated) 获取用户公开仓库数据，
 * 计算综合技术评分，支撑冷启动排行榜。
 */
@Slf4j
@Service
public class TechProfileServiceImpl implements TechProfileService {

    @Autowired
    private TechProfileMapper techProfileMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OssService ossService;

    @Autowired
    private GitHubSyncService gitHubSyncService;

    @Autowired
    private GiteeSyncService giteeSyncService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public TechProfile generateOrUpdateProfile(String username, String source) {
        // 1. 查询是否已存在（按 username + source 联合查询）
        LambdaQueryWrapper<TechProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TechProfile::getGithubUsername, username)
               .eq(TechProfile::getSource, source != null ? source : "github");
        TechProfile existing = techProfileMapper.selectOne(wrapper);

        if (existing != null) {
            return existing;
        }

        // 2. 新记录：先创建占位数据，避免并发重复创建
        TechProfile profile = new TechProfile();
        profile.setGithubUsername(username);
        profile.setSource(source != null ? source : "github");
        profile.setSyncStatus("pending");
        profile.setTotalStars(0);
        profile.setTotalRepos(0);
        profile.setTotalCommits(0);
        profile.setTotalPrs(0);
        profile.setTotalContributions(0);
        profile.setTopLanguages("[]");
        profile.setTechScore(0);
        profile.setLastSyncedAt(null);

        try {
            techProfileMapper.insert(profile);
            log.info("创建技术画像占位: username={}, source={}, id={}", username, source, profile.getId());

            // 同步在 triggerAsyncSync 中触发（由调用方在事务外调用）
        } catch (DuplicateKeyException e) {
            // 并发下另一个线程已创建，查询已存在的记录
            LambdaQueryWrapper<TechProfile> retryWrapper = new LambdaQueryWrapper<>();
            retryWrapper.eq(TechProfile::getGithubUsername, username)
                        .eq(TechProfile::getSource, source != null ? source : "github");
            profile = techProfileMapper.selectOne(retryWrapper);
            log.info("技术画像已存在（并发场景）: username={}, source={}", username, source);
        }

        return profile;
    }

    // syncGitHubDataAsync 已移至 GitHubSyncService，确保 @Async 通过 AOP 代理生效

    @Override
    @Transactional
    public TechProfile claimProfile(String username, String source, Long userId) {
        // 1. 查找或生成技术画像
        LambdaQueryWrapper<TechProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TechProfile::getGithubUsername, username)
               .eq(TechProfile::getSource, source != null ? source : "github");
        TechProfile profile = techProfileMapper.selectOne(wrapper);

        if (profile == null) {
            // 没有则生成
            profile = generateOrUpdateProfile(username, source);
        }

        // 2. 检查是否已被其他人认领
        if (profile.isClaimed() && !profile.getClaimedByUserId().equals(userId)) {
            throw new RuntimeException("该" + source + " 账号已被其他用户认领");
        }

        // 3. 认领
        if (!profile.isClaimed()) {
            profile.setClaimedByUserId(userId);
            techProfileMapper.updateById(profile);
        }

        // 4. 更新 user 表的 tech_profile_id 外键
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setTechProfileId(profile.getId());
            userMapper.updateById(user);
        }

        return profile;
    }

    @Override
    public TechProfileVO getProfileByUserId(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getTechProfileId() == null) return null;

        TechProfile profile = techProfileMapper.selectById(user.getTechProfileId());
        if (profile == null) return null;

        TechProfileVO vo = TechProfileVO.from(profile);
        vo.setClaimedByUserNickname(user.getNickname());
        return vo;
    }

    @Override
    public List<LeaderboardEntryVO> getLeaderboard(int page, int size) {
        // 按 tech_score 降序分页查询
        LambdaQueryWrapper<TechProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(TechProfile::getTechScore);

        Page<TechProfile> pageObj = new Page<>(page, size);
        Page<TechProfile> result = techProfileMapper.selectPage(pageObj, wrapper);

        List<TechProfile> profiles = result.getRecords();

        // 构建排行榜，附加上用户信息
        int rankBase = (page - 1) * size;
        List<LeaderboardEntryVO> entries = new ArrayList<>();
        for (int i = 0; i < profiles.size(); i++) {
            TechProfile profile = profiles.get(i);
            LeaderboardEntryVO entry = new LeaderboardEntryVO();
            entry.setRank(rankBase + i + 1);
            entry.setGithubUsername(profile.getGithubUsername());
            entry.setTechScore(profile.getTechScore());
            entry.setTotalStars(profile.getTotalStars());
            entry.setTotalRepos(profile.getTotalRepos());
            entry.setTotalCommits(profile.getTotalCommits());
            entry.setTotalPrs(profile.getTotalPrs());
            entry.setTotalContributions(profile.getTotalContributions());
            entry.setTopLanguages(profile.getTopLanguages());
            entry.setBio(profile.getBio());
            entry.setSource(profile.getSource() != null ? profile.getSource() : "github");
            entry.setClaimed(profile.isClaimed());

            // 如果已认领，附加用户信息
            if (profile.isClaimed()) {
                User user = userMapper.selectById(profile.getClaimedByUserId());
                if (user != null) {
                    entry.setUserId(user.getId());
                    entry.setNickname(user.getNickname());
                    entry.setAvatarUrl(ossService.resolveAvatarUrl(user.getAvatarUrl()));
                    entry.setSchool(user.getSchool());
                }
            }

            entries.add(entry);
        }

        return entries;
    }

    @Override
    public long getLeaderboardCount() {
        return techProfileMapper.selectCount(null);
    }


    /**
     * 事务提交后触发异步同步（由调用方确保事务已提交）
     * 此方法无 @Transactional，保证 @Async 在事务外执行
     */
    @Override
    public com.teammatch.entity.TechProfile findByUsernameAndSource(String username, String source) {
        LambdaQueryWrapper<com.teammatch.entity.TechProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.teammatch.entity.TechProfile::getGithubUsername, username)
               .eq(com.teammatch.entity.TechProfile::getSource, source != null ? source : "github");
        return techProfileMapper.selectOne(wrapper);
    }

    @Override
    public void updateTechProfile(com.teammatch.entity.TechProfile profile) {
        techProfileMapper.updateById(profile);
    }

    @Override
    public void deleteTechProfile(com.teammatch.entity.TechProfile profile) {
        techProfileMapper.deleteById(profile.getId());
    }

    @Override
    public void triggerAsyncSync(String username, String source) {
        LambdaQueryWrapper<TechProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TechProfile::getGithubUsername, username)
               .eq(TechProfile::getSource, source != null ? source : "github");
        TechProfile profile = techProfileMapper.selectOne(wrapper);
        if (profile != null) {
            profile.setSyncStatus("pending");
            techProfileMapper.updateById(profile);
            log.info("触发技术画像异步同步: username={}, source={}", username, source);
            if ("gitee".equals(source)) {
                // 使用公开 API 同步 Gitee 数据（无需 access token）
                giteeSyncService.syncGiteeDataWithoutToken(profile.getId(), username);
            } else {
                gitHubSyncService.syncGitHubDataAsync(profile.getId(), username);
            }
        }
    }
}