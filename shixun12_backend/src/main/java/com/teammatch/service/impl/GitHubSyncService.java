package com.teammatch.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * GitHub 数据同步服务
 *
 * 单独抽取为独立 Bean，确保 @Async 通过 Spring AOP 代理生效。
 * 若在 TechProfileServiceImpl 内部直接调用 @Async 方法，
 * Spring 代理不会拦截同类内部调用，异步不会生效。
 */
@Slf4j
@Component
public class GitHubSyncService {

    @Autowired
    private TechProfileMapper techProfileMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String GITHUB_API_USER = "https://api.github.com/users/%s";
    private static final String GITHUB_API_REPOS = "https://api.github.com/users/%s/repos?per_page=100&sort=pushed";

    /**
     * 检查 GitHub 用户名是否存在
     */
    public boolean checkUserExists(String username) {
        try {
            String url = String.format(GITHUB_API_USER, username);
            String responseBody = restTemplate.getForObject(url, String.class);
            return responseBody != null && !responseBody.contains("Not Found");
        } catch (Exception e) {
            log.warn("GitHub 用户检查失败: username={}, error={}", username, e.getMessage());
            return false;
        }
    }

    /**
     * 异步同步 GitHub 数据
     */
    @Async
    public void syncGitHubDataAsync(Long profileId, String githubUsername) {
        log.info("开始异步同步 GitHub 数据: id={}, username={}", profileId, githubUsername);

        TechProfile profile = techProfileMapper.selectById(profileId);
        if (profile == null) {
            log.warn("技术画像不存在，无法同步: id={}", profileId);
            return;
        }

        try {
            fetchGitHubData(profile);
            profile.computeTechScore();
            profile.setSyncStatus("synced");
            profile.setLastSyncedAt(LocalDateTime.now());
            techProfileMapper.updateById(profile);
            log.info("GitHub 数据同步完成: username={}, techScore={}", githubUsername, profile.getTechScore());
        } catch (Exception e) {
            log.error("GitHub 数据同步失败: username={}, error={}", githubUsername, e.getMessage());
            profile.setSyncStatus("failed");
            techProfileMapper.updateById(profile);
        }
    }

    // ==================== 私有方法 ====================

    private void fetchGitHubData(TechProfile profile) {
        String username = profile.getGithubUsername();

        Map<String, Object> userData = fetchGitHubUser(username);
        if (userData != null) {
            profile.setTotalRepos(getInt(userData, "public_repos"));
            profile.setBio(getString(userData, "bio"));
            profile.setAvatarUrl(getString(userData, "avatar_url"));
        }

        List<Map<String, Object>> repos = fetchGitHubRepos(username);
        if (repos != null && !repos.isEmpty()) {
            int totalStars = 0;
            int totalForks = 0;
            Map<String, Integer> langCount = new HashMap<>();

            for (Map<String, Object> repo : repos) {
                Boolean isFork = (Boolean) repo.get("fork");
                if (Boolean.TRUE.equals(isFork)) continue;
                totalStars += getInt(repo, "stargazers_count");
                totalForks += getInt(repo, "forks_count");
                String lang = getString(repo, "language");
                if (lang != null && !lang.isEmpty()) {
                    langCount.merge(lang, 1, Integer::sum);
                }
            }

            profile.setTotalStars(totalStars);
            profile.setTotalCommits(Math.max(1, totalStars * 20 + totalForks * 10));
            profile.setTotalPrs(Math.max(0, repos.size() * 2));

            String topLangs = langCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList())
                    .toString();
            profile.setTopLanguages(topLangs);
        }

        profile.setTotalContributions(
                Math.max(0,
                        (profile.getTotalStars() != null ? profile.getTotalStars() : 0)
                                + (profile.getTotalRepos() != null ? profile.getTotalRepos() : 0) * 10
                )
        );
    }

    private Map<String, Object> fetchGitHubUser(String username) {
        try {
            String url = String.format(GITHUB_API_USER, username);
            String responseBody = restTemplate.getForObject(url, String.class);
            if (responseBody == null) return null;
            return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("GitHub 用户 API 调用失败: username={}, error={}", username, e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> fetchGitHubRepos(String username) {
        try {
            String url = String.format(GITHUB_API_REPOS, username);
            String responseBody = restTemplate.getForObject(url, String.class);
            if (responseBody == null) return Collections.emptyList();
            return objectMapper.readValue(responseBody, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("GitHub 仓库 API 调用失败: username={}, error={}", username, e.getMessage());
            return Collections.emptyList();
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
