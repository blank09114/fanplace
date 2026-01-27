package kr.co.fanplace.setting.security;

import kr.co.fanplace.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class WithdrawCleanupScheduler
{
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanup()
    {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        int deleted = userRepository.deleteWithdrawnBefore(threshold);
    }
}