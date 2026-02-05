package kr.co.fanplace.service.board;

import jakarta.persistence.EntityManager;
import kr.co.fanplace.dto.board.PostDTO;
import kr.co.fanplace.dto.user.MyActivityDTO;
import kr.co.fanplace.entity.board.Board;
import kr.co.fanplace.entity.board.Category;
import kr.co.fanplace.entity.board.post.Post;
import kr.co.fanplace.entity.board.post.PostLike;
import kr.co.fanplace.entity.board.post.PostLog;
import kr.co.fanplace.entity.board.post.PostView;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.repository.board.BoardRepository;
import kr.co.fanplace.repository.board.CategoryRepository;
import kr.co.fanplace.repository.board.post.PostLikeRepository;
import kr.co.fanplace.repository.board.post.PostLogRepository;
import kr.co.fanplace.repository.board.post.PostRepository;
import kr.co.fanplace.repository.board.post.PostViewRepository;
import kr.co.fanplace.repository.board.post.comment.CommentRepository;
import kr.co.fanplace.repository.board.post.comment.RecommentRepository;
import kr.co.fanplace.repository.user.UserRepository;
import kr.co.fanplace.service.user.AlarmService;
import kr.co.fanplace.service.user.UserSanctionService;
import kr.co.fanplace.setting.ip.GeoIpService;
import kr.co.fanplace.setting.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService
{
    private final BoardRepository boardRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostLogRepository postLogRepository;
    private final PostViewRepository postViewRepository;
    private final PostLikeRepository postLikeRepository;

    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;
    private final AlarmService alarmService;
    private final UserSanctionService userSanctionService;

    private final CommentService commentService;
    private final GeoIpService geoIpService;

    private final EntityManager em;

    // 게시글 상세 조회
    @Transactional(readOnly = true)
    public PostDTO.PostPage getPostPage(String boardId, Long postId)
    {
        Post post = postRepository.findByIdAndBoard_Id(postId, boardId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        PostLog latestLog = postLogRepository.findFirstByPost_IdOrderByUpdatedAtDescIdDesc(postId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "게시글 로그가 없습니다."));

        // 로그인/권한 판별
        boolean login = SecurityContextHelper.isLogin();
        String loginUserId = SecurityContextHelper.userIdOrNull();
        boolean isAdmin = SecurityContextHelper.isAdmin();

        if (post.isDeleted() && !isAdmin) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제된 글입니다."); }

        String authorUserId = (post.getUser() != null) ? post.getUser().getId() : null;
        String authorName   = (post.getUser() != null) ? post.getUser().getName() : null;

        boolean owner = login && authorUserId != null && authorUserId.equals(loginUserId);

        String deletedReasonDisplay = null;
        if (post.isDeleted())
        {
            String raw = post.getDeletedReason();
            deletedReasonDisplay = (raw == null || raw.isBlank()) ? "본인 삭제" : raw;
        }

        Long prevId = postRepository.findPrevPostId(boardId, isAdmin, postId);
        Long nextId = postRepository.findNextPostId(boardId, isAdmin, postId);

        String region = geoIpService.resolveRegion(post.getIp());
        String ipForView = isAdmin ? post.getIp() : null;

        // 버튼 노출 정책
        boolean canLike = login && !post.isDeleted();
        if (canLike && loginUserId != null && userSanctionService.isBlocked(loginUserId)) { canLike = false; }
        boolean canEdit = owner && !post.isDeleted();
        boolean canDelete = owner && !post.isDeleted();
        boolean canAdminDelete = isAdmin && !post.isDeleted();
        boolean canChangeDeletedReason = isAdmin && post.isDeleted();

        // 조회/좋아요/댓글 수
        long viewCount = postViewRepository.countByPost_Id(postId);
        long likeCount = postLikeRepository.countByPost_Id(postId);
        long commentCount = commentService.getTotalCommentCount(postId);

        PostDTO.DetailView detail = new PostDTO.DetailView(
            post.getId(), post.getBoard().getId(), post.getBoard().getName(),
            post.getCategory().getId(), post.getCategory().getName(), authorUserId,
            authorName, post.getCreatedAt(), ipForView, region,
            post.isDeleted(), deletedReasonDisplay, post.getDeletedAt(),
            latestLog.getTitle(), latestLog.getContent(), latestLog.getUpdatedAt(),
            viewCount, likeCount, commentCount
        );

        return new PostDTO.PostPage(
            detail, prevId, nextId, login, canLike, canEdit,
            canDelete, canAdminDelete, canChangeDeletedReason
        );
    }

    // 조회수 증가
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPostViewIfNeeded(Long postId)
    {
        String userId = SecurityContextHelper.userIdOrNull();
        String ip = SecurityContextHelper.clientIp();

        // 하나의 계정/IP당 한 번만 발생
        if (userId != null)
        {
            if (postViewRepository.existsByPost_IdAndUser_Id(postId, userId)) return;
            if (postViewRepository.existsByPost_IdAndUserIp(postId, ip)) return;
        } else { if (postViewRepository.existsByPost_IdAndUserIp(postId, ip)) return; }

        try
        {
            Post postRef = em.getReference(Post.class, postId);
            User userRef = (userId != null) ? em.getReference(User.class, userId) : null;

            PostView view = PostView.create(postRef, userRef, ip);
            postViewRepository.save(view);
        } catch (DataIntegrityViolationException e) { }
    }

    // 좋아요
    @Transactional
    public PostDTO.LikeRes likePost(Long postId)
    {
        String userId = SecurityContextHelper.userIdOrNull();
        userSanctionService.assertWritable(userId);

        // 삭제된 글 방어
        Post post = postRepository.findById(postId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));
        if (post.isDeleted())
        { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제된 글입니다."); }

        // 이미 좋아요 했는지
        if (postLikeRepository.existsByPost_IdAndUser_Id(postId, userId))
        { throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 좋아요한 게시글"); }

        String ip = SecurityContextHelper.clientIp();

        Post postRef = em.getReference(Post.class, postId);
        User userRef = em.getReference(User.class, userId);

        PostLike like = PostLike.create(postRef, userRef, ip);
        postLikeRepository.save(like);

        long count = postLikeRepository.countByPost_Id(postId);
        return new PostDTO.LikeRes(true, count);
    }

    // 좋아요 취소
    @Transactional
    public PostDTO.LikeRes unlikePost(Long postId)
    {
        String userId = SecurityContextHelper.requireUserId();
        userSanctionService.assertWritable(userId);

        long deleted = postLikeRepository.deleteByPost_IdAndUser_Id(postId, userId);
        if (deleted == 0) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "좋아요 기록 없음"); }

        long count = postLikeRepository.countByPost_Id(postId);
        return new PostDTO.LikeRes(false, count);
    }

    // 좋아요 상태 조회
    @Transactional(readOnly = true)
    public PostDTO.LikeRes getLikeStatus(Long postId)
    {
        String userId = SecurityContextHelper.userIdOrNull();

        boolean liked = false;
        if (userId != null)
        { liked = postLikeRepository.existsByPost_IdAndUser_Id(postId, userId); }

        long count = postLikeRepository.countByPost_Id(postId);
        return new PostDTO.LikeRes(liked, count);
    }

    // 게시글 작성
    @Transactional
    public Long createPost(String boardId, PostDTO.CreateForm form)
    {
        LocalDateTime now = LocalDateTime.now();

        String userId = SecurityContextHelper.requireUserId();
        userSanctionService.assertWritable(userId);

        String clientIp = SecurityContextHelper.clientIp();

        Board board = boardRepository.findById(boardId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시판입니다."));

        Category category = categoryRepository.findById(form.getCategoryId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리입니다."));

        if (!category.getBoard().getId().equals(boardId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시판과 카테고리가 일치하지 않습니다.");

        User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 유효하지 않습니다."));

        Post post = Post.create(board, category, user, clientIp, now);
        postRepository.save(post);

        PostLog log = PostLog.create(post, form.getTitle(), form.getContent(), now);
        postLogRepository.save(log);

        return post.getId();
    }

    // 수정 폼 로딩
    @Transactional(readOnly = true)
    public PostDTO.CreateForm getEditForm(String boardId, Long postId)
    {
        Post post = postRepository.findByIdAndBoard_Id(postId, boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (post.isDeleted())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제된 글입니다.");

        assertEditableByOwner(post); // 작성자 본인만(관리자라도 본인 글이면 OK)

        PostLog latestLog = postLogRepository.findFirstByPost_IdOrderByUpdatedAtDescIdDesc(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "게시글 로그가 없습니다."));

        PostDTO.CreateForm form = new PostDTO.CreateForm();
        form.setCategoryId(post.getCategory().getId());
        form.setTitle(latestLog.getTitle());
        form.setContent(latestLog.getContent());

        return form;
    }

    // 수정 요청
    @Transactional
    public void editPost(String boardId, Long postId, PostDTO.CreateForm form)
    {
        LocalDateTime now = LocalDateTime.now();

        Post post = postRepository.findByIdAndBoard_Id(postId, boardId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (post.isDeleted())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제된 글입니다.");

        assertEditableByOwner(post);

        // 카테고리-게시판 정합성 + 변경 최소화
        if (!post.getCategory().getId().equals(form.getCategoryId()))
        {
            Category category = categoryRepository.findById(form.getCategoryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리입니다."));

            if (!category.getBoard().getId().equals(boardId))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시판과 카테고리가 일치하지 않습니다.");

            post.changeCategory(category);
        }

        PostLog log = PostLog.create(post, form.getTitle(), form.getContent(), now);
        postLogRepository.save(log);
    }

    // 삭제
    @Transactional
    public boolean deletePost(String boardId, Long postId, String reason)
    {
        String userId = SecurityContextHelper.requireUserId();
        LocalDateTime now = LocalDateTime.now();

        Post post = postRepository.findByIdAndBoard_Id(postId, boardId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (post.isDeleted())
        { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 삭제된 글입니다."); }

        boolean isAdmin = SecurityContextHelper.isAdmin();

        String authorUserId = (post.getUser() != null) ? post.getUser().getId() : null;
        boolean owner = authorUserId != null && authorUserId.equals(userId);

        // 삭제 권한: 작성자 or 관리자
        if (!owner && !isAdmin)
        { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다."); }

        boolean adminDelete = !owner && isAdmin;
        if (adminDelete)
        {
            if (reason == null || reason.isBlank())
            { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제 사유를 입력하세요."); }

            post.softDelete(reason, now);
        }
        else { post.softDelete(null, now); }

        List<Long> commentIds = commentRepository.findIdsByPostIds(List.of(postId));
        if (!commentIds.isEmpty())
        {
            // 대댓글 알람 먼저 삭제
            List<Long> recommentIds = recommentRepository.findIdsByCommentIds(commentIds);
            alarmService.hardDeleteByRecommentIds(recommentIds);

            // 댓글 알람 삭제
            for (Long cid : commentIds) { alarmService.hardDeleteByCommentId(cid); }
        }

        return adminDelete;
    }

    // 식제 사유 변경
    @Transactional
    public void changeDeletedReason(String boardId, Long postId, String reason)
    {
        // 관리자만
        boolean isAdmin = SecurityContextHelper.isAdmin();

        if (!isAdmin) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 변경할 수 있습니다.");

        if (reason == null || reason.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제 사유를 입력하세요.");

        Post post = postRepository.findByIdAndBoard_Id(postId, boardId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (!post.isDeleted())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제된 글만 사유 변경이 가능합니다.");

        post.changeDeletedReason(reason);
    }

    // 특정인 게시글 조회
    @Transactional(readOnly = true)
    public Page<MyActivityDTO.PostItem> getUserPostPagePublic(String userId, int page, int size)
    {
        boolean admin = SecurityContextHelper.isAdmin();

        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findUserPostPage(userId, admin, pageable);
    }

    // 본인 여부 검증
    private void assertEditableByOwner(Post post)
    {
        String userId = SecurityContextHelper.requireUserId();
        String authorUserId = (post.getUser() != null) ? post.getUser().getId() : null;
        if (authorUserId == null || !authorUserId.equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
    }

    // 최신 공지사항
    @Transactional(readOnly = true)
    public List<PostDTO.ListItem> getRecentNotices()
    {
        LocalDateTime to = java.time.LocalDate.now().plusDays(1).atStartOfDay(); // 내일 00:00
        LocalDateTime from = to.minusDays(7);

        return postRepository.findRecentBoardPosts(
            "notice", from, to,
            org.springframework.data.domain.PageRequest.of(0, 5)
        );
    }

    // 최신글
    @Transactional(readOnly = true)
    public List<PostDTO.ListItem> getLatestPosts()
    { return postRepository.findLatestPosts(org.springframework.data.domain.PageRequest.of(0, 5)); }

    // 삭제된 글 조회
    @Transactional(readOnly = true)
    public Page<PostDTO.DeletedListItem> getDeletedPostPage(int page)
    {
        int safePage = Math.max(page, 0);
        PageRequest pageable = PageRequest.of(safePage, 10);
        return postRepository.findDeletedPostPage(pageable);
    }
}