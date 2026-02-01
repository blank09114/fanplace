package kr.co.fanplace.repository.user;
import kr.co.fanplace.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String>
{
    // 메일 찾기
    Optional<User> findByMail(String mail);
    boolean existsByMail(String mail);

    // 개인정보 삭제
    @Modifying
    @Query("""
        delete from User u
        where u.withdraw = true
            and u.withdrawAt is not null
            and u.withdrawAt < :threshold
    """)
    int deleteWithdrawnBefore(@Param("threshold") LocalDateTime threshold);

    @Query("""
        select (count(u) > 0) from User u
        where u.name = :name and u.id <> :userId
    """)
    boolean existsByNameAndIdNot(@Param("name") String name, @Param("userId") String userId);
}