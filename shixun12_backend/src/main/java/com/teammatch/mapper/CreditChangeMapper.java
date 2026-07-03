package com.teammatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teammatch.entity.CreditChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CreditChangeMapper extends BaseMapper<CreditChange> {

    @Select("<script>" +
            "SELECT * FROM credit_change WHERE source_type = #{sourceType} AND source_id IN " +
            "<foreach collection='sourceIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND change_type = 'evaluation' AND effective = 1" +
            "</script>")
    List<CreditChange> findBySourceTypeAndSourceIds(@Param("sourceType") String sourceType,
                                                     @Param("sourceIds") List<Long> sourceIds);

    @Update("<script>" +
            "UPDATE credit_change SET effective = #{effective}, updated_at = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateEffective(@Param("ids") List<Long> ids, @Param("effective") Boolean effective);

    /**
     * 查询单条 suspended credit_change（限定完整条件）
     * 用于 M5-6 评价复核：确认 evaluation 对应的 credit_change 存在且可操作
     */
    @Select("SELECT * FROM credit_change WHERE source_type = #{sourceType} AND source_id = #{sourceId} " +
            "AND change_type = #{changeType} AND user_id = #{userId} AND effective = 0 LIMIT 1")
    CreditChange findSuspendedOne(@Param("sourceType") String sourceType,
                                  @Param("sourceId") Long sourceId,
                                  @Param("changeType") String changeType,
                                  @Param("userId") Long userId);

    /**
     * 更新单条 credit_change 的 description（审计用途）
     * 用于 M5-6 评价复核 keep_no_credit：标记流水终态原因
     */
    @Update("UPDATE credit_change SET description = #{description}, updated_at = NOW() WHERE id = #{id}")
    int updateDescription(@Param("id") Long id, @Param("description") String description);

    /**
     * 原子性翻转 effective 0→1（完整条件守卫）
     * 防止并发 approve 重复生效同一流水
     */
    @Update("UPDATE credit_change SET effective = 1, updated_at = NOW() " +
            "WHERE id = #{id} AND source_type = #{sourceType} AND source_id = #{sourceId} " +
            "AND change_type = #{changeType} AND user_id = #{userId} AND effective = 0")
    int updateEffectiveToTrue(@Param("id") Long id,
                              @Param("sourceType") String sourceType,
                              @Param("sourceId") Long sourceId,
                              @Param("changeType") String changeType,
                              @Param("userId") Long userId);

    /**
     * 查询原评价信用流水（按精确四维键）
     * M5-7 申诉恢复步骤5: 不预判 effective，由业务代码判断
     */
    @Select("SELECT * FROM credit_change WHERE source_type = #{sourceType} " +
            "AND source_id = #{sourceId} AND change_type = #{changeType} " +
            "AND user_id = #{userId} LIMIT 1")
    CreditChange findBySourceTypeAndSourceIdAndChangeType(
            @Param("sourceType") String sourceType,
            @Param("sourceId") Long sourceId,
            @Param("changeType") String changeType,
            @Param("userId") Long userId);

    /**
     * 查询 appeal_restore 流水是否已存在（幂等检查）
     * M5-7 申诉恢复步骤7
     */
    @Select("SELECT * FROM credit_change WHERE source_type = #{sourceType} " +
            "AND source_id = #{sourceId} AND change_type = #{changeType} " +
            "AND user_id = #{userId} LIMIT 1")
    CreditChange findAppealRestoreExists(
            @Param("sourceType") String sourceType,
            @Param("sourceId") Long sourceId,
            @Param("changeType") String changeType,
            @Param("userId") Long userId);
}
