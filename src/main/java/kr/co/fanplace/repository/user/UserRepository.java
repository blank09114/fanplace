package kr.co.fanplace.repository.user;
import kr.co.fanplace.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String>
{
    // 메일 찾기
    Optional<User> findByMail(String mail);
    boolean existsByMail(String mail);
}