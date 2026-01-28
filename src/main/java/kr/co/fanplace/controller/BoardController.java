package kr.co.fanplace.controller;

import kr.co.fanplace.service.board.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class BoardController
{
    private final BoardService boardService;

    // 게시판
    @GetMapping("/{boardId}")
    public String boardPage(@PathVariable String boardId, Model model)
    {
        var header = boardService.getBoardHeader(boardId);

        // 뷰에서 한 덩어리로 쓰기 좋게
        model.addAttribute("header", header);

        // 네가 기존에 쓰던 방식도 유지(원하면 뷰에서 boardId 직접 사용 가능)
        model.addAttribute("boardId", boardId);

        return "board/board";
    }

    // 게시글
    @GetMapping("/{boardId}/write")
    public String writePage(@PathVariable String boardId, Model model)
    {
        var header = boardService.getBoardHeader(boardId);
        model.addAttribute("header", header);
        model.addAttribute("boardId", boardId);

        return "board/write";
    }

    // 게시글 작성

    // 게시글 작성 요청

    // 게시글 수정

    // 게시글 수정 요청

    // 게시글 삭제
}