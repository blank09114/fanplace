package kr.co.fanplace.entity.user;

import jakarta.persistence.*;
import kr.co.fanplace.entity.board.post.comment.Comment;
import kr.co.fanplace.entity.board.post.comment.Recomment;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "alarm_tbl")
public class Alarm
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recomment_id")
    private Recomment recomment;

    @Column(name = "alarm_at", nullable = false)
    private LocalDateTime alarmAt;

    @Column(name = "alarm_check_at")
    private LocalDateTime checkedAt;

    @PrePersist
    void prePersist() { if (alarmAt == null) alarmAt = LocalDateTime.now(); }

    public static Alarm commentAlarm(User target, Comment comment, LocalDateTime now)
    {
        Alarm a = new Alarm();
        a.user = target;
        a.comment = comment;
        a.alarmAt = now;
        return a;
    }

    public static Alarm recommentAlarm(User target, Recomment recomment, LocalDateTime now)
    {
        Alarm a = new Alarm();
        a.user = target;
        a.comment = null;
        a.recomment = recomment;
        a.alarmAt = now;
        a.checkedAt = null;
        return a;
    }
}