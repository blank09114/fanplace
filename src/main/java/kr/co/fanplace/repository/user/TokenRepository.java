package kr.co.fanplace.repository.user;

import kr.co.fanplace.entity.user.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long>
{
    // 토큰 조회
    @Query("select t from Token t join fetch t.user where t.type = kr.co.fanplace.entity.user.Token$TokenType.JOIN and t.hash = :hash")
    Optional<Token> findJoinByHash(@Param("hash") String hash);

    // 기존 활성 JOIN 토큰 만료처리
    @Modifying
    @Query("""
        update Token t set t.expiresAt = :now
        where t.type = kr.co.fanplace.entity.user.Token$TokenType.JOIN
            and t.user.id = :userId
            and t.usedAt is null
            and t.expiresAt > :now
    """)
    int expireActiveJoinTokens(@Param("userId") String userId, @Param("now") LocalDateTime now);
}