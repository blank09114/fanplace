package kr.co.fanplace.api.admin;

import jakarta.validation.Valid;
import kr.co.fanplace.dto.ApiOk;
import kr.co.fanplace.dto.user.UserSanctionDTO;
import kr.co.fanplace.service.user.UserSanctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAPI
{
    private final UserSanctionService userSanctionService;

    // 사용자 차단
    @PostMapping("/user/{userId}/sanction")
    public UserSanctionDTO.CreateRes sanction(@PathVariable String userId, @RequestBody @Valid UserSanctionDTO.CreateReq req)
    {
        Long id = userSanctionService.createSanctionLog(userId, req);
        return new UserSanctionDTO.CreateRes(id);
    }

    // 제재 내역 삭제
    @DeleteMapping("/user/{userId}/sanction/logs/{sanctionId}")
    public ResponseEntity<ApiOk> deleteSanctionLog(@PathVariable String userId, @PathVariable Long sanctionId)
    {
        userSanctionService.deleteSanctionLog(userId, sanctionId);
        return ResponseEntity.ok(ApiOk.ok());
    }
}