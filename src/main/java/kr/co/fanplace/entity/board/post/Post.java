package kr.co.fanplace.entity.board.post;

import jakarta.persistence.*;
import kr.co.fanplace.entity.board.Board;
import kr.co.fanplace.entity.board.Category;
import kr.co.fanplace.entity.user.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private boolean deleted;

    @Column(name = "post_deleted_reason", length = 100)
    private String deletedReason;

    @Column(name = "post_deleted_at")
    private LocalDateTime deletedAt;
}