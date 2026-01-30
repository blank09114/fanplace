package kr.co.fanplace.repository.user;

import kr.co.fanplace.entity.user.UserSanctionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSanctionLogRepository extends JpaRepository<UserSanctionLog, Long>
{
    // 차단 여부 확인
    Optional<UserSanctionLog> findFirstByUser_IdOrderBySanctionedAtDescIdDesc(String userId);
}