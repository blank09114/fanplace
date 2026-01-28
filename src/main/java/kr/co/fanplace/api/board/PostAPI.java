package kr.co.fanplace.api.board;

import kr.co.fanplace.dto.board.PostDTO;
import kr.co.fanplace.service.board.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
public class PostAPI
{
    private final PostService postService;

    // 좋아요
    @PostMapping("/{postId}/like")
    public PostDTO.LikeRes like(@PathVariable Long postId)
    { return postService.likePost(postId); }

    // 좋아요 취소
    @DeleteMapping("/{postId}/like")
    public PostDTO.LikeRes unlike(@PathVariable Long postId)
    { return postService.unlikePost(postId); }

    // 좋아요 상태/카운트
    @GetMapping("/{postId}/like")
    public PostDTO.LikeRes status(@PathVariable Long postId)
    { return postService.getLikeStatus(postId); }
}