package kr.co.fanplace.api.board;

import kr.co.fanplace.dto.board.PostDTO;
import kr.co.fanplace.service.board.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board")
public class BoardAPI
{
    private final BoardService boardService;

    // 게시글 목록 조회
    @GetMapping("/{boardId}/posts")
    public Page<PostDTO.ListItem> getPosts(
        @PathVariable String boardId, @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) String categoryId,
        @RequestParam(defaultValue = "false") boolean hot
    ) { return boardService.getPostList(boardId, categoryId, hot, page); }

    // 게시판 내 검색
    @GetMapping("/{boardId}/posts/search")
    public Page<PostDTO.ListItem> searchPosts(
        @PathVariable String boardId, @RequestParam String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) String categoryId,
        @RequestParam(defaultValue = "false") boolean hot
    ) { return boardService.searchPostList(boardId, categoryId, hot, page, q); }

    // 통합 검색
    @GetMapping("/search/posts")
    public Page<PostDTO.UnivListItem> searchUnivPosts(
        @RequestParam String q,
        @RequestParam(defaultValue = "0") int page
    ) { return boardService.searchUnivPostList(page, q); }
}