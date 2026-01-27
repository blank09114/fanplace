package kr.co.fanplace.entity.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "token_tbl")
public class Token
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", length = 10, nullable = false)
    private TokenType type;

    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String hash;

    @Column(name = "token_expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "token_used_at")
    private LocalDateTime usedAt;

    @Column(name = "token_created_at", nullable = false)
    private LocalDateTime createdAt;
}