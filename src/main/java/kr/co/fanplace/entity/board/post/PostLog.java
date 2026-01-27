package kr.co.fanplace.entity.board.post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "post_log_tbl")
public class PostLog
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_log_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "post_title", length = 100, nullable = false)
    private String title;

    @Lob
    @Column(name = "post_content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "post_update_date", nullable = false)
    private LocalDateTime updatedAt;
}