package kr.co.fanplace.service.user;

import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.entity.user.UserSanctionLog;
import kr.co.fanplace.repository.user.UserRepository;
import kr.co.fanplace.repository.user.UserSanctionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserSanctionService
{
    private final UserRepository userRepository;
    private final UserSanctionLogRepository userSanctionLogRepository;

    // 접근 차단
    @Transactional(readOnly = true)
    public void assertWritable(String userId)
    {
        if (userId == null || userId.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (user.isWithdraw()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (isBlocked(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    // 차단 여부 확인
    @Transactional(readOnly = true)
    public boolean isBlocked(String userId)
    {
        UserSanctionLog latest = userSanctionLogRepository
        .findFirstByUser_IdOrderBySanctionedAtDescIdDesc(userId).orElse(null);

        if (latest == null) return false;

        int days = latest.getSanctionLong();
        LocalDateTime start = latest.getSanctionedAt();

        if (days == 0) return true;
        if (days < 0) return true;

        LocalDateTime until = start.plusDays(days);
        return until.isAfter(LocalDateTime.now());
    }
}