package com.teammatch.m6.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teammatch.m6.dto.BoardProjectSummaryVO;
import com.teammatch.m6.entity.Board;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 板块 Mapper 接口
 *
 * 根据详细设计文档 4.5 节、7.5 节定义
 * 提供板块基础 CRUD 和查询能力
 */
@Mapper
public interface BoardMapper extends BaseMapper<Board> {

    /**
     * 查询所有启用的板块，按排序号升序
     * 供项目创建时选择板块使用
     *
     * @return 启用的板块列表
     */
    @Select("SELECT * FROM board WHERE status = 'active' ORDER BY sort_order ASC, id ASC")
    List<Board> selectActiveBoards();

    /**
     * 根据名称查询板块（用于唯一性校验）
     *
     * @param name 板块名称
     * @return 板块实体
     */
    @Select("SELECT * FROM board WHERE name = #{name} LIMIT 1")
    Board selectByName(String name);

    /**
     * 统计引用该板块的项目数量
     * 用于删除前检查是否有项目引用
     *
     * @param boardId 板块ID
     * @return 项目数量
     */
    @Select("SELECT COUNT(*) FROM project WHERE board_id = #{boardId}")
    Long countProjectsByBoardId(Long boardId);

    /**
     * 查询板块下的项目列表（管理端）
     *
     * @param boardId 板块ID
     * @return 项目摘要列表，按创建时间倒序
     */
    @Select("SELECT id, creator_id AS creatorId, title, status, max_members AS maxMembers, created_at AS createdAt " +
            "FROM project WHERE board_id = #{boardId} ORDER BY created_at DESC")
    List<BoardProjectSummaryVO> selectProjectsByBoardId(@Param("boardId") Long boardId);
}
