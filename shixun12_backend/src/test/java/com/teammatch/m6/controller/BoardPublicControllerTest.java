package com.teammatch.m6.controller;

import com.teammatch.m6.entity.Board;
import com.teammatch.m6.service.BoardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BoardPublicController 单元测试
 * 测试公开查询接口（无需认证）
 */
@WebMvcTest(BoardPublicController.class)
@DisplayName("板块公开查询控制器测试")
class BoardPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    private Board activeBoard1;
    private Board activeBoard2;

    @BeforeEach
    void setUp() {
        activeBoard1 = new Board();
        activeBoard1.setId(1L);
        activeBoard1.setName("技术交流");
        activeBoard1.setDescription("技术讨论板块");
        activeBoard1.setStatus("active");
        activeBoard1.setSortOrder(1);

        activeBoard2 = new Board();
        activeBoard2.setId(2L);
        activeBoard2.setName("项目合作");
        activeBoard2.setDescription("寻找队友板块");
        activeBoard2.setStatus("active");
        activeBoard2.setSortOrder(2);
    }

    @Test
    @DisplayName("listActiveBoards: 返回所有启用的板块列表")
    void listActiveBoards_success() throws Exception {
        List <Board> boards = Arrays.asList(activeBoard1, activeBoard2);
        when(boardService.getActiveBoards()).thenReturn(boards);

        mockMvc.perform(get("/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("技术交流"))
                .andExpect(jsonPath("$.data[0].status").value("active"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("项目合作"));

        verify(boardService).getActiveBoards();
    }

    @Test
    @DisplayName("listActiveBoards: 无启用板块时返回空列表")
    void listActiveBoards_empty() throws Exception {
        when(boardService.getActiveBoards()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(boardService).getActiveBoards();
    }

    @Test
    @DisplayName("listActiveBoards: 接口无需认证即可访问")
    void listActiveBoards_noAuthRequired() throws Exception {
        // 公开接口不需要Authorization头
        when(boardService.getActiveBoards()).thenReturn(Arrays.asList(activeBoard1));

        mockMvc.perform(get("/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(boardService).getActiveBoards();
    }
}
