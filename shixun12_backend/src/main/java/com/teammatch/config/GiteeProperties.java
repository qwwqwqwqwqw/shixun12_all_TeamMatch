package com.teammatch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gitee OAuth 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "gitee.oauth")
public class GiteeProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope;
    private String publicBaseUrl;
}
