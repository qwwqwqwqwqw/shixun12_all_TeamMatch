package com.teammatch.m6.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teammatch.m6.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 举报 Mapper 接口
 *
 * 根据详细设计文档 7.5 节定义
 *
 * <p>注意：{@code evidence_urls} 为 JSON 字段，须通过 BaseMapper（selectById/selectList 等）
 * 才能应用 JacksonTypeHandler；下方 {@code @Select("SELECT * ...")} 方法不映射该字段，
 * 业务层已改用 LambdaQueryWrapper 查询带证据的列表。
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    /**
     * 查询用户的举报列表（不含 evidence_urls 映射，请优先使用 BaseMapper.selectList）
     *
     * @param reporterId 举报人ID
     * @return 举报列表
     */
    @Select("SELECT * FROM report WHERE reporter_id = #{reporterId} ORDER BY created_at DESC")
    List<Report> selectByReporterId(Long reporterId);

    /**
     * 查询待处理的举报列表（不含 evidence_urls 映射，请优先使用 BaseMapper.selectList）
     *
     * @return 待处理举报列表
     */
    @Select("SELECT * FROM report WHERE status = 'pending' ORDER BY created_at ASC")
    List<Report> selectPendingReports();

    /**
     * 查询指定状态的举报列表（不含 evidence_urls 映射，请优先使用 BaseMapper.selectList）
     *
     * @param status 状态
     * @return 举报列表
     */
    @Select("SELECT * FROM report WHERE status = #{status} ORDER BY created_at DESC")
    List<Report> selectByStatus(String status);
}
