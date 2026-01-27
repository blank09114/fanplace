package kr.co.fanplace.repository.user;

import kr.co.fanplace.entity.user.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long>
{
    // 현재 세션으로 열린 로그 1건 찾기
    @Query("""
        select l from LoginLog l
        where l.tokenHash = :tokenHash and l.logoutAt is null
        order by l.loginAt desc
    """)
    Optional<LoginLog> findOpenByTokenHash(String tokenHash);
}