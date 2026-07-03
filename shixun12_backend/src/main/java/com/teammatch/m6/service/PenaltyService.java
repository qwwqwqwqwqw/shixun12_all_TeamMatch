package com.teammatch.m6.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teammatch.m6.dto.PenaltyCreateDTO;
import com.teammatch.m6.dto.PenaltyRevokeDTO;
import com.teammatch.m6.entity.Penalty;

import java.util.List;

/**
 * 处罚记录 Service 接口
 *
 * 根据详细设计文档 7.2 节定义
 */
public interface PenaltyService extends IService<Penalty> {

    /**
     * 创建处罚（管理员）
     *
     * @param adminId 执行处罚的管理员ID
     * @param dto 处罚请求
     * @return 创建的处罚记录
     * @throws com.teammatch.exception.BusinessException 若管理员尝试处罚自己
     */
    Penalty createPenalty(Long adminId, PenaltyCreateDTO dto);

    /**
     * 撤销处罚（管理员，申诉通过时使用）
     *
     * @param penaltyId 处罚记录ID
     * @param adminId 执行撤销的管理员ID
     * @param dto 撤销请求
     * @return 撤销后的处罚记录
     * @throws IllegalArgumentException 如果处罚不存在
     * @throws IllegalStateException 如果处罚已撤销
     */
    Penalty revokePenalty(Long penaltyId, Long adminId, PenaltyRevokeDTO dto);

    /**
     * 获取用户的处罚记录列表
     *
     * @param userId 用户ID
     * @return 处罚记录列表
     */
    List<Penalty> getUserPenalties(Long userId);

    /**
     * 获取用户生效中的处罚记录
     *
     * @param userId 用户ID
     * @return 生效中的处罚记录列表
     */
    List<Penalty> getUserActivePenalties(Long userId);

    /**
     * 获取处罚记录列表（管理端）
     *
     * @param status 状态筛选，null表示全部
     * @param type 类型筛选，null表示全部
     * @return 处罚记录列表
     */
    List<Penalty> getPenaltyList(String status, String type);

    /**
     * 获取处罚详情
     *
     * @param penaltyId 处罚记录ID
     * @return 处罚记录，不存在返回null
     */
    Penalty getPenaltyById(Long penaltyId);
}
