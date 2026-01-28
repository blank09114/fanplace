package kr.co.fanplace.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.co.fanplace.dto.board.PostDTO;
import kr.co.fanplace.service.board.BoardService;
import kr.co.fanplace.service.board.PostService;
import kr.co.fanplace.setting.ip.IpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class BoardController
{
    private final BoardService boardService;
    private final PostService postService;

    // 게시판
    @GetMapping("/{boardId}")
    public String boardPage(@PathVariable String boardId, Model model)
    {
        var header = boardService.getBoardHeader(boardId);

        model.addAttribute("header", header);
        model.addAttribute("boardId", boardId);

        return "board/board";
    }

    // 게시글

    // 게시글 작성 페이지
    @GetMapping("/{boardId}/write")
    public String writePage(@PathVariable String boardId, Model model)
    {
        var header = boardService.getBoardHeader(boardId);
        model.addAttribute("header", header);
        model.addAttribute("boardId", boardId);

        return "board/write";
    }

    // 게시글 작성
    @PostMapping("/{boardId}/write")
    public String submitWrite(
            @PathVariable String boardId,
            @Valid @ModelAttribute("form") PostDTO.CreateForm form,
            BindingResult br,
            Model model
    ) {
        if (br.hasErrors()) return writePage(boardId, model);
        Long postId = postService.createPost(boardId, form);
        return "redirect:/" + boardId;
    }

    // 게시글 수정 페이지

    // 게시글 수정

    // 게시글 삭제
}