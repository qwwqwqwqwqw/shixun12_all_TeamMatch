package com.teammatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teammatch.entity.Appeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 申诉数据访问层
 * M5-7 只需 selectById，继承 BaseMapper 自动提供
 * 完整 Mapper 方法由 M6 扩展
 *
 * <p>注意：{@code evidence_urls} 为 JSON 字段，须通过 BaseMapper（selectById/selectList 等）
 * 才能应用 JacksonTypeHandler；下方 {@code @Select("SELECT * ...")} 方法不映射该字段，
 * 业务层已改用 LambdaQueryWrapper 查询带证据的列表。
 */
@Mapper
public interface AppealMapper extends BaseMapper<Appeal> {

    /**
     * 查询用户的申诉列表（不含 evidence_urls 映射，请优先使用 BaseMapper.selectList）
     */
    @Select("SELECT * FROM appeal WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Appeal> selectByUserId(@Param("userId") Long userId);

    /**
     * 按状态查询申诉列表（不含 evidence_urls 映射，请优先使用 BaseMapper.selectList）
     */
    @Select("SELECT * FROM appeal WHERE status = #{status} ORDER BY created_at DESC")
    List<Appeal> selectByStatus(@Param("status") String status);

    /**
     * 按目标类型查询申诉列表（不含 evidence_urls 映射，请优先使用 BaseMapper.selectList）
     */
    @Select("SELECT * FROM appeal WHERE target_type = #{targetType} ORDER BY created_at DESC")
    List<Appeal> selectByTargetType(@Param("targetType") String targetType);

    /**
     * 查询指定目标的待处理申诉（不含 evidence_urls 映射，请优先使用 BaseMapper.selectOne）
     */
    @Select("SELECT * FROM appeal WHERE target_type = #{targetType} AND target_id = #{targetId} AND status = 'pending' LIMIT 1")
    Appeal selectPendingByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    /**
     * 查询用户对指定目标是否已有申诉（仅查存在性字段，不含 evidence_urls）
     */
    @Select("SELECT id, user_id, target_type, target_id, status FROM appeal "
            + "WHERE target_type = #{targetType} AND target_id = #{targetId} AND user_id = #{userId} LIMIT 1")
    Appeal selectByTargetAndUser(@Param("targetType") String targetType,
                                 @Param("targetId") Long targetId,
                                 @Param("userId") Long userId);
}
