package com.teammatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务配置
 * 启用 Spring @Async 支持，用于技术画像的 GitHub 数据异步同步
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}