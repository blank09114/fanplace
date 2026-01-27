package kr.co.fanplace.entity.board.post.comment;

import jakarta.persistence.*;
import kr.co.fanplace.entity.board.post.Post;
import kr.co.fanplace.entity.user.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "comment_tbl")
public class Comment
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "comment_date", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "comment_content", length = 500, nullable = false)
    private String content;

    @Column(name = "comment_is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "comment_deleted_reason", length = 100)
    private String deletedReason;

    @Column(name = "comment_deleted_at")
    private LocalDateTime deletedAt;
}