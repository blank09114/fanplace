package kr.co.fanplace.controller;

import jakarta.validation.Valid;
import kr.co.fanplace.dto.board.BoardDTO;
import kr.co.fanplace.dto.board.PostDTO;
import kr.co.fanplace.service.board.BoardService;
import kr.co.fanplace.service.board.PostService;
import kr.co.fanplace.service.user.UserSanctionService;
import kr.co.fanplace.setting.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class BoardController
{
    private final BoardService boardService;
    private final PostService postService;
    private final UserSanctionService userSanctionService;

    // 게시판
    @GetMapping("/{boardId}")
    public String boardPage(@PathVariable String boardId, Model model)
    {
        var header = boardService.getBoardHeader(boardId);

        model.addAttribute("header", header);
        model.addAttribute("boardId", boardId);

        String loginUserId = SecurityContextHelper.userIdOrNull();
        boolean canWrite = (loginUserId != null) && !userSanctionService.isBlocked(loginUserId);
        model.addAttribute("canWrite", canWrite);
        model.addAttribute("isNotice", "notice".equals(boardId));

        return "board/board";
    }

    // 게시글
    @GetMapping("/{boardId}/post/{postId}")
    public String postPage(@PathVariable String boardId, @PathVariable Long postId, Model model)
    {
        postService.recordPostViewIfNeeded(postId);

        model.addAttribute("header", boardService.getBoardHeader(boardId));
        model.addAttribute("boardId", boardId);

        var page = postService.getPostPage(boardId, postId);

        model.addAttribute("post", page.getPost());
        model.addAttribute("prevPostId", page.getPrevPostId());
        model.addAttribute("nextPostId", page.getNextPostId());

        model.addAttribute("login", page.isLogin());
        model.addAttribute("canLike", page.isCanLike());
        model.addAttribute("canEdit", page.isCanEdit());
        model.addAttribute("canDelete", page.isCanDelete());
        model.addAttribute("canAdminDelete", page.isCanAdminDelete());
        model.addAttribute("canChangeDeletedReason", page.isCanChangeDeletedReason());

        String loginUserId = SecurityContextHelper.userIdOrNull();
        boolean canWrite = (loginUserId != null) && !userSanctionService.isBlocked(loginUserId);
        model.addAttribute("canWrite", canWrite);

        return "board/post";
    }

    // 게시글 작성 페이지
    @GetMapping("/{boardId}/write")
    public String writePage(@PathVariable String boardId, Model model)
    {
        var header = boardService.getBoardHeader(boardId);
        model.addAttribute("header", header);
        model.addAttribute("boardId", boardId);
        model.addAttribute("form", new PostDTO.CreateForm());
        if ("notice".equals(boardId) && !SecurityContextHelper.isAdmin())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return "board/write";
    }

    // 게시글 작성
    @PostMapping("/{boardId}/write")
    public String submitWrite(
        @PathVariable String boardId, @Valid @ModelAttribute("form") PostDTO.CreateForm form,
        BindingResult br, Model model
    ) {
        if (br.hasErrors())
        {
            model.addAttribute("mode", "create");
            return writePage(boardId, model);
        }
        Long postId = postService.createPost(boardId, form);
        if ("notice".equals(boardId) && !SecurityContextHelper.isAdmin())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return "redirect:/" + boardId + "/post/" + postId;
    }

    // 게시글 수정 페이지
    @GetMapping("/{boardId}/post/{postId}/edit")
    public String editPage(@PathVariable String boardId, @PathVariable Long postId, Model model)
    {
        model.addAttribute("header", boardService.getBoardHeader(boardId));
        model.addAttribute("boardId", boardId);
        model.addAttribute("postId", postId);

        model.addAttribute("mode", "edit");
        model.addAttribute("form", postService.getEditForm(boardId, postId));

        return "board/write";
    }

    // 게시글 수정
    @PostMapping("/{boardId}/post/{postId}/edit")
    public String submitEdit(
        @PathVariable String boardId, @PathVariable Long postId,
        @Valid @ModelAttribute("form") PostDTO.CreateForm form,
        BindingResult br, Model model
    ) {
        if (br.hasErrors())
        {
            model.addAttribute("mode", "edit");
            return editPage(boardId, postId, model);
        }

        postService.editPost(boardId, postId, form);
        return "redirect:/" + boardId + "/post/" + postId;
    }

    // 게시글 삭제
    @PostMapping("/{boardId}/post/{postId}/delete")
    public String submitDelete(
        @PathVariable String boardId,
        @PathVariable Long postId,
        @ModelAttribute("reason") String reason
    ) {
        boolean adminDelete = postService.deletePost(boardId, postId, reason);
        if (!adminDelete) return "redirect:/" + boardId;
        return "redirect:/" + boardId + "/post/" + postId;
    }

    // 삭제 사유 변경
    @PostMapping("/{boardId}/post/{postId}/deleted-reason")
    public String submitDeletedReason(
        @PathVariable String boardId,
        @PathVariable Long postId,
        @ModelAttribute("reason") String reason
    ) {
        postService.changeDeletedReason(boardId, postId, reason);
        return "redirect:/" + boardId + "/post/" + postId;
    }

    // 통합 검색 결과
    @GetMapping("/search")
    public String univSearchPage(@RequestParam(required = false) String q, Model model)
    {
        var header = new BoardDTO.Header("univ", "통합 검색 결과", List.of());
        model.addAttribute("header", header);
        model.addAttribute("boardId", "univ");
        model.addAttribute("univSearch", true);
        String loginUserId = SecurityContextHelper.userIdOrNull();
        boolean canWrite = (loginUserId != null) && !userSanctionService.isBlocked(loginUserId);
        model.addAttribute("canWrite", canWrite);

        return "board/board";
    }
}