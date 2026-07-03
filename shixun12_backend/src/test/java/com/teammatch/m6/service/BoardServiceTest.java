package com.teammatch.m6.service;

import com.teammatch.m6.dto.BoardCreateDTO;
import com.teammatch.m6.dto.BoardProjectSummaryVO;
import com.teammatch.m6.dto.BoardUpdateDTO;
import com.teammatch.m6.entity.Board;
import com.teammatch.m6.mapper.BoardMapper;
import com.teammatch.m6.service.impl.BoardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BoardService 单元测试
 * 使用 Mockito Mock 数据库操作，专注于 Service 业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("板块服务单元测试")
class BoardServiceTest {

    @Mock
    private BoardMapper boardMapper;

    @InjectMocks
    private BoardServiceImpl service;

    private static final Long BOARD_ID = 1L;
    private Board existingBoard;

    @BeforeEach
    void setUp() {
        // ServiceImpl 继承自 ServiceImpl<BoardMapper, Board>，
        // 其 baseMapper 字段在 @InjectMocks 时不会自动注入，需手动设置
        ReflectionTestUtils.setField(service, "baseMapper", boardMapper);

        existingBoard = new Board();
        existingBoard.setId(BOARD_ID);
        existingBoard.setName("已有板块");
        existingBoard.setDescription("已有描述");
        existingBoard.setStatus("active");
        existingBoard.setSortOrder(1);
        existingBoard.setCreatedAt(LocalDateTime.now().minusDays(1));
        existingBoard.setUpdatedAt(LocalDateTime.now().minusDays(1));
    }

    // ==================== createBoard ====================

    @Test
    @DisplayName("createBoard: 正常创建板块")
    void createBoard_success() {
        when(boardMapper.selectByName("新板块")).thenReturn(null);
        when(boardMapper.insert(any(Board.class))).thenReturn(1);

        BoardCreateDTO dto = new BoardCreateDTO();
        dto.setName("新板块");
        dto.setDescription("新描述");
        dto.setSortOrder(1);

        Board result = service.createBoard(dto);

        assertThat(result.getName()).isEqualTo("新板块");
        assertThat(result.getDescription()).isEqualTo("新描述");
        assertThat(result.getStatus()).isEqualTo("active");
        assertThat(result.getSortOrder()).isEqualTo(1);
        assertThat(result.getCreatedAt()).isNotNull();
        verify(boardMapper).insert(any(Board.class));
    }

    @Test
    @DisplayName("createBoard: 名称已存在时抛出异常")
    void createBoard_duplicateName_throws() {
        when(boardMapper.selectByName("已有板块")).thenReturn(existingBoard);

        BoardCreateDTO dto = new BoardCreateDTO();
        dto.setName("已有板块");
        dto.setDescription("描述");

        assertThatThrownBy(() -> service.createBoard(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("板块名称已存在");
        verify(boardMapper, never()).insert(any());
    }

    @Test
    @DisplayName("createBoard: 名称前后空格自动trim")
    void createBoard_trimName() {
        when(boardMapper.selectByName("新板块")).thenReturn(null);
        when(boardMapper.insert(any(Board.class))).thenReturn(1);

        BoardCreateDTO dto = new BoardCreateDTO();
        dto.setName("  新板块  ");
        dto.setDescription("描述");

        Board result = service.createBoard(dto);

        assertThat(result.getName()).isEqualTo("新板块");
    }

    @Test
    @DisplayName("createBoard: 名称trim后为空时抛出异常")
    void createBoard_emptyAfterTrim_throws() {
        BoardCreateDTO dto = new BoardCreateDTO();
        dto.setName("   ");
        dto.setDescription("描述");

        assertThatThrownBy(() -> service.createBoard(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("板块名称不能为空");
        verify(boardMapper, never()).insert(any());
    }

    @Test
    @DisplayName("createBoard: sortOrder 为 null 时默认为 0")
    void createBoard_defaultSortOrder() {
        when(boardMapper.selectByName("无排序板块")).thenReturn(null);
        when(boardMapper.insert(any(Board.class))).thenReturn(1);

        BoardCreateDTO dto = new BoardCreateDTO();
        dto.setName("无排序板块");
        dto.setDescription("描述");
        dto.setSortOrder(null);

        Board result = service.createBoard(dto);

        assertThat(result.getSortOrder()).isEqualTo(0);
    }

    // ==================== updateBoard ====================

    @Test
    @DisplayName("updateBoard: 正常更新板块描述和状态")
    void updateBoard_success() {
        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);
        when(boardMapper.updateById(any(Board.class))).thenReturn(1);

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setDescription("更新后的描述");
        dto.setStatus("inactive");
        dto.setSortOrder(10);

        Board result = service.updateBoard(BOARD_ID, dto);

        assertThat(result.getDescription()).isEqualTo("更新后的描述");
        assertThat(result.getStatus()).isEqualTo("inactive");
        assertThat(result.getSortOrder()).isEqualTo(10);
        // 名称不应改变
        assertThat(result.getName()).isEqualTo("已有板块");
        verify(boardMapper).updateById(any(Board.class));
    }

    @Test
    @DisplayName("updateBoard: 板块不存在时抛出异常")
    void updateBoard_notFound_throws() {
        when(boardMapper.selectById(99999L)).thenReturn(null);

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setDescription("描述");

        assertThatThrownBy(() -> service.updateBoard(99999L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("板块不存在");
    }

    @Test
    @DisplayName("updateBoard: 更新名称为已存在的其他板块名时抛出异常")
    void updateBoard_nameConflict_throws() {
        Board anotherBoard = new Board();
        anotherBoard.setId(2L);
        anotherBoard.setName("另一板块");

        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);
        when(boardMapper.selectByName("另一板块")).thenReturn(anotherBoard);

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setName("另一板块");

        assertThatThrownBy(() -> service.updateBoard(BOARD_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("板块名称已存在");
    }

    @Test
    @DisplayName("updateBoard: 更新为相同名称不冲突")
    void updateBoard_sameName_noConflict() {
        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);
        when(boardMapper.updateById(any(Board.class))).thenReturn(1);

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setName("已有板块"); // 与自己名称相同

        Board result = service.updateBoard(BOARD_ID, dto);

        assertThat(result.getName()).isEqualTo("已有板块");
        verify(boardMapper).updateById(any(Board.class));
    }

    @Test
    @DisplayName("updateBoard: 名称前后空格自动trim")
    void updateBoard_trimName() {
        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);
        when(boardMapper.selectByName("新名称")).thenReturn(null);
        when(boardMapper.updateById(any(Board.class))).thenReturn(1);

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setName("  新名称  ");

        Board result = service.updateBoard(BOARD_ID, dto);

        assertThat(result.getName()).isEqualTo("新名称");
    }

    @Test
    @DisplayName("updateBoard: 名称trim后为空时抛出异常")
    void updateBoard_emptyAfterTrim_throws() {
        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setName("   ");

        assertThatThrownBy(() -> service.updateBoard(BOARD_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("板块名称不能为空");
    }

    @Test
    @DisplayName("updateBoard: 无效的状态值抛出异常")
    void updateBoard_invalidStatus_throws() {
        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setStatus("invalid_status");

        assertThatThrownBy(() -> service.updateBoard(BOARD_ID, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无效的板块状态");
    }

    @Test
    @DisplayName("updateBoard: status 为 active 有效")
    void updateBoard_statusActive_valid() {
        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);
        when(boardMapper.updateById(any(Board.class))).thenReturn(1);

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setStatus("active");

        Board result = service.updateBoard(BOARD_ID, dto);

        assertThat(result.getStatus()).isEqualTo("active");
    }

    @Test
    @DisplayName("updateBoard: status 为 inactive 有效")
    void updateBoard_statusInactive_valid() {
        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);
        when(boardMapper.updateById(any(Board.class))).thenReturn(1);

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setStatus("inactive");

        Board result = service.updateBoard(BOARD_ID, dto);

        assertThat(result.getStatus()).isEqualTo("inactive");
    }

    // ==================== deleteBoard ====================

    @Test
    @DisplayName("deleteBoard: 正常删除无引用的板块")
    void deleteBoard_success() {
        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);
        when(boardMapper.countProjectsByBoardId(BOARD_ID)).thenReturn(0L);
        when(boardMapper.deleteById((Serializable) BOARD_ID)).thenReturn(1);

        service.deleteBoard(BOARD_ID);

        verify(boardMapper).deleteById((Serializable) eq(BOARD_ID));
    }

    @Test
    @DisplayName("deleteBoard: 板块不存在时抛出异常")
    void deleteBoard_notFound_throws() {
        when(boardMapper.selectById(99999L)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteBoard(99999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("板块不存在");
        verify(boardMapper, never()).deleteById((Serializable) any());
    }

    @Test
    @DisplayName("deleteBoard: 有项目引用时抛出 IllegalStateException")
    void deleteBoard_hasProjects_throws() {
        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);
        when(boardMapper.countProjectsByBoardId(BOARD_ID)).thenReturn(3L);

        assertThatThrownBy(() -> service.deleteBoard(BOARD_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("该板块下存在项目，无法删除");
        verify(boardMapper, never()).deleteById((Serializable) any());
    }

    // ==================== getActiveBoards ====================

    @Test
    @DisplayName("getActiveBoards: 返回所有启用的板块")
    void getActiveBoards_success() {
        Board board1 = new Board();
        board1.setId(1L);
        board1.setName("板块1");
        board1.setStatus("active");

        Board board2 = new Board();
        board2.setId(2L);
        board2.setName("板块2");
        board2.setStatus("active");

        when(boardMapper.selectActiveBoards()).thenReturn(Arrays.asList(board1, board2));

        List<Board> result = service.getActiveBoards();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Board::getStatus).containsOnly("active");
    }

    @Test
    @DisplayName("getActiveBoards: 无启用板块时返回空列表")
    void getActiveBoards_empty() {
        when(boardMapper.selectActiveBoards()).thenReturn(Collections.emptyList());

        List<Board> result = service.getActiveBoards();

        assertThat(result).isEmpty();
    }

    // ==================== getBoardById ====================

    @Test
    @DisplayName("getBoardById: 正常查询存在的板块")
    void getBoardById_exists() {
        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);

        Board result = service.getBoardById(BOARD_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(BOARD_ID);
        assertThat(result.getName()).isEqualTo("已有板块");
    }

    @Test
    @DisplayName("getBoardById: 不存在的 ID 返回 null")
    void getBoardById_notExists() {
        when(boardMapper.selectById(99999L)).thenReturn(null);

        Board result = service.getBoardById(99999L);

        assertThat(result).isNull();
    }

    // ==================== existsActiveBoard ====================

    @Test
    @DisplayName("existsActiveBoard: 存在且启用的板块返回 true")
    void existsActiveBoard_active_returnsTrue() {
        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);

        boolean result = service.existsActiveBoard(BOARD_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsActiveBoard: 禁用的板块返回 false")
    void existsActiveBoard_inactive_returnsFalse() {
        Board inactiveBoard = new Board();
        inactiveBoard.setId(BOARD_ID);
        inactiveBoard.setStatus("inactive");
        when(boardMapper.selectById(BOARD_ID)).thenReturn(inactiveBoard);

        boolean result = service.existsActiveBoard(BOARD_ID);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsActiveBoard: 不存在的板块返回 false")
    void existsActiveBoard_notExists_returnsFalse() {
        when(boardMapper.selectById(99999L)).thenReturn(null);

        boolean result = service.existsActiveBoard(99999L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsActiveBoard: null ID 返回 false")
    void existsActiveBoard_nullId_returnsFalse() {
        boolean result = service.existsActiveBoard(null);

        assertThat(result).isFalse();
    }

    // ==================== listProjectsByBoardId ====================

    @Test
    @DisplayName("listProjectsByBoardId: 返回板块下的项目列表")
    void listProjectsByBoardId_success() {
        BoardProjectSummaryVO project = new BoardProjectSummaryVO();
        project.setId(10L);
        project.setTitle("后端实训");
        project.setStatus("recruiting");

        when(boardMapper.selectById(BOARD_ID)).thenReturn(existingBoard);
        when(boardMapper.selectProjectsByBoardId(BOARD_ID)).thenReturn(List.of(project));

        List<BoardProjectSummaryVO> result = service.listProjectsByBoardId(BOARD_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("后端实训");
    }

    @Test
    @DisplayName("listProjectsByBoardId: 板块不存在时抛出异常")
    void listProjectsByBoardId_boardNotFound_throws() {
        when(boardMapper.selectById(99999L)).thenReturn(null);

        assertThatThrownBy(() -> service.listProjectsByBoardId(99999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("板块不存在");

        verify(boardMapper, never()).selectProjectsByBoardId(any());
    }
}