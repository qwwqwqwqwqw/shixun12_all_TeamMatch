package com.teammatch.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OSS 客户端配置：仅在 access-key-id 非空时创建 Bean
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OssConfig {

    private final OssProperties ossProperties;
    private OSS ossClient;

    @Bean
    @ConditionalOnExpression("'${aliyun.oss.access-key-id:}'.trim().length() > 0 "
            + "&& '${aliyun.oss.access-key-secret:}'.trim().length() > 0")
    public OSS ossClient() {
        ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
        log.info("阿里云 OSS 客户端已初始化: bucket={}, endpoint={}",
                ossProperties.getBucket(), ossProperties.getEndpoint());
        return ossClient;
    }

    @PreDestroy
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
