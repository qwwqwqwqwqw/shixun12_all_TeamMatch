package com.teammatch.m4.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teammatch.m4.dto.ProjectCreateDTO;
import com.teammatch.m4.entity.Project;

public interface ProjectService extends IService<Project> {
    
    /**
     * T-113 创建/发布项目
     */
    Project createProject(ProjectCreateDTO dto);

    /**
     * T-117 项目进入进行中
     */
    void startProject(Long projectId, Long operatorId);

    /**
     * T-118 队长结束项目: in_progress -> ended，写 endedAt、evalDeadline
     */
    void endProject(Long projectId, Long operatorId);

    /**
     * T-119 互评窗口懒检查：如果 evalDeadline 已过期则切换为 eval_closed，返回最新状态
     */
    String checkAndCloseEvalWindow(Long projectId);
}
