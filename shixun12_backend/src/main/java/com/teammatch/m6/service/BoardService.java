package com.teammatch.m6.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teammatch.m6.dto.BoardCreateDTO;
import com.teammatch.m6.dto.BoardProjectSummaryVO;
import com.teammatch.m6.dto.BoardUpdateDTO;
import com.teammatch.m6.entity.Board;

import java.util.List;

/**
 * 板块 Service 接口
 *
 * 根据详细设计文档 7.2 节定义
 */
public interface BoardService extends IService<Board> {

    /**
     * 创建板块
     *
     * @param dto 创建请求
     * @return 创建的板块
     * @throws IllegalArgumentException 如果名称已存在
     */
    Board createBoard(BoardCreateDTO dto);

    /**
     * 更新板块
     *
     * @param id 板块ID
     * @param dto 更新请求
     * @return 更新后的板块
     * @throws IllegalArgumentException 如果板块不存在或名称冲突
     */
    Board updateBoard(Long id, BoardUpdateDTO dto);

    /**
     * 删除板块
     * 仅允许无项目引用时删除
     *
     * @param id 板块ID
     * @throws IllegalArgumentException 如果板块不存在
     * @throws IllegalStateException 如果有项目引用该板块
     */
    void deleteBoard(Long id);

    /**
     * 获取所有启用的板块列表
     * 供项目创建时使用
     *
     * @return 启用的板块列表
     */
    List<Board> getActiveBoards();

    /**
     * 获取板块详情
     *
     * @param id 板块ID
     * @return 板块实体，不存在返回 null
     */
    Board getBoardById(Long id);

    /**
     * 检查板块是否存在且启用
     * 供M4项目创建时调用
     *
     * @param boardId 板块ID
     * @return true-存在且启用，false-不存在或禁用
     */
    boolean existsActiveBoard(Long boardId);

    /**
     * 获取板块下的项目列表（管理端）
     *
     * @param boardId 板块ID
     * @return 项目摘要列表
     * @throws IllegalArgumentException 板块不存在
     */
    List<BoardProjectSummaryVO> listProjectsByBoardId(Long boardId);
}
