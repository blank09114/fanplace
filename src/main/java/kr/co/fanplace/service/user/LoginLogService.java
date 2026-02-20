package kr.co.fanplace.service.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.fanplace.dto.user.LoginLogDTO;
import kr.co.fanplace.entity.user.LoginLog;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.repository.user.LoginLogRepository;
import kr.co.fanplace.repository.user.UserRepository;
import kr.co.fanplace.setting.ip.GeoIpService;
import kr.co.fanplace.setting.ip.IpUtil;
import kr.co.fanplace.setting.security.SecurityContextHelper;
import kr.co.fanplace.setting.security.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginLogService
{
    private final LoginLogRepository loginLogRepository;
    private final UserRepository userRepository;
    private final GeoIpService geoIpService;

    // 로그인 기록 생성
    @Transactional
    public void recordLogin(User user, HttpSession session, HttpServletRequest request)
    {
        String tokenHash = TokenUtil.sha256Hex(session.getId());
        String ip = IpUtil.resolveClientIp(request);

        LoginLog log = LoginLog.builder()
        .user(user).tokenHash(tokenHash).loginIp(ip == null ? "" : ip)
        .loginAt(LocalDateTime.now()).logoutAt(null).build();

        loginLogRepository.save(log);
    }

    // 로그아웃 시각 기록
    @Transactional
    public void markLogoutBySessionId(String sessionId)
    {
        if (sessionId == null || sessionId.isBlank()) return;

        String tokenHash = TokenUtil.sha256Hex(sessionId);
        loginLogRepository.findOpenByTokenHash(tokenHash).ifPresent(l -> l.markLogout(LocalDateTime.now()));
    }

    // 서버 시작 시 일괄 로그아웃 처리
    @Transactional
    public int forceLogoutAllActive(LocalDateTime now)
    { return loginLogRepository.forceLogoutAllActive(now); }

    // 로그인 기록 삭제
    @Transactional
    public int deleteLogsLoggedOutBefore(LocalDateTime cutoff)
    { return loginLogRepository.deleteByLogoutAtBeforeOrEqual(cutoff); }

    // 로그인 기록 조회
    @Transactional(readOnly = true)
    public Page<LoginLogDTO.Item> getLoginLogs(String targetUserId, int page, int size)
    {
        if (targetUserId == null || targetUserId.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        String viewerId = SecurityContextHelper.userIdOrNull();
        if (viewerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        boolean isAdmin = SecurityContextHelper.isAdmin();
        boolean owner = viewerId.equals(targetUserId);
        if (!owner && !isAdmin) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        // 탈퇴 계정이면 본인/관리자만 허용 (UserService와 동일 패턴) :contentReference[oaicite:6]{index=6}
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (target.isWithdraw() && !isAdmin && !owner)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0 || size > 50) ? 10 : size;

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<LoginLog> logs = loginLogRepository.findByUser_IdOrderByLoginAtDesc(targetUserId, pageable);

        List<LoginLogDTO.Item> mapped = logs.getContent().stream().map(l -> {
            String ip = (l.getLoginIp() == null) ? "" : l.getLoginIp();
            String region = (ip.isBlank()) ? "UNKNOWN" : geoIpService.resolveRegion(ip);

            return new LoginLogDTO.Item
            (l.getId(), ip, region, l.getLoginAt(), l.getLogoutAt());
        }).toList();

        return new PageImpl<>(mapped, pageable, logs.getTotalElements());
    }
}