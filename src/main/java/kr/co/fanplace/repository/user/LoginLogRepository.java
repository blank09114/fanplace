package kr.co.fanplace.repository.user;

import kr.co.fanplace.entity.user.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // 열린 로그 전부 닫기
    @Modifying
    @Query("""
        update LoginLog l set l.logoutAt = :now
        where l.logoutAt is null
    """)
    int forceLogoutAllActive(@Param("now") LocalDateTime now);

    // 로그인 기록 자동 삭제
    @Modifying
    @Query("""
        delete from LoginLog l
        where l.logoutAt is not null and l.logoutAt <= :cutoff
    """)
    int deleteByLogoutAtBeforeOrEqual(@Param("cutoff") LocalDateTime cutoff);

    // 로그인 기록 조회
    Page<LoginLog> findByUser_IdOrderByLoginAtDesc(String userId, Pageable pageable);
}