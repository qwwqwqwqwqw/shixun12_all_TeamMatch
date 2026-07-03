package com.teammatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置
 * 用于调用外部 HTTP 接口（如微信 code2session API）
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时 3 秒：避免微信接口响应慢时长时间阻塞 Tomcat 线程池
        factory.setConnectTimeout(3000);
        // 读取超时 5 秒：微信接口数据量大或网络抖动时尽快熔断
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}
