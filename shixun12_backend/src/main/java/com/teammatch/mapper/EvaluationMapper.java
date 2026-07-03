package com.teammatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teammatch.dto.EndorsementPoint;
import com.teammatch.entity.Evaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 互评数据访问层
 * 用于 M5-1B/C 互评状态查询和重复评价检查
 */
@Mapper
public interface EvaluationMapper extends BaseMapper<Evaluation> {

    // 继承 BaseMapper 提供的基础 CRUD 方法

    /**
     * 检查是否已经评价过
     * 用于 M5-1C 最终提交前资格兜底校验（防止重复评价）
     *
     * @param evaluatorId 评价人 ID
     * @param targetId 被评价人 ID
     * @param projectId 项目 ID
     * @return 如果已评价返回评价记录，否则返回 null
     */
    @Select("SELECT * FROM evaluation WHERE evaluator_id = #{evaluatorId} AND target_id = #{targetId} AND project_id = #{projectId} LIMIT 1")
    Evaluation findEvaluation(@Param("evaluatorId") Long evaluatorId,
                              @Param("targetId") Long targetId,
                              @Param("projectId") Long projectId);

    /**
     * 查询评价人在某个项目中已评价的所有目标用户 ID
     * 用于 M5-1B 待评价成员列表判断（标记哪些成员已评价）
     *
     * @param evaluatorId 评价人 ID
     * @param projectId 项目 ID
     * @return 已评价的目标用户 ID 列表
     */
    @Select("SELECT target_id FROM evaluation WHERE evaluator_id = #{evaluatorId} AND project_id = #{projectId}")
    List<Long> findEvaluatedTargetIds(@Param("evaluatorId") Long evaluatorId,
                                      @Param("projectId") Long projectId);

    /**
     * 查询评价人在某个项目中已提交的全部评价
     * 用于 M5-4 覆盖全部队友后的异常评价检测
     *
     * @param evaluatorId 评价人 ID
     * @param projectId 项目 ID
     * @return 该评价人在项目下提交的评价列表
     */
    @Select("SELECT * FROM evaluation WHERE evaluator_id = #{evaluatorId} AND project_id = #{projectId} ORDER BY id")
    List<Evaluation> findByEvaluatorAndProject(@Param("evaluatorId") Long evaluatorId,
                                               @Param("projectId") Long projectId);

    @Update("<script>" +
            "UPDATE evaluation SET status = #{status}, updated_at = NOW() WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 评价复核：更新 evaluation 状态及复核字段
     * 用于 M5-6 评价复核 Service
     */
    @Update("UPDATE evaluation SET status = #{status}, reviewer_id = #{reviewerId}, " +
            "review_note = #{reviewNote}, reviewed_at = #{reviewedAt}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateReview(@Param("id") Long id, @Param("status") String status,
                     @Param("reviewerId") Long reviewerId,
                     @Param("reviewNote") String reviewNote,
                     @Param("reviewedAt") LocalDateTime reviewedAt);

    /**
     * 评价复核条件更新：仅当 status='pending_review' 时更新
     * 防止并发/重复复核导致状态覆盖或重复计分
     */
    @Update("UPDATE evaluation SET status = #{status}, reviewer_id = #{reviewerId}, " +
            "review_note = #{reviewNote}, reviewed_at = #{reviewedAt}, updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'pending_review'")
    int updateReviewIfPending(@Param("id") Long id, @Param("status") String status,
                              @Param("reviewerId") Long reviewerId,
                              @Param("reviewNote") String reviewNote,
                              @Param("reviewedAt") LocalDateTime reviewedAt);

    /**
     * SELECT ... FOR UPDATE 锁定 evaluation 行
     * M5-7 申诉恢复使用，V2.1 8.7 D-11/D-13 并发协议
     */
    @Select("SELECT * FROM evaluation WHERE id = #{id} FOR UPDATE")
    Evaluation selectByIdForUpdate(@Param("id") Long id);

    /**
     * 查询用户收到的所有互评（按 target_id）
     * M5-8A B5 使用，前台匿名展示。
     * 过滤 voided 和 projectId 二次过滤在 Controller 层完成。
     *
     * @param targetId 被评价人（即当前用户）ID
     * @return 该用户收到的全部互评记录
     */
    @Select("SELECT * FROM evaluation WHERE target_id = #{targetId} ORDER BY created_at DESC")
    List<Evaluation> findByTargetId(@Param("targetId") Long targetId);

    /**
     * 查询用户收到的、状态为 normal 的互评（申诉页候选，与 POST /appeals 规则一致）
     */
    @Select("SELECT * FROM evaluation WHERE target_id = #{targetId} AND status = 'normal' ORDER BY created_at DESC")
    List<Evaluation> findNormalByTargetId(@Param("targetId") Long targetId);

    // ==================== M4 推荐模块新增 ====================

    /**
     * 批量查多个被评价人的互评技术维度打分,JOIN user 取评价者信誉分
     * <p>
     * 用于 M4 推荐精排层:避免循环里 N 次单查,一次 SQL 拿全部候选人的加权点。
     * 只取 status='normal' 的有效互评。
     * </p>
     *
     * @param targetUserIds 被评价人用户 ID 列表(候选人)
     * @return EndorsementPoint 列表,字段 targetUserId 用于业务层 groupingBy 分组
     */
    @Select("<script>" +
            "SELECT e.skill_score        AS skillScore, " +
            "       u.credit_score       AS evaluatorCreditScore, " +
            "       e.evaluator_id       AS evaluatorId, " +
            "       e.target_id          AS targetUserId " +
            "FROM evaluation e " +
            "JOIN `user` u ON e.evaluator_id = u.id " +
            "WHERE e.target_id IN " +
            "  <foreach collection='targetUserIds' item='id' open='(' separator=',' close=')'>" +
            "    #{id}" +
            "  </foreach> " +
            "  AND e.status = 'normal' " +
            "ORDER BY e.target_id, e.created_at DESC" +
            "</script>")
    List<EndorsementPoint> findSkillScoresByTargets(@Param("targetUserIds") List<Long> targetUserIds);
}
