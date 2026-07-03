package com.teammatch.m6.controller;

import com.teammatch.common.Result;
import com.teammatch.m6.entity.Board;
import com.teammatch.m6.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 板块公开查询 Controller
 *
 * 根据详细设计文档 4.3 节定义
 * 用户端接口前缀：/api/boards
 * 供项目创建时选择板块使用
 */
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardPublicController {

    private final BoardService boardService;

    /**
     * 获取启用的板块列表
     * GET /api/boards
     * 供项目创建时选择板块使用
     *
     * @return 启用的板块列表
     */
    @GetMapping
    public Result<List<Board>> listActiveBoards() {
        List<Board> boards = boardService.getActiveBoards();
        return Result.success(boards);
    }
}
