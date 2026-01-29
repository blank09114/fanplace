package kr.co.fanplace.api.board;

import jakarta.validation.Valid;
import kr.co.fanplace.dto.ApiOk;
import kr.co.fanplace.dto.board.CommentDTO;
import kr.co.fanplace.service.board.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
public class CommentAPI
{
    private final CommentService commentService;

    // 댓글 조회
    @GetMapping("/{postId}/comments")
    public Page<CommentDTO.Item> getComments(@PathVariable Long postId, @RequestParam(defaultValue = "0") int page)
    { return commentService.getCommentPage(postId, page); }

    // 댓글 작성
    @PostMapping("/{postId}/comment")
    public CommentDTO.WriteRes write(@PathVariable Long postId, @Valid @RequestBody CommentDTO.WriteReq req)
    { return commentService.writeComment(postId, req); }

    // 대댓글 작성
    @PostMapping("/comment/{commentId}/recomment")
    public CommentDTO.RecommentWriteRes writeRecomment(@PathVariable Long commentId, @Valid @RequestBody CommentDTO.RecommentWriteReq req)
    { return commentService.writeRecomment(commentId, req); }

    // 댓글 삭제
    @PostMapping("/comment/{commentId}/delete")
    public ApiOk deleteComment(@PathVariable Long commentId, @RequestParam(required = false) String reason)
    {
        commentService.deleteComment(commentId, reason);
        return ApiOk.ok();
    }

    // 대댓글 삭제
    @PostMapping("/recomment/{recommentId}/delete")
    public ApiOk deleteRecomment(@PathVariable Long recommentId, @RequestParam(required = false) String reason)
    {
        commentService.deleteRecomment(recommentId, reason);
        return ApiOk.ok();
    }
}