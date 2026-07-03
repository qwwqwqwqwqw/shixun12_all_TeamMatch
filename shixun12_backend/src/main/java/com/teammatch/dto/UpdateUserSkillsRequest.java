package com.teammatch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 更新用户技能请求 DTO（全量替换）
 * 
 * 【实习要点3】为什么采用全量替换而不是增量添加？
 * 1. 简化前端逻辑：前端只需提交最终状态，不需要计算差异
 * 2. 减少网络请求：一次调用完成所有技能更新
 * 3. 避免并发问题：多次增量添加可能导致数据不一致
 * 4. 符合 RESTful 设计：PUT /skills 表示资源的完整替换
 * 
 * 面试时可以这样说：
 * "我采用了全量替换模式来更新用户技能，这样设计的好处是：
 *  - 降低了前后端的耦合度，前端只需要关心最终状态
 *  - 减少了数据库事务的复杂度，避免了多次插入/删除操作
 *  - 更符合声明式编程的思想，提升了代码的可维护性"
 */
@Data
public class UpdateUserSkillsRequest {

    /**
     * 技能标签ID列表（全量替换）
     * 传空数组 [] 表示清空所有技能；null 由 @NotNull 拦截为参数错误
     */
    @NotNull(message = "skillTagIds 不能为 null")
    private List<Long> skillTagIds;
}
