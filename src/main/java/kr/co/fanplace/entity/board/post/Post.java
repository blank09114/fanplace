package kr.co.fanplace.entity.board.post;

import jakarta.persistence.*;
import kr.co.fanplace.entity.board.Board;
import kr.co.fanplace.entity.board.Category;
import kr.co.fanplace.entity.user.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "post_tbl")
public class Post
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "post_date", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "post_ip", length = 255, nullable = false)
    private String ip;

    @Column(name = "post_is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "post_deleted_reason", length = 100)
    private String deletedReason;

    @Column(name = "post_deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public static Post create(Board board, Category category, User userOrNull, String ip, LocalDateTime now)
    {
        return Post.builder().board(board).category(category).user(userOrNull).ip(ip)
        .createdAt(now).deleted(false).build();
    }
}