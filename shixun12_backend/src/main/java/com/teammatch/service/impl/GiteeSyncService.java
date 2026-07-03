package com.teammatch.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.config.GiteeProperties;
import com.teammatch.entity.TechProfile;
import com.teammatch.mapper.TechProfileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gitee 数据同步服务
 * 通过 Gitee Open API 获取用户数据和技术画像
 */
@Slf4j
@Component
public class GiteeSyncService {

    @Autowired
    private TechProfileMapper techProfileMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GiteeProperties giteeProperties;

    private static final String GITEE_API_USER = "https://gitee.com/api/v5/user?access_token=%s";
    private static final String GITEE_API_USER_PUBLIC = "https://gitee.com/api/v5/users/%s";
    private static final String GITEE_API_REPOS = "https://gitee.com/api/v5/users/%s/repos?page=1&per_page=100";
    private static final String GITEE_API_REPOS_AUTH = "https://gitee.com/api/v5/user/repos?access_token=%s&page=1&per_page=100&type=all";
    private static final String GITEE_API_COMMITS_COUNT = "https://gitee.com/api/v5/repos/%s/commits?per_page=1&page=1";

    /**
     * 异步同步 Gitee 数据
     */
    @Async
    public void syncGiteeDataAsync(Long profileId, String accessToken) {
        log.info("开始异步同步 Gitee 数据: id={}", profileId);

        TechProfile profile = techProfileMapper.selectById(profileId);
        if (profile == null) {
            log.warn("技术画像不存在，无法同步: id={}", profileId);
            return;
        }

        try {
            // 获取 Gitee 用户信息
            Map<String, Object> userData = fetchGiteeUser(accessToken);
            if (userData != null) {
                profile.setAvatarUrl(getString(userData, "avatar_url"));
                profile.setBio(getString(userData, "bio"));
                profile.setGithubUsername(getString(userData, "login"));
            }

            // 获取仓库数据（带 token，可获取私有仓库）
            String username = profile.getGithubUsername();
            List<Map<String, Object>> repos = fetchGiteeReposWithToken(accessToken);
            if (repos != null && !repos.isEmpty()) {
                processRepos(profile, repos);
            } else {
                // 带 token 也拉不到，fallback 到公开 API
                repos = fetchGiteeRepos(username);
                if (repos != null && !repos.isEmpty()) {
                    processRepos(profile, repos);
                }
            }

            profile.computeTechScore();
            profile.setSyncStatus("synced");
            profile.setLastSyncedAt(LocalDateTime.now());
            techProfileMapper.updateById(profile);
            log.info("Gitee 数据同步完成: username={}, techScore={}", profile.getGithubUsername(), profile.getTechScore());
        } catch (Exception e) {
            log.error("Gitee 数据同步失败: id={}, error={}", profileId, e.getMessage());
            profile.setSyncStatus("failed");
            techProfileMapper.updateById(profile);
        }
    }

    /**
     * 异步同步 Gitee 数据（无需 access token，使用公开 API）
     * 用于手动绑定场景
     */
    @Async
    public void syncGiteeDataWithoutToken(Long profileId, String username) {
        log.info("开始异步同步 Gitee 公开数据: id={}, username={}", profileId, username);

        TechProfile profile = techProfileMapper.selectById(profileId);
        if (profile == null) {
            log.warn("技术画像不存在，无法同步: id={}", profileId);
            return;
        }

        try {
            // 通过公开 API 获取用户信息
            Map<String, Object> userData = fetchGiteeUserPublic(username);
            if (userData != null) {
                profile.setAvatarUrl(getString(userData, "avatar_url"));
                profile.setBio(getString(userData, "bio"));
            }

            // 获取仓库数据
            List<Map<String, Object>> repos = fetchGiteeRepos(username);
            if (repos != null && !repos.isEmpty()) {
                int totalStars = 0;
                Map<String, Integer> langCount = new HashMap<>();
                int repoCount = 0;

                for (Map<String, Object> repo : repos) {
                    Boolean isFork = (Boolean) repo.get("fork");
                    if (Boolean.TRUE.equals(isFork)) continue;
                    repoCount++;

                    totalStars += getInt(repo, "stargazers_count");

                    String lang = getString(repo, "language");
                    if (lang != null && !lang.isEmpty()) {
                        langCount.merge(lang, 1, Integer::sum);
                    }
                }

                profile.setTotalStars(totalStars);
                profile.setTotalRepos(repoCount);
                // 公开 API 无法获取真实 commits/PRs，使用估算值
                profile.setTotalCommits(Math.max(1, totalStars * 20 + repoCount * 5));
                profile.setTotalPrs(Math.max(0, repoCount * 2));

                String topLangs = langCount.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(5)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList())
                        .toString();
                profile.setTopLanguages(topLangs);

                // contributions 估算
                profile.setTotalContributions(Math.max(0, totalStars + repoCount * 10));
            } else {
                profile.setTotalStars(0);
                profile.setTotalRepos(getInt(userData, "public_repos"));
                profile.setTotalCommits(1);
                profile.setTotalPrs(0);
                profile.setTotalContributions(0);
                profile.setTopLanguages("[]");
            }

            profile.computeTechScore();
            profile.setSyncStatus("synced");
            profile.setLastSyncedAt(LocalDateTime.now());
            techProfileMapper.updateById(profile);
            log.info("Gitee 公开数据同步完成: username={}, techScore={}", username, profile.getTechScore());
        } catch (Exception e) {
            log.error("Gitee 公开数据同步失败: id={}, error={}", profileId, e.getMessage());
            profile.setSyncStatus("failed");
            techProfileMapper.updateById(profile);
        }
    }

    /**
     * 通过公开 API 获取 Gitee 用户信息（无需 token）
     */
    public Map<String, Object> fetchGiteeUserPublic(String username) {
        try {
            String url = String.format(GITEE_API_USER_PUBLIC, username);
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return null;
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Gitee 公开用户 API 调用失败: username={}, error={}", username, e.getMessage());
            return null;
        }
    }

    /**
     * 通过 OAuth token 获取 Gitee 用户信息
     */
    public Map<String, Object> fetchGiteeUser(String accessToken) {
        try {
            String url = String.format(GITEE_API_USER, accessToken);
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return null;
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Gitee 用户 API 调用失败: error={}", e.getMessage());
            return null;
        }
    }

    /**
     * 通过 token 获取 Gitee 仓库（含私有仓库）
     */
    private List<Map<String, Object>> fetchGiteeReposWithToken(String accessToken) {
        try {
            String url = String.format(GITEE_API_REPOS_AUTH, accessToken);
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return Collections.emptyList();
            return objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Gitee 仓库 API（token）调用失败: error={}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 处理仓库数据，计算技术画像
     */
    private void processRepos(TechProfile profile, List<Map<String, Object>> repos) {
        int totalStars = 0;
        int totalCommits = 0;
        int totalPrs = 0;
        Map<String, Integer> langCount = new HashMap<>();
        int repoCount = 0;

        for (Map<String, Object> repo : repos) {
            Boolean isFork = (Boolean) repo.get("fork");
            if (Boolean.TRUE.equals(isFork)) continue;
            repoCount++;

            totalStars += getInt(repo, "stargazers_count");

            String fullName = getString(repo, "full_name");
            totalCommits += fetchRepoCommitCount(fullName);
            totalPrs += fetchRepoPrCount(fullName);

            String lang = getString(repo, "language");
            if (lang != null && !lang.isEmpty()) {
                langCount.merge(lang, 1, Integer::sum);
            }
        }

        profile.setTotalStars(totalStars);
        profile.setTotalRepos(repoCount);
        profile.setTotalCommits(Math.max(1, totalCommits));
        profile.setTotalPrs(Math.max(0, totalPrs));

        String topLangs = langCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
                .toString();
        profile.setTopLanguages(topLangs);

        profile.setTotalContributions(Math.max(0, totalCommits + totalPrs * 5));
    }

    private List<Map<String, Object>> fetchGiteeRepos(String username) {
        try {
            String url = String.format(GITEE_API_REPOS, username);
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return Collections.emptyList();
            return objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Gitee 仓库 API 调用失败: username={}, error={}", username, e.getMessage());
            return Collections.emptyList();
        }
    }

    private int fetchRepoCommitCount(String fullName) {
        try {
            String url = String.format(GITEE_API_COMMITS_COUNT, fullName);
            String response = restTemplate.getForObject(url, String.class);
            // 通过 Link header 或 total_count 获取总数
            // 简化：返回1表示至少有一次提交
            return response != null && !response.equals("[]") ? 5 : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private int fetchRepoPrCount(String fullName) {
        try {
            String url = String.format("https://gitee.com/api/v5/repos/%s/pulls?state=all&per_page=1&page=1", fullName);
            String response = restTemplate.getForObject(url, String.class);
            return response != null && !response.equals("[]") ? getPrTotal(fullName) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int getPrTotal(String fullName) {
        try {
            String url = String.format("https://gitee.com/api/v5/repos/%s/pulls?state=all&per_page=100&page=1", fullName);
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return 0;
            List<Map<String, Object>> prs = objectMapper.readValue(response,
                    new TypeReference<List<Map<String, Object>>>() {});
            return prs != null ? prs.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
