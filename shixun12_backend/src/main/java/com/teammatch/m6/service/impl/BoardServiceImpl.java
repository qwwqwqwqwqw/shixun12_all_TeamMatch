package com.teammatch.m6.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teammatch.m6.dto.BoardCreateDTO;
import com.teammatch.m6.dto.BoardProjectSummaryVO;
import com.teammatch.m6.dto.BoardUpdateDTO;
import com.teammatch.m6.entity.Board;
import com.teammatch.m6.mapper.BoardMapper;
import com.teammatch.m6.service.BoardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 板块 Service 实现类
 *
 * 根据详细设计文档 7.2 节、7.4 节定义实现
 */
@Service
public class BoardServiceImpl extends ServiceImpl<BoardMapper, Board> implements BoardService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Board createBoard(BoardCreateDTO dto) {
        // 名称trim处理，确保业务唯一性
        String trimmedName = dto.getName().trim();
        if (!StringUtils.hasText(trimmedName)) {
            throw new IllegalArgumentException("板块名称不能为空");
        }

        // 检查名称是否已存在（使用trim后的名称）
        Board existingBoard = baseMapper.selectByName(trimmedName);
        if (existingBoard != null) {
            throw new IllegalArgumentException("板块名称已存在: " + trimmedName);
        }

        // 创建板块实体
        Board board = new Board();
        board.setName(trimmedName);
        board.setDescription(dto.getDescription());
        board.setStatus("active");
        board.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        board.setCreatedAt(LocalDateTime.now());
        board.setUpdatedAt(LocalDateTime.now());

        // 保存到数据库
        baseMapper.insert(board);
        return board;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Board updateBoard(Long id, BoardUpdateDTO dto) {
        // 检查板块是否存在
        Board board = baseMapper.selectById(id);
        if (board == null) {
            throw new IllegalArgumentException("板块不存在: " + id);
        }

        // 如果更新名称，检查是否与其他板块冲突
        if (dto.getName() != null) {
            String trimmedName = dto.getName().trim();
            if (!StringUtils.hasText(trimmedName)) {
                throw new IllegalArgumentException("板块名称不能为空");
            }
            Board existingBoard = baseMapper.selectByName(trimmedName);
            if (existingBoard != null && !existingBoard.getId().equals(id)) {
                throw new IllegalArgumentException("板块名称已存在: " + trimmedName);
            }
            board.setName(trimmedName);
        }

        // 更新其他字段
        if (dto.getDescription() != null) {
            board.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            // 校验状态值
            if (!"active".equals(dto.getStatus()) && !"inactive".equals(dto.getStatus())) {
                throw new IllegalArgumentException("无效的板块状态: " + dto.getStatus());
            }
            board.setStatus(dto.getStatus());
        }
        if (dto.getSortOrder() != null) {
            board.setSortOrder(dto.getSortOrder());
        }

        board.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(board);
        return board;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBoard(Long id) {
        // 检查板块是否存在
        Board board = baseMapper.selectById(id);
        if (board == null) {
            throw new IllegalArgumentException("板块不存在: " + id);
        }

        // 检查是否有项目引用该板块
        Long projectCount = baseMapper.countProjectsByBoardId(id);
        if (projectCount > 0) {
            throw new IllegalStateException("该板块下存在项目，无法删除");
        }

        // 删除板块
        baseMapper.deleteById(id);
    }

    @Override
    public List<Board> getActiveBoards() {
        return baseMapper.selectActiveBoards();
    }

    @Override
    public Board getBoardById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public boolean existsActiveBoard(Long boardId) {
        if (boardId == null) {
            return false;
        }
        Board board = baseMapper.selectById(boardId);
        return board != null && "active".equals(board.getStatus());
    }

    @Override
    public List<BoardProjectSummaryVO> listProjectsByBoardId(Long boardId) {
        Board board = baseMapper.selectById(boardId);
        if (board == null) {
            throw new IllegalArgumentException("板块不存在: " + boardId);
        }
        return baseMapper.selectProjectsByBoardId(boardId);
    }
}
