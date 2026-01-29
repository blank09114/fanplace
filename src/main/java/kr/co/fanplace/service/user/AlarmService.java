package kr.co.fanplace.service.user;

import kr.co.fanplace.dto.user.AlarmDTO;
import kr.co.fanplace.entity.board.post.Post;
import kr.co.fanplace.entity.board.post.comment.Comment;
import kr.co.fanplace.entity.board.post.comment.Recomment;
import kr.co.fanplace.entity.user.Alarm;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.repository.board.post.comment.CommentRepository;
import kr.co.fanplace.repository.user.AlarmRepository;
import kr.co.fanplace.setting.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AlarmService
{
    private final SimpMessagingTemplate messagingTemplate;
    private final AlarmRepository alarmRepository;
    private final CommentRepository commentRepository;

    // 댓글 알람 생성
    @Transactional
    public void onNewComment(Post post, Comment comment, User actor, LocalDateTime now)
    {
        User owner = post.getUser();
        if (owner == null) return;
        if (owner.getId() == null) return;
        if (owner.getId().equals(actor.getId())) return;

        alarmRepository.save(Alarm.commentAlarm(owner, comment, now));
    }

    // 대댓글 알람 생성
    @Transactional
    public void onNewRecomment(Comment parentComment, Recomment recomment, User actor, LocalDateTime now)
    {
        User target = (parentComment != null) ? parentComment.getUser() : null;
        if (target == null) return;
        if (target.getId() == null) return;
        if (actor == null || actor.getId() == null) return;

        // 내 댓글에 내가 단 대댓글이면 알람 생성 X
        if (target.getId().equals(actor.getId())) return;

        alarmRepository.save(Alarm.recommentAlarm(target, recomment, now));
    }

    // 알람 목록
    @Transactional(readOnly = true)
    public Page<AlarmDTO.Item> getAlarmPage(int page, boolean unreadOnly)
    {
        String userId = SecurityContextHelper.requireUserId();

        PageRequest pageable = PageRequest.of(Math.max(page, 0), 10);

        Page<Alarm> alarms = unreadOnly
        ? alarmRepository.findByUser_IdAndCheckedAtIsNullOrderByIdDesc(userId, pageable)
        : alarmRepository.findByUser_IdOrderByIdDesc(userId, pageable);

        return alarms.map(this::toItem);
    }

    // 알람 읽음 처리
    @Transactional
    public String clickAndGetTargetUrl(Long alarmId)
    {
        String userId = SecurityContextHelper.requireUserId();

        Alarm alarm = alarmRepository.findByIdAndUser_Id(alarmId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알람을 찾을 수 없습니다."));

        if (alarm.getCheckedAt() == null) alarm.setCheckedAt(LocalDateTime.now()); // <- setter 없으면 메서드로 바꿔줘야 함

        // 저장
        return buildTargetUrl(alarm);
    }

    // 푸시 알림
    private void pushUnreadCount(String userId)
    {
        long cnt = alarmRepository.countByUser_IdAndCheckedAtIsNull(userId);
        messagingTemplate.convertAndSendToUser
        (userId, "/queue/alarm/unread-count", new AlarmDTO.UnreadCount(cnt));
    }

    // 안 읽은 알람 갯수
    @Transactional(readOnly = true)
    public long getUnreadCount()
    {
        String userId = SecurityContextHelper.requireUserId();
        return alarmRepository.countByUser_IdAndCheckedAtIsNull(userId);
    }

    // 알람 생성
    private AlarmDTO.Item toItem(Alarm a)
    {
        boolean unread = (a.getCheckedAt() == null);

        AlarmDTO.Type type = (a.getRecomment() != null) ? AlarmDTO.Type.RECOMMENT : AlarmDTO.Type.COMMENT;

        String actorName;
        String preview;

        if (type == AlarmDTO.Type.RECOMMENT)
        {
            // 대댓글 작성자
            var r = a.getRecomment();
            actorName = (r != null && r.getAuthorUser() != null) ? r.getAuthorUser().getName() : "탈퇴 회원";
            preview = (r != null) ? safePreview(r.getContent()) : "";
        }
        else
        {
            // 댓글 작성자
            var c = a.getComment();
            actorName = (c != null && c.getUser() != null) ? c.getUser().getName() : "탈퇴 회원";
            preview = (c != null) ? safePreview(c.getContent()) : "";
        }

        return new AlarmDTO.Item
        (a.getId(), type, unread, a.getAlarmAt(), actorName, preview, buildTargetUrl(a));
    }

    // URL 생성
    private String buildTargetUrl(Alarm a)
    {
        // comment 기반
        if (a.getComment() != null)
        {
            Comment c = a.getComment();
            Post p = c.getPost();
            if (p == null || p.getBoard() == null) return "/";

            String boardId = p.getBoard().getId();
            Long postId = p.getId();
            Long commentId = c.getId();

            int cp = calcCommentPage(postId, commentId, 10);

            return "/" + boardId + "/post/" + postId + "?cp=" + cp + "#comment-" + commentId;
        }

        // recomment 기반
        if (a.getRecomment() != null)
        {
            Recomment r = a.getRecomment();
            Comment parent = r.getComment();
            if (parent == null) return "/";

            Post p = parent.getPost();
            if (p == null || p.getBoard() == null) return "/";

            String boardId = p.getBoard().getId();
            Long postId = p.getId();
            Long parentCommentId = parent.getId();

            int cp = calcCommentPage(postId, parentCommentId, 10);

            return "/" + boardId + "/post/" + postId + "?cp=" + cp + "#recomment-" + r.getId();
        }

        return "/";
    }

    // 페이지 추출
    private int calcCommentPage(Long postId, Long commentId, int pageSize)
    {
        long before = commentRepository.countVisibleBefore(postId, commentId);
        return (int)(before / pageSize);
    }

    private String safePreview(String s) { if (s == null) return ""; return s; }
}