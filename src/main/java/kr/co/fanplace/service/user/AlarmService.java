package kr.co.fanplace.service.user;

import kr.co.fanplace.entity.board.post.Post;
import kr.co.fanplace.entity.board.post.comment.Comment;
import kr.co.fanplace.entity.board.post.comment.Recomment;
import kr.co.fanplace.entity.user.Alarm;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.repository.user.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AlarmService
{
    private final AlarmRepository alarmRepository;

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
}