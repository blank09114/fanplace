package kr.co.fanplace.api.user;

import kr.co.fanplace.dto.ApiOk;
import kr.co.fanplace.dto.user.AlarmDTO;
import kr.co.fanplace.dto.user.LoginLogDTO;
import kr.co.fanplace.dto.user.UserInfoDTO;
import kr.co.fanplace.dto.user.UserSanctionDTO;
import kr.co.fanplace.service.user.AlarmService;
import kr.co.fanplace.service.user.LoginLogService;
import kr.co.fanplace.service.user.UserSanctionService;
import kr.co.fanplace.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserAPI
{
    private final AlarmService alarmService;
    private final UserService userService;
    private final UserSanctionService userSanctionService;
    private final LoginLogService loginLogService;

    // 알람 목록 조회
    @GetMapping("/alarm")
    public Page<AlarmDTO.Item> getAlarms
    (@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "false") boolean unreadOnly)
    { return alarmService.getAlarmPage(page, unreadOnly); }

    // 알람 읽음 처리
    @PostMapping("/alarm/{alarmId}/click")
    public String clickAlarm(@PathVariable Long alarmId) { return alarmService.clickAndGetTargetUrl(alarmId); }

    // 안 읽은 알람 갯수
    @GetMapping("/alarm/unread-count")
    public long getAlarmUnreadCount() { return alarmService.getUnreadCount(); }

    // 회원정보 카드
    @GetMapping("/{userId}/card")
    public UserInfoDTO.Card getUserCard(@PathVariable String userId)
    { return userService.getUserCard(userId); }

    // 닉네임 변경
    @PatchMapping("/{userId}/name")
    public ResponseEntity<ApiOk> changeName(@PathVariable String userId, @RequestBody UserInfoDTO.ChangeNameReq req)
    {
        userService.changeName(userId, req == null ? null : req.getName());
        return ResponseEntity.ok(ApiOk.ok());
    }

    // 제재 내역 조회
    @GetMapping("/{userId}/sanction/logs")
    public UserSanctionDTO.LogListRes sanctionLogs(@PathVariable String userId)
    { return userSanctionService.getSanctionLogs(userId); }

    // 로그인 기록 조회
    @GetMapping("/{userId}/login/logs")
    public Page<LoginLogDTO.Item> loginLogs(
        @PathVariable String userId, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) { return loginLogService.getLoginLogs(userId, page, size); }
}