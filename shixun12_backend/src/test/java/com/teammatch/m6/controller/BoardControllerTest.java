package com.teammatch.m6.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teammatch.common.ReasonCode;
import com.teammatch.m6.dto.BoardCreateDTO;
import com.teammatch.m6.dto.BoardProjectSummaryVO;
import com.teammatch.m6.dto.BoardUpdateDTO;
import com.teammatch.m6.entity.Board;
import com.teammatch.config.AuthExceptionHandler;
import com.teammatch.exception.AuthenticationException;
import com.teammatch.exception.AuthorizationException;
import com.teammatch.m6.service.BoardService;
import com.teammatch.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BoardController 单元测试
 * 测试认证鉴权和接口响应
 */
@WebMvcTest(BoardController.class)
@Import(AuthExceptionHandler.class)
@DisplayName("板块管理控制器测试")
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BoardService boardService;

    @MockBean
    private AuthUtil authUtil;

    private static final String ADMIN_TOKEN = "Bearer admin-token";
    private static final String USER_TOKEN = "Bearer user-token";

    private Board mockBoard;

    @BeforeEach
    void setUp() {
        mockBoard = new Board();
        mockBoard.setId(1L);
        mockBoard.setName("测试板块");
        mockBoard.setDescription("测试描述");
        mockBoard.setStatus("active");
        mockBoard.setSortOrder(1);
    }

    // ==================== 认证失败场景 ====================

    @Test
    @DisplayName("createBoard: Token无效返回M3000")
    void createBoard_invalidToken_returnsUnauthorized() throws Exception {
        doThrow(new AuthenticationException(ReasonCode.UNAUTHORIZED))
                .when(authUtil).requireAdmin("Bearer invalid-token");

        BoardCreateDTO dto = new BoardCreateDTO();
        dto.setName("新板块");
        dto.setDescription("描述");

        mockMvc.perform(post("/admin/boards")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.UNAUTHORIZED.getCode()));

        verify(boardService, never()).createBoard(any());
    }

    @Test
    @DisplayName("createBoard: 非管理员访问返回403")
    void createBoard_nonAdmin_returnsForbidden() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        BoardCreateDTO dto = new BoardCreateDTO();
        dto.setName("新板块");
        dto.setDescription("描述");

        mockMvc.perform(post("/admin/boards")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(boardService, never()).createBoard(any());
    }

    @Test
    @DisplayName("updateBoard: 非管理员访问返回403")
    void updateBoard_nonAdmin_returnsForbidden() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setDescription("更新描述");

        mockMvc.perform(put("/admin/boards/1")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(boardService, never()).updateBoard(any(), any());
    }

    @Test
    @DisplayName("deleteBoard: 非管理员访问返回403")
    void deleteBoard_nonAdmin_returnsForbidden() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        mockMvc.perform(delete("/admin/boards/1")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(boardService, never()).deleteBoard(any());
    }

    @Test
    @DisplayName("getBoard: 非管理员访问返回403")
    void getBoard_nonAdmin_returnsForbidden() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        mockMvc.perform(get("/admin/boards/1")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(boardService, never()).getBoardById(any());
    }

    @Test
    @DisplayName("listBoards: 非管理员访问返回403")
    void listBoards_nonAdmin_returnsForbidden() throws Exception {
        doThrow(new AuthorizationException(ReasonCode.ADMIN_REQUIRED))
                .when(authUtil).requireAdmin(ADMIN_TOKEN);

        mockMvc.perform(get("/admin/boards")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.ADMIN_REQUIRED.getCode()));

        verify(boardService, never()).list();
    }

    // ==================== 管理员成功场景 ====================

    @Test
    @DisplayName("createBoard: 管理员成功创建板块")
    void createBoard_adminSuccess() throws Exception {
        when(boardService.createBoard(any(BoardCreateDTO.class))).thenReturn(mockBoard);

        BoardCreateDTO dto = new BoardCreateDTO();
        dto.setName("新板块");
        dto.setDescription("新描述");
        dto.setSortOrder(1);

        mockMvc.perform(post("/admin/boards")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.name").value("测试板块"));

        verify(boardService).createBoard(any(BoardCreateDTO.class));
    }

    @Test
    @DisplayName("updateBoard: 管理员成功更新板块")
    void updateBoard_adminSuccess() throws Exception {
        when(boardService.updateBoard(eq(1L), any(BoardUpdateDTO.class))).thenReturn(mockBoard);

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setDescription("更新后的描述");

        mockMvc.perform(put("/admin/boards/1")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.name").value("测试板块"));

        verify(boardService).updateBoard(eq(1L), any(BoardUpdateDTO.class));
    }

    @Test
    @DisplayName("deleteBoard: 管理员成功删除板块")
    void deleteBoard_adminSuccess() throws Exception {
        doNothing().when(boardService).deleteBoard(1L);

        mockMvc.perform(delete("/admin/boards/1")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(boardService).deleteBoard(1L);
    }

    @Test
    @DisplayName("getBoard: 管理员成功获取板块详情")
    void getBoard_adminSuccess() throws Exception {
        when(boardService.getBoardById(1L)).thenReturn(mockBoard);

        mockMvc.perform(get("/admin/boards/1")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.name").value("测试板块"));

        verify(boardService).getBoardById(1L);
    }

    @Test
    @DisplayName("listBoardProjects: 管理员成功获取板块项目列表")
    void listBoardProjects_adminSuccess() throws Exception {
        BoardProjectSummaryVO project = new BoardProjectSummaryVO();
        project.setId(10L);
        project.setTitle("后端实训");
        project.setStatus("recruiting");
        when(boardService.listProjectsByBoardId(1L)).thenReturn(List.of(project));

        mockMvc.perform(get("/admin/boards/1/projects")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data[0].title").value("后端实训"));

        verify(boardService).listProjectsByBoardId(1L);
    }

    @Test
    @DisplayName("listBoardProjects: 板块不存在返回NOT_FOUND")
    void listBoardProjects_notFound() throws Exception {
        when(boardService.listProjectsByBoardId(999L))
                .thenThrow(new IllegalArgumentException("板块不存在: 999"));

        mockMvc.perform(get("/admin/boards/999/projects")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("listBoards: 管理员成功获取板块列表")
    void listBoards_adminSuccess() throws Exception {
        List<Board> boards = Arrays.asList(mockBoard);
        when(boardService.list()).thenReturn(boards);

        mockMvc.perform(get("/admin/boards")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("测试板块"));

        verify(boardService).list();
    }

    // ==================== 业务错误场景 ====================

    @Test
    @DisplayName("createBoard: 参数错误返回PARAM_ERROR")
    void createBoard_paramError() throws Exception {
        when(boardService.createBoard(any())).thenThrow(new IllegalArgumentException("板块名称已存在"));

        BoardCreateDTO dto = new BoardCreateDTO();
        dto.setName("重复名称");

        mockMvc.perform(post("/admin/boards")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.PARAM_ERROR.getCode()));
    }

    @Test
    @DisplayName("updateBoard: 板块不存在返回NOT_FOUND")
    void updateBoard_notFound() throws Exception {
        when(boardService.updateBoard(any(), any())).thenThrow(new IllegalArgumentException("板块不存在: 999"));

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setDescription("描述");

        mockMvc.perform(put("/admin/boards/999")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("deleteBoard: 板块有项目引用返回STATUS_CONFLICT")
    void deleteBoard_hasProjects() throws Exception {
        doThrow(new IllegalStateException("该板块下存在项目")).when(boardService).deleteBoard(1L);

        mockMvc.perform(delete("/admin/boards/1")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.STATUS_CONFLICT.getCode()));
    }

    @Test
    @DisplayName("getBoard: 板块不存在返回NOT_FOUND")
    void getBoard_notFound() throws Exception {
        when(boardService.getBoardById(999L)).thenReturn(null);

        mockMvc.perform(get("/admin/boards/999")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("updateBoard: 参数错误（非板块不存在）返回PARAM_ERROR")
    void updateBoard_paramError_notNotFound() throws Exception {
        // 抛出不含"板块不存在"的IllegalArgumentException，应返回PARAM_ERROR
        when(boardService.updateBoard(any(), any())).thenThrow(new IllegalArgumentException("板块名称已存在"));

        BoardUpdateDTO dto = new BoardUpdateDTO();
        dto.setName("重复名称");

        mockMvc.perform(put("/admin/boards/1")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.PARAM_ERROR.getCode()));
    }

    @Test
    @DisplayName("deleteBoard: 板块不存在返回NOT_FOUND")
    void deleteBoard_notFound() throws Exception {
        doThrow(new IllegalArgumentException("板块不存在: 999")).when(boardService).deleteBoard(999L);

        mockMvc.perform(delete("/admin/boards/999")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ReasonCode.NOT_FOUND.getCode()));
    }
}
