package com.teammatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teammatch.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 项目数据访问层
 * 用于 M5-1A 项目级互评入口判断
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    // 继承 BaseMapper 提供的基础 CRUD 方法：
    // - selectById(Long id) - 根据 ID 查询项目
    // - selectOne(Wrapper<Project> queryWrapper) - 根据条件查询单个项目
    // - selectList(Wrapper<Project> queryWrapper) - 根据条件查询项目列表
    // - insert(Project project) - 插入项目
    // - updateById(Project project) - 根据 ID 更新项目
    // - deleteById(Long id) - 根据 ID 删除项目

    // M5-1A 项目级互评入口判断需要的查询：
    // 1. 根据 projectId 查询项目信息（使用 selectById）
    // 2. 检查项目状态是否为 "ended"（在 Service 层判断）
    // 3. 检查当前时间是否在 evalDeadline 之前（在 Service 层判断）

    /**
     * 互评窗口过期时执行项目状态懒关闭。
     * 仅允许 ended -> eval_closed，避免覆盖其他并发状态变更。
     */
    @Update("UPDATE project SET status = 'eval_closed' WHERE id = #{id} AND status = 'ended'")
    int updateStatusToEvalClosed(@Param("id") Long id);
}
