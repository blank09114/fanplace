package kr.co.fanplace.service.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.fanplace.entity.user.LoginLog;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.repository.user.LoginLogRepository;
import kr.co.fanplace.setting.security.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginLogService
{
    private final LoginLogRepository loginLogRepository;

    // 로그인 기록 생성
    @Transactional
    public void recordLogin(User user, HttpSession session, HttpServletRequest request)
    {
        String tokenHash = TokenUtil.sha256Hex(session.getId());

        String ip = request.getRemoteAddr();

        LoginLog log = LoginLog.builder()
        .user(user).tokenHash(tokenHash).loginIp(ip == null ? "" : ip)
        .loginAt(LocalDateTime.now()).logoutAt(null).build();

        loginLogRepository.save(log);
    }

    // 로그아웃 시각 기록
    @Transactional
    public void markLogout(HttpSession session)
    {
        if (session == null) return;

        String tokenHash = TokenUtil.sha256Hex(session.getId());

        loginLogRepository.findOpenByTokenHash(tokenHash).ifPresent(l -> l.markLogout(LocalDateTime.now()));
    }
}