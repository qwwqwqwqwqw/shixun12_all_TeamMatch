package com.teammatch.m6.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teammatch.m6.dto.ReportCreateDTO;
import com.teammatch.m6.dto.ReportHandleDTO;
import com.teammatch.m6.entity.Report;

import java.util.List;

/**
 * 举报 Service 接口
 *
 * 根据详细设计文档 7.2 节定义
 */
public interface ReportService extends IService<Report> {

    /**
     * 提交举报
     *
     * @param reporterId 举报人ID
     * @param dto 举报请求
     * @return 创建的举报记录
     */
    Report createReport(Long reporterId, ReportCreateDTO dto);

    /**
     * 获取用户的举报列表
     *
     * @param reporterId 举报人ID
     * @return 举报列表
     */
    List<Report> getMyReports(Long reporterId);

    /**
     * 获取举报列表（管理端）
     *
     * @param status 状态筛选，null表示全部
     * @return 举报列表
     */
    List<Report> getReportList(String status);

    /**
     * 处理举报（管理端）
     *
     * @param reportId 举报ID
     * @param handlerId 处理人ID（管理员）
     * @param dto 处理请求
     * @return 处理后的举报记录
     * @throws IllegalArgumentException 如果举报不存在或状态不是待处理
     */
    Report handleReport(Long reportId, Long handlerId, ReportHandleDTO dto);

    /**
     * 获取举报详情
     *
     * @param reportId 举报ID
     * @return 举报记录，不存在返回null
     */
    Report getReportById(Long reportId);
}
