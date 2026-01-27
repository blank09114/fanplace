package kr.co.fanplace.entity.user;

import jakarta.persistence.*;
import kr.co.fanplace.entity.board.post.comment.Comment;
import kr.co.fanplace.entity.board.post.comment.Recomment;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
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
}