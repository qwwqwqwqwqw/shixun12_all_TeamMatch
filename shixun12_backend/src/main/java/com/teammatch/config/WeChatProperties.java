package com.teammatch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置属性
 * 绑定 application.yml 中的 wechat.miniprogram.* 配置
 *
 * 使用方式：
 * - 通过环境变量 WECHAT_APP_ID、WECHAT_SECRET 注入
 * - 或在 application-local.yml 中直接填写（仅开发环境）
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.miniprogram")
public class WeChatProperties {

    /** 微信小程序的 AppID */
    private String appId;

    /** 微信小程序的 AppSecret */
    private String secret;
}
