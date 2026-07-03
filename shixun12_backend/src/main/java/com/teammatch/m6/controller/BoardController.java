package com.teammatch.m6.controller;

import com.teammatch.common.ReasonCode;
import com.teammatch.common.Result;
import com.teammatch.m6.dto.BoardCreateDTO;
import com.teammatch.m6.dto.BoardProjectSummaryVO;
import com.teammatch.m6.dto.BoardUpdateDTO;
import com.teammatch.m6.entity.Board;
import com.teammatch.m6.service.BoardService;
import com.teammatch.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 板块管理 Controller
 *
 * 根据详细设计文档 7.2 节定义
 * 管理端接口前缀：/api/admin/boards
 * 需要管理员权限（role=admin）
 */
@RestController
@RequestMapping("/admin/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final AuthUtil authUtil;

    /**
     * 创建板块
     * POST /api/admin/boards
     */
    @PostMapping
    public Result<Board> createBoard(@Valid @RequestBody BoardCreateDTO dto,
                                     @RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);
        try {
            Board board = boardService.createBoard(dto);
            return Result.success(board);
        } catch (IllegalArgumentException e) {
            return Result.fail(ReasonCode.PARAM_ERROR);
        }
    }

    /**
     * 更新板块
     * PUT /api/admin/boards/{id}
     */
    @PutMapping("/{id}")
    public Result<Board> updateBoard(@PathVariable Long id,
                                       @Valid @RequestBody BoardUpdateDTO dto,
                                       @RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);
        try {
            Board board = boardService.updateBoard(id, dto);
            return Result.success(board);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("板块不存在")) {
                return Result.fail(ReasonCode.NOT_FOUND);
            }
            return Result.fail(ReasonCode.PARAM_ERROR);
        }
    }

    /**
     * 删除板块
     * DELETE /api/admin/boards/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteBoard(@PathVariable Long id,
                                    @RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);
        try {
            boardService.deleteBoard(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.fail(ReasonCode.NOT_FOUND);
        } catch (IllegalStateException e) {
            return Result.fail(ReasonCode.STATUS_CONFLICT);
        }
    }

    /**
     * 获取板块详情
     * GET /api/admin/boards/{id}
     */
    @GetMapping("/{id}")
    public Result<Board> getBoard(@PathVariable Long id,
                                  @RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);
        Board board = boardService.getBoardById(id);
        if (board == null) {
            return Result.fail(ReasonCode.NOT_FOUND);
        }
        return Result.success(board);
    }

    /**
     * 获取板块下的项目列表
     * GET /api/admin/boards/{id}/projects
     */
    @GetMapping("/{id}/projects")
    public Result<List<BoardProjectSummaryVO>> listBoardProjects(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);
        try {
            return Result.success(boardService.listProjectsByBoardId(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(ReasonCode.NOT_FOUND);
        }
    }

    /**
     * 获取所有板块列表（管理端）
     * GET /api/admin/boards
     */
    @GetMapping
    public Result<List<Board>> listBoards(@RequestHeader("Authorization") String token) {
        authUtil.requireAdmin(token);
        List<Board> boards = boardService.list();
        return Result.success(boards);
    }
}
