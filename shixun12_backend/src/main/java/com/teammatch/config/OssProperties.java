package com.teammatch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置
 * 通过环境变量 OSS_* 或 application-local.yml 注入
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucket;
    private String baseUrl;
    private int presignExpireHours = 24;

    public boolean isConfigured() {
        return endpoint != null && !endpoint.isBlank()
                && accessKeyId != null && !accessKeyId.isBlank()
                && accessKeySecret != null && !accessKeySecret.isBlank()
                && bucket != null && !bucket.isBlank()
                && baseUrl != null && !baseUrl.isBlank();
    }

    public String normalizedBaseUrl() {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
