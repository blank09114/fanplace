package kr.co.fanplace.service.board;

import kr.co.fanplace.dto.board.CommentDTO;
import kr.co.fanplace.dto.user.MyActivityDTO;
import kr.co.fanplace.entity.board.post.Post;
import kr.co.fanplace.entity.board.post.comment.Comment;
import kr.co.fanplace.entity.board.post.comment.Recomment;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.repository.board.post.PostRepository;
import kr.co.fanplace.repository.board.post.comment.CommentRepository;
import kr.co.fanplace.repository.board.post.comment.RecommentRepository;
import kr.co.fanplace.repository.user.UserRepository;
import kr.co.fanplace.service.user.AlarmService;
import kr.co.fanplace.service.user.UserSanctionService;
import kr.co.fanplace.setting.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService
{
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;

    private final UserSanctionService userSanctionService;
    private final AlarmService alarmService;

    // 댓글 수 카운트
    @Transactional(readOnly = true)
    public long getTotalCommentCount(Long postId)
    {
        long commentCount = commentRepository.countByPost_IdAndDeletedFalse(postId);
        long recommentCount = recommentRepository.countByComment_Post_IdAndDeletedFalseAndComment_DeletedFalse(postId);
        return commentCount + recommentCount;
    }

    // 댓글 조회
    @Transactional(readOnly = true)
    public Page<CommentDTO.Item> getCommentPage(Long postId, int page)
    {
        // 관리자 판별
        boolean isAdmin = SecurityContextHelper.isAdmin();
        String loginUserId = SecurityContextHelper.userIdOrNull();

        // 게시글 존재/삭제 방어(삭제글은 관리자만)
        Post post = postRepository.findById(postId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (post.isDeleted() && !isAdmin)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제된 글입니다.");

        int safePage = Math.max(page, 0);
        PageRequest pageable = PageRequest.of(safePage, 10);

        Page<CommentDTO.Item> parentPage = commentRepository.findCommentPage(postId, isAdmin, loginUserId, pageable);

        List<Long> commentIds = parentPage.getContent().stream()
        .map(CommentDTO.Item::getCommentId).toList();

        if (commentIds.isEmpty()) return parentPage;

        // 대댓글 묶음 조회
        List<CommentDTO.RecommentItem> recomments = recommentRepository.findRecommentItems(commentIds, isAdmin, loginUserId);

        Map<Long, List<CommentDTO.RecommentItem>> grouped = new HashMap<>();
        for (var r : recomments) grouped.computeIfAbsent(r.getCommentId(), k -> new ArrayList<>()).add(r);
        for (var c : parentPage.getContent()) c.setRecomments(grouped.getOrDefault(c.getCommentId(), List.of()));

        return parentPage;
    }

    // 댓글 작성
    @Transactional
    public CommentDTO.WriteRes writeComment(Long postId, CommentDTO.WriteReq req)
    {
        String loginUserId = SecurityContextHelper.requireUserId();
        userSanctionService.assertWritable(loginUserId);
        User actor = userRepository.findById(loginUserId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));

        Post post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        if (post.isDeleted()) throw new IllegalArgumentException("삭제된 글에는 댓글을 작성할 수 없습니다.");

        LocalDateTime now = LocalDateTime.now();

        Comment comment = Comment.create(post, actor, req.getContent(), now);
        commentRepository.save(comment);

        // 내 글에 내가 단 댓글이면 알람 생성 X
        alarmService.onNewComment(post, comment, actor, now);

        return new CommentDTO.WriteRes(comment.getId());
    }

    // 대댓글 작성
    @Transactional
    public CommentDTO.RecommentWriteRes writeRecomment(Long commentId, CommentDTO.RecommentWriteReq req)
    {
        String loginUserId = SecurityContextHelper.requireUserId();
        userSanctionService.assertWritable(loginUserId);
        User actor = userRepository.findById(loginUserId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));

        Comment parent = commentRepository.findById(commentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));

        Post post = parent.getPost();
        if (post == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");

        if (post.isDeleted()) throw new IllegalArgumentException("삭제된 글에는 대댓글을 작성할 수 없습니다.");
        if (parent.isDeleted()) throw new IllegalArgumentException("삭제된 댓글에는 대댓글을 작성할 수 없습니다.");

        LocalDateTime now = LocalDateTime.now();

        // mention: 기본적으로 부모 댓글 작성자
        User mentionUser = parent.getUser();

        Recomment recomment = Recomment.create(parent, mentionUser, actor, req.getContent(), now);
        recommentRepository.save(recomment);

        // 알람: 부모댓글 작성자에게
        alarmService.onNewRecomment(parent, recomment, actor, now);

        return new CommentDTO.RecommentWriteRes(recomment.getId());
    }

    // 댓글 삭제
    @Transactional
    public boolean deleteComment(Long commentId, String reason)
    {
        String loginUserId = SecurityContextHelper.requireUserId();
        LocalDateTime now = LocalDateTime.now();

        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));

        if (c.isDeleted())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 삭제된 댓글입니다.");

        boolean isAdmin = SecurityContextHelper.isAdmin();
        String authorUserId = (c.getUser() != null) ? c.getUser().getId() : null;
        boolean owner = authorUserId != null && authorUserId.equals(loginUserId);

        // 권한: 작성자 or 관리자
        if (!owner && !isAdmin)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");

        boolean adminDelete = !owner && isAdmin;
        if (adminDelete)
        {
            if (reason == null || reason.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제 사유를 입력하세요.");
            c.softDelete(reason, now);
        }
        else { c.softDelete(null, now); }

        // 연쇄 삭제
        recommentRepository.softDeleteByCommentId(commentId, now, "원댓글 삭제");

        alarmService.hardDeleteByCommentId(commentId);

        List<Long> recommentIds = recommentRepository.findIdsByCommentIds(List.of(commentId));
        alarmService.hardDeleteByRecommentIds(recommentIds);

        return adminDelete;
    }

    // 대댓글 삭제
    @Transactional
    public boolean deleteRecomment(Long recommentId, String reason)
    {
        String loginUserId = SecurityContextHelper.requireUserId();
        LocalDateTime now = LocalDateTime.now();

        Recomment r = recommentRepository.findById(recommentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대댓글을 찾을 수 없습니다."));

        if (r.isDeleted())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 삭제된 댓글입니다.");

        boolean isAdmin = SecurityContextHelper.isAdmin();
        String authorUserId = (r.getAuthorUser() != null) ? r.getAuthorUser().getId() : null;
        boolean owner = authorUserId != null && authorUserId.equals(loginUserId);

        if (!owner && !isAdmin)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");

        boolean adminDelete = !owner && isAdmin;
        if (adminDelete)
        {
            if (reason == null || reason.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제 사유를 입력하세요.");

            r.softDelete(reason, now);
            alarmService.hardDeleteByRecommentIds(List.of(recommentId));
        }
        else { r.softDelete(null, now); }

        return adminDelete;
    }

    // 특정인 댓글 조회
    @Transactional(readOnly = true)
    public Page<MyActivityDTO.CommentItem> getUserCommentActivityPage(String userId, int page, int size)
    {
        boolean admin = SecurityContextHelper.isAdmin();

        int safePage = Math.max(0, page);
        int safeSize = (size <= 0 || size > 20) ? 10 : size;

        PageRequest pageable = PageRequest.of(safePage, safeSize);

        Page<CommentRepository.UserActivityCommentRow> rows =
        commentRepository.findUserActivityCommentPage(userId, admin, pageable);

        return rows.map(r -> new MyActivityDTO.CommentItem(
            r.getType(), r.getId(), r.getPostId(), r.getBoardId(), r.getBoardName(),
            r.getCategoryId(), r.getCategoryName(), r.getContent(), r.getCreatedAt()
        ));
    }

    // 삭제된 댓글/대댓글
    @Transactional(readOnly = true)
    public Page<CommentDTO.DeletedListItem> getDeletedCommentPage(int page, int size, String q)
    {
        Pageable pageable = PageRequest.of(page, size);

        Page<CommentRepository.DeletedCommentRow> rows = commentRepository.findDeletedCommentPage(q, pageable);

        return rows.map(r -> {
            String reason = r.getDeletedReason();
            String reasonDisplay = (reason == null || reason.isBlank()) ? "본인 삭제" : reason;

            String authorName = (r.getAuthorName() == null || r.getAuthorName().isBlank())
            ? "탈퇴 회원" : r.getAuthorName();

            return new CommentDTO.DeletedListItem(
                r.getType(), r.getId(), r.getPostId(), r.getBoardId(), r.getBoardName(),
                r.getCategoryId(), r.getCategoryName(), r.getAuthorUserId(), authorName,
                r.getContent(), r.getCreatedAt(), r.getDeletedAt(), reasonDisplay
            );
        });
    }
}