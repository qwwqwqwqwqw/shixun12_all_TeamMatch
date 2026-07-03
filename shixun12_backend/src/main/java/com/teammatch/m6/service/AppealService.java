package com.teammatch.m6.service;

import com.teammatch.entity.Appeal;
import com.teammatch.m6.dto.AppealCreateDTO;
import com.teammatch.m6.dto.AppealHandleDTO;
import com.teammatch.m6.dto.AppealableEvaluationVO;
import com.teammatch.m6.dto.AppealablePenaltyVO;

import java.util.List;

/**
 * 申诉 Service 接口
 *
 * 根据详细设计文档 7.5 节定义
 */
public interface AppealService {

    /**
     * 提交申诉
     *
     * @param userId 申诉用户ID
     * @param dto 申诉请求
     * @return 创建的申诉记录
     * @throws IllegalArgumentException 如果参数无效或目标不存在
     * @throws IllegalStateException 如果已存在待处理的申诉
     */
    Appeal createAppeal(Long userId, AppealCreateDTO dto);

    /**
     * 获取用户的申诉列表
     *
     * @param userId 用户ID
     * @return 申诉列表，按时间倒序
     */
    List<Appeal> getMyAppeals(Long userId);

    /**
     * 获取申诉列表（管理端）
     *
     * @param status 状态筛选，null表示全部
     * @param targetType 目标类型筛选，null表示全部
     * @return 申诉列表
     */
    List<Appeal> getAppealList(String status, String targetType);

    /**
     * 处理申诉（管理端）
     *
     * @param appealId 申诉ID
     * @param handlerId 处理人ID（管理员）
     * @param dto 处理请求
     * @return 处理后的申诉记录
     * @throws IllegalArgumentException 如果申诉不存在
     * @throws IllegalStateException 如果申诉已处理
     */
    Appeal handleAppeal(Long appealId, Long handlerId, AppealHandleDTO dto);

    /**
     * 获取申诉详情
     *
     * @param appealId 申诉ID
     * @return 申诉记录，不存在返回null
     */
    Appeal getAppealById(Long appealId);

    /**
     * 获取指定目标的待处理申诉
     *
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @return 待处理申诉，不存在返回null
     */
    Appeal getPendingAppeal(String targetType, Long targetId);

    /**
     * 申诉页：可申诉的评价列表（status=normal 且未提交过申诉，含项目标题）
     */
    List<AppealableEvaluationVO> listAppealableEvaluations(Long userId);

    /**
     * 申诉页：可申诉的生效中处罚列表（active 且未提交过申诉）
     */
    List<AppealablePenaltyVO> listAppealablePenalties(Long userId);
}
