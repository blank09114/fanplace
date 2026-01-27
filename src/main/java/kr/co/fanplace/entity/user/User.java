package kr.co.fanplace.entity.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_tbl")
public class User
{
    @Id
    @Column(name = "user_id", length = 20, nullable = false)
    private String id;

    @Column(name = "user_mail", length = 255, nullable = false, unique = true)
    private String mail;

    @Column(name = "user_name", length = 10, nullable = false)
    private String name;

    @Column(name = "user_pw", length = 255, nullable = false)
    private String password;

    @Column(name = "user_pw_change_at")
    private LocalDateTime passwordChangedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", length = 5, nullable = false)
    private UserRole role = UserRole.USER;

    @Column(name = "user_date", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_ip", length = 255, nullable = false)
    private String ip;

    @Column(name = "user_is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "user_is_withdraw", nullable = false)
    private boolean withdraw;
}