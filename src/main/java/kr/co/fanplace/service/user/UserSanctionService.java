package kr.co.fanplace.service.user;

import kr.co.fanplace.dto.user.UserSanctionDTO;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.entity.user.UserSanctionLog;
import kr.co.fanplace.repository.user.UserRepository;
import kr.co.fanplace.repository.user.UserSanctionLogRepository;
import kr.co.fanplace.setting.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSanctionService
{
    private final UserRepository userRepository;
    private final UserSanctionLogRepository userSanctionLogRepository;

    // 제재 내역 조회
    @Transactional(readOnly = true)
    public UserSanctionDTO.LogListRes getSanctionLogs(String targetUserId)
    {
        if (targetUserId == null || targetUserId.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대상 사용자 ID가 필요합니다.");

        List<UserSanctionLog> logs =
            userSanctionLogRepository.findByUser_IdOrderBySanctionedAtDescIdDesc(targetUserId);

        List<UserSanctionDTO.LogItem> items = logs.stream()
        .map(l -> new UserSanctionDTO.LogItem(l.getId(), l.getSanctionLong(), l.getReason(), l.getSanctionedAt())).toList();

        return new UserSanctionDTO.LogListRes(items);
    }

    // 차단 요청
    @Transactional
    public Long createSanctionLog(String targetUserId, UserSanctionDTO.CreateReq req)
    {
        if (!SecurityContextHelper.isAdmin())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 가능합니다.");

        if (targetUserId == null || targetUserId.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대상 사용자 ID가 필요합니다.");
        if (req == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 바디가 필요합니다.");

        int sanctionLong = req.getSanctionLong();
        if (!(sanctionLong == 0 || sanctionLong == 1 || sanctionLong == 7 || sanctionLong == 30))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "차단 기간은 0(영구), 1, 7, 30만 허용됩니다.");

        String reason = (req.getReason() == null) ? "" : req.getReason().trim();
        if (reason.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "차단 사유를 입력하세요.");
        if (reason.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "차단 사유는 100자 이내입니다.");

        User user = userRepository.findById(targetUserId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 사용자를 찾을 수 없습니다."));

        if (user.isWithdraw())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "탈퇴한 계정은 차단할 수 없습니다.");

        LocalDateTime now = LocalDateTime.now();
        UserSanctionLog log = UserSanctionLog.create(user, sanctionLong, reason, now);

        userSanctionLogRepository.save(log);
        return log.getId();
    }

    // 제재 내역 삭제
    @Transactional
    public void deleteSanctionLog(String targetUserId, Long sanctionId)
    {
        SecurityContextHelper.requireUserId();
        if (!SecurityContextHelper.isAdmin())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 가능합니다.");

        UserSanctionLog log = userSanctionLogRepository.findById(sanctionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "차단 기록이 존재하지 않습니다."));

        // url의 userId랑 로그 소유자가 다르면 잘못된 호출로 판단
        String ownerId = (log.getUser() == null ? null : log.getUser().getId());
        if (ownerId == null || !ownerId.equals(targetUserId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.");

        userSanctionLogRepository.delete(log);
    }

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