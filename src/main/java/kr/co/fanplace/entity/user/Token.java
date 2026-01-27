package kr.co.fanplace.entity.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
    name = "token_tbl", uniqueConstraints = @UniqueConstraint(name = "uq_token_hash", columnNames = "token_hash"),
    indexes = @Index(name = "token_user_lookup", columnList = "user_id, token_expires_at, token_used_at")
)
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_token_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", length = 10, nullable = false)
    private TokenType type;

    @Column(name = "token_hash", length = 64, nullable = false)
    private String hash;

    @Column(name = "token_expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "token_used_at")
    private LocalDateTime usedAt;

    @Column(name = "token_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "token_pw_hash", length = 255)
    private String pwHash;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public boolean isUsed() { return usedAt != null; }

    public boolean isExpired(LocalDateTime now) { return !expiresAt.isAfter(now); }

    public enum TokenType { JOIN, RESET, MAILCHANGE, WITHDRAW }

    public void markUsed(LocalDateTime at) { this.usedAt = at; }
}