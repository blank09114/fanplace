package kr.co.fanplace.entity.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "login_log_tbl")
public class LoginLog
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_log_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", length = 64, nullable = false)
    private String tokenHash;

    @Column(name = "login_ip", length = 255, nullable = false)
    private String loginIp;

    @Column(name = "login_date", nullable = false)
    private LocalDateTime loginAt;

    @Column(name = "logout_date")
    private LocalDateTime logoutAt;

    public void markLogout(LocalDateTime at) { this.logoutAt = at; }
}