package kr.co.fanplace.entity.board.post.comment;

import jakarta.persistence.*;
import kr.co.fanplace.entity.user.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "recomment_tbl")
public class Recomment
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recomment_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mention_user_id")
    private User mentionUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private User authorUser;

    @Column(name = "recomment_date", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "recomment_content", length = 500, nullable = false)
    private String content;

    @Column(name = "recomment_is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "recomment_deleted_reason", length = 100)
    private String deletedReason;

    @Column(name = "recomment_deleted_at")
    private LocalDateTime deletedAt;
}