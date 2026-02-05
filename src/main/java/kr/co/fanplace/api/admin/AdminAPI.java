package kr.co.fanplace.api.admin;

import jakarta.validation.Valid;
import kr.co.fanplace.dto.ApiOk;
import kr.co.fanplace.dto.board.CommentDTO;
import kr.co.fanplace.dto.board.PostDTO;
import kr.co.fanplace.dto.user.UserInfoDTO;
import kr.co.fanplace.dto.user.UserSanctionDTO;
import kr.co.fanplace.service.board.CommentService;
import kr.co.fanplace.service.board.PostService;
import kr.co.fanplace.service.user.UserSanctionService;
import kr.co.fanplace.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAPI
{
    private final UserSanctionService userSanctionService;
    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    // 사용자 차단
    @PostMapping("/user/{userId}/sanction")
    public UserSanctionDTO.CreateRes sanction(@PathVariable String userId, @RequestBody @Valid UserSanctionDTO.CreateReq req)
    {
        Long id = userSanctionService.createSanctionLog(userId, req);
        return new UserSanctionDTO.CreateRes(id);
    }

    // 제재 내역 삭제
    @DeleteMapping("/user/{userId}/sanction/logs/{sanctionId}")
    public ResponseEntity<ApiOk> deleteSanctionLog(@PathVariable String userId, @PathVariable Long sanctionId)
    {
        userSanctionService.deleteSanctionLog(userId, sanctionId);
        return ResponseEntity.ok(ApiOk.ok());
    }

    // 회원목록
    @GetMapping("/users")
    public Page<UserInfoDTO.Card> getUserList(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) { return userService.getUserListPageAdmin(page, size); }

    // 삭제된 글 목록
    @GetMapping("/deleted-posts")
    public Page<PostDTO.DeletedListItem> getDeletedPosts
    (@RequestParam(defaultValue = "0") int page)
    { return postService.getDeletedPostPage(page); }

    // 식제된 글 검색
    @GetMapping("/deleted-posts/search")
    public Page<PostDTO.DeletedListItem> searchDeletedPostsByTitle
    (@RequestParam String q, @RequestParam(defaultValue = "0") int page)
    { return postService.searchDeletedPostPageByTitle(page, q); }

    // mnt/data/AdminAPI.java

    // 삭제된 댓글/대댓글 목록
    @GetMapping("/deleted-comments")
    public Page<CommentDTO.DeletedListItem> getDeletedComments
    (@RequestParam(defaultValue = "0") int page)
    { return commentService.getDeletedCommentPage(page, 10, null); }

    // 삭제된 댓글/대댓글 검색
    @GetMapping("/deleted-comments/search")
    public Page<CommentDTO.DeletedListItem> searchDeletedComments
    (@RequestParam String q, @RequestParam(defaultValue = "0") int page)
    { return commentService.getDeletedCommentPage(page, 10, q); }
}