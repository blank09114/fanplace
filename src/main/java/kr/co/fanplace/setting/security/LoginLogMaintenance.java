package kr.co.fanplace.setting.security;

import kr.co.fanplace.service.user.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LoginLogMaintenance
{
    private static final Logger log = LoggerFactory.getLogger(LoginLogMaintenance.class);
    private final LoginLogService loginLogService;

    // 세션 만료 시 logout 기록 남기기
    @EventListener
    @Transactional
    public void onSessionDestroyed(SessionDestroyedEvent event)
    {
        if (event == null) return;
        loginLogService.markLogoutBySessionId(event.getId());
    }

    // 서버 시작 시 logout_at이 null인 로그들 강제 로그아웃 처리
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup()
    {
        LocalDateTime now = LocalDateTime.now();
        int updated = loginLogService.forceLogoutAllActive(now);
        log.info("[Startup] forced logout logs = {}", updated);
    }

    // 로그인 기록 자동 삭제
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupOldLogs()
    {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = loginLogService.deleteLogsLoggedOutBefore(cutoff);
        log.info("[Cleanup] deleted login logs = {}", deleted);
    }
}