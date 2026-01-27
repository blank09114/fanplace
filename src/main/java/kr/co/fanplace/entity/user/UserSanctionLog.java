package kr.co.fanplace.entity.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_sanction_log_tbl")
public class UserSanctionLog
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sanction_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sanction_long", nullable = false)
    private int sanctionLong; // CHECK(0, 1, 7, 30)

    @Column(name = "sanction_reason", length = 100, nullable = false)
    private String reason;

    @Column(name = "sanction_at", nullable = false)
    private LocalDateTime sanctionedAt;
}