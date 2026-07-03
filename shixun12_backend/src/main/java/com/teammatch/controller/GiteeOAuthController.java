package com.teammatch.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.Result;
import com.teammatch.config.GiteeProperties;
import com.teammatch.entity.TechProfile;
import com.teammatch.entity.User;
import com.teammatch.mapper.UserMapper;
import com.teammatch.service.TechProfileService;
import com.teammatch.service.impl.GiteeSyncService;
import com.teammatch.util.AuthUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

/**
 * Gitee OAuth 控制器
 * 提供 Gitee 账号的 OAuth 绑定和技术画像生成
 */
@Slf4j
@RestController
@RequestMapping("/profile/gitee")
public class GiteeOAuthController {

    @Autowired
    private GiteeProperties giteeProperties;

    @Autowired
    private GiteeSyncService giteeSyncService;

    @Autowired
    private TechProfileService techProfileService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthUtil authUtil;

    /**
     * Step 1: 跳转 Gitee OAuth 授权页
     * GET /api/profile/gitee/auth
     */
    @GetMapping("/auth")
    public RedirectView authorize(@RequestHeader("Authorization") String token) {
        authUtil.requireUserId(token);

        String url = String.format(
                "https://gitee.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s",
                giteeProperties.getClientId(),
                giteeProperties.getRedirectUri(),
                giteeProperties.getScope(),
                extractUserId(token)
        );
        return new RedirectView(url);
    }

    /**
     * Step 2: OAuth 回调
     * GET /api/profile/gitee/callback?code=xxx&state=userId
     */
    @GetMapping("/callback")
    public RedirectView callback(@RequestParam String code, @RequestParam String state) {
        Long userId = Long.parseLong(state);

        try {
            // 1. 用 code 换取 access_token
            String tokenUrl = String.format(
                    "https://gitee.com/oauth/token?grant_type=authorization_code&code=%s&client_id=%s&client_secret=%s&redirect_uri=%s",
                    code,
                    giteeProperties.getClientId(),
                    giteeProperties.getClientSecret(),
                    giteeProperties.getRedirectUri()
            );
            String tokenResponse = restTemplate.postForObject(tokenUrl, null, String.class);
            Map<String, Object> tokenData = objectMapper.readValue(tokenResponse,
                    new TypeReference<Map<String, Object>>() {});
            String accessToken = (String) tokenData.get("access_token");

            if (accessToken == null) {
                log.error("Gitee OAuth 获取 token 失败: {}", tokenResponse);
                return redirectWithError("Gitee 授权失败");
            }

            // 2. 获取 Gitee 用户信息
            Map<String, Object> userData = giteeSyncService.fetchGiteeUser(accessToken);
            if (userData == null) {
                return redirectWithError("Gitee 用户信息获取失败");
            }
            String giteeUsername = (String) userData.get("login");

            // 3. 创建/认领技术画像
            TechProfile profile = claimOrCreateGiteeProfile(giteeUsername, userId);

            // 4. 更新 user 表
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setGiteeUsername(giteeUsername);
                user.setGiteeClaimed(true);
                user.setTechProfileId(profile.getId());
                userMapper.updateById(user);
            }

            // 5. 异步同步 Gitee 数据
            giteeSyncService.syncGiteeDataAsync(profile.getId(), accessToken);

            log.info("Gitee OAuth 绑定成功: userId={}, giteeUsername={}", userId, giteeUsername);
        } catch (Exception e) {
            log.error("Gitee OAuth 回调失败: userId={}, error={}", userId, e.getMessage());
            return redirectWithError("Gitee 绑定失败: " + e.getMessage());
        }

        // 回到前端（统一跳转小程序兼容成功页）
        return new RedirectView(giteeProperties.getPublicBaseUrl() + "/api/profile/gitee/mp-success");
    }

    /**
     * Step 0: 获取 OAuth 授权 URL（小程序用）
     * GET /api/profile/gitee/auth-url
     * 返回 JSON，前端拿到 url 后用 <web-view> 打开
     */
    @GetMapping("/auth-url")
    public Result<Map<String, String>> getAuthUrl(@RequestHeader("Authorization") String token) {
        Long userId = authUtil.requireUserId(token);

        String url = String.format(
                "https://gitee.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s",
                giteeProperties.getClientId(),
                giteeProperties.getRedirectUri(),
                giteeProperties.getScope(),
                userId
        );

        Map<String, String> data = new HashMap<>();
        data.put("authUrl", url);
        data.put("state", userId.toString());
        return Result.success(data);
    }

    /**
     * 绑定成功页面（微信小程序 WebView 版）
     * GET /api/profile/gitee/mp-success
     * 通过 wx.miniProgram.postMessage 通知小程序授权完成
     */
    @GetMapping("/mp-success")
    public String mpSuccess() {
        // 兼容浏览器和小程序 WebView 两种场景
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">"
                + "<title>Gitee 授权成功</title>"
                + "<style>"
                + "*{margin:0;padding:0;box-sizing:border-box}"
                + "body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:#f5f5f5;display:flex;align-items:center;justify-content:center;min-height:100vh}"
                + ".card{background:#fff;border-radius:16px;padding:48px 32px;text-align:center;box-shadow:0 2px 12px rgba(0,0,0,0.08);max-width:320px;width:90%}"
                + ".icon{font-size:64px;margin-bottom:16px}"
                + "h2{font-size:22px;color:#333;margin-bottom:8px}"
                + "p{font-size:14px;color:#999;margin-bottom:24px;line-height:1.6}"
                + ".btn{display:inline-block;background:#c71d23;color:#fff;border:none;padding:12px 32px;border-radius:8px;font-size:16px;text-decoration:none;cursor:pointer}"
                + ".btn:active{opacity:0.8}"
                + "</style></head><body>"
                + "<div class=\"card\">"
                + "<div class=\"icon\">✅</div>"
                + "<h2>Gitee 授权成功</h2>"
                + "<p>数据正在同步中，请返回<br><b>TeamMatch 小程序</b><br>进入个人档案页查看技术画像</p>"
                + "<a class=\"btn\" href=\"javascript:history.back()\">返回</a>"
                + "</div>"
                + "<script>"
                + "try{"
                + "  wx&&wx.miniProgram&&wx.miniProgram.navigateBack({delta:1});"
                + "}catch(e){}"
                + "</script>"
                + "</body></html>";
    }

    /**
     * 绑定成功页面（浏览器版）
     */
    @GetMapping("/success")
    public String success() {
        return "<html><body><script>window.opener.postMessage('gitee-auth-success','*');window.close();</script><h3>Gitee 绑定成功，正在同步数据...</h3></body></html>";
    }

    private TechProfile claimOrCreateGiteeProfile(String giteeUsername, Long userId) {
        return techProfileService.claimProfile(giteeUsername, "gitee", userId);
    }

    private RedirectView redirectWithError(String msg) {
        return new RedirectView(giteeProperties.getPublicBaseUrl() + "?error=" + msg);
    }

    private String extractUserId(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return authUtil.requireUserId("Bearer " + token).toString();
    }
}
