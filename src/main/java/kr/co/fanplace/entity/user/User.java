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
    name = "user_tbl",
    uniqueConstraints = @UniqueConstraint(name = "uq_user_mail", columnNames = "user_mail")
)
public class User
{
    @Id
    @Column(name = "user_id", length = 20, nullable = false)
    private String id;

    @Column(name = "user_mail", length = 255, nullable = false)
    private String mail;

    @Column(name = "user_name", length = 10, nullable = false)
    private String name;

    @Column(name = "user_pw", length = 255, nullable = false)
    private String password;

    @Column(name = "user_pw_change_at")
    private LocalDateTime passwordChangedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", length = 5, nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "user_date", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_ip", length = 255, nullable = false)
    private String ip;

    @Column(name = "user_is_enabled", nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "user_is_withdraw", nullable = false)
    @Builder.Default
    private boolean withdraw = false;

    @PrePersist
    void prePersist()
    {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (role == null) role = UserRole.USER;
    }

    public enum UserRole { USER, ADMIN }

    public void enable() { this.enabled = true; }

    public void cancelWithdraw() { this.withdraw = false; }
}