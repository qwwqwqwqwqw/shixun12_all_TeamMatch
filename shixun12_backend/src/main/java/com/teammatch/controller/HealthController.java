package com.teammatch.controller;

import com.teammatch.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
public class HealthController {

    /**
     * 健康检查端点
     * GET /api/health
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now().toString());
        healthInfo.put("service", "TeamMatch Backend");
        healthInfo.put("version", "1.0.0-SNAPSHOT");
        
        return Result.success(healthInfo);
    }
}
