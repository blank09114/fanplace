package kr.co.fanplace.entity.board.post;

import jakarta.persistence.*;
import kr.co.fanplace.entity.user.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "post_like_tbl")
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_like_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_ip", length = 255, nullable = false)
    private String userIp;

    @Column(name = "post_like_date", nullable = false)
    private LocalDateTime likedAt;

    @PrePersist
    void prePersist() { if (likedAt == null) likedAt = LocalDateTime.now(); }

    public static PostLike create(Post postRef, User userRef, String ip)
    {
        PostLike pl = new PostLike();
        pl.post = postRef;
        pl.user = userRef;
        pl.userIp = ip;
        return pl;
    }
}