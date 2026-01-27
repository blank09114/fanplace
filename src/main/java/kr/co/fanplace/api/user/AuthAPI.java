package kr.co.fanplace.api.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kr.co.fanplace.dto.ApiOk;
import kr.co.fanplace.dto.user.AuthReqs;
import kr.co.fanplace.service.user.AuthService;
import kr.co.fanplace.setting.ip.IpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthAPI
{
    private final AuthService authService;

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiOk> login(@RequestBody @Valid AuthReqs.LoginRequest req, HttpServletRequest request)
    {
        authService.login(req, request);
        return ResponseEntity.ok(ApiOk.ok());
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiOk> logout(HttpServletRequest request)
    {
        authService.logout(request);
        return ResponseEntity.ok(ApiOk.ok());
    }

    // 내 정보
    @GetMapping("/me")
    public ResponseEntity<AuthReqs.MeResponse> me(Authentication authentication)
    { return ResponseEntity.ok(authService.me(authentication)); }

    // ID 중복 검사
    @PostMapping("/join/check-id")
    public ResponseEntity<AuthReqs.CheckIdResponse> checkId(@RequestBody @Valid AuthReqs.CheckIdRequest req)
    {
        boolean available = authService.isUserIdAvailable(req.getUserId());
        return ResponseEntity.ok(AuthReqs.CheckIdResponse.of(available));
    }

    // 회원가입 요청
    @PostMapping("/join/request")
    public ResponseEntity<ApiOk> requestJoin(@RequestBody @Valid AuthReqs.JoinRequest req, HttpServletRequest request)
    {
        String clientIp = IpUtil.resolveClientIp(request);
        authService.requestJoin(req, clientIp);
        return ResponseEntity.ok(ApiOk.ok());
    }

    // 인증 메일 재발급
    @PostMapping("/join/resend")
    public ResponseEntity<ApiOk> resendJoin(@RequestBody @Valid AuthReqs.ResendJoinRequest req, HttpServletRequest request)
    {
        String clientIp = IpUtil.resolveClientIp(request);
        authService.resendJoin(req.getUserMail(), clientIp);
        return ResponseEntity.ok(ApiOk.ok());
    }

    // 인증
    @GetMapping("/join/verify")
    public void verifyJoin(@RequestParam("token") String token, HttpServletResponse response) throws IOException
    {
        try
        {
            authService.verifyJoin(token);
            response.sendRedirect("/?joined=1");
        }
        catch (IllegalArgumentException e) { response.sendRedirect("/?joined=expired"); }
    }

    // 계정 찾기 요청
    @PostMapping("/reset/request")
    public ResponseEntity<ApiOk> requestReset(@RequestBody @Valid AuthReqs.ResetRequest req, HttpServletRequest request)
    {
        String clientIp = IpUtil.resolveClientIp(request);
        authService.requestReset(req, clientIp);
        return ResponseEntity.ok(ApiOk.ok());
    }

    // 임시 비밀번호 적용
    @GetMapping("/reset/apply")
    public void applyReset(@RequestParam("token") String token, HttpServletResponse response) throws IOException
    {
        try
        {
            authService.applyReset(token);
            response.sendRedirect("/?reset=done");
        }
        catch (IllegalArgumentException e) { response.sendRedirect("/?reset=expired"); }
    }

    // 비밀번호 변경
    @PostMapping("/pw/change")
    public ResponseEntity<ApiOk> changePw(
        @RequestBody @Valid AuthReqs.ChangePwRequest req, Authentication authentication,
        HttpServletRequest request
    )
    {
        authService.changePw(req, authentication, request);
        return ResponseEntity.ok(ApiOk.ok());
    }

    // 회원 탈퇴 요청
    @PostMapping("/withdraw/request")
    public ResponseEntity<ApiOk> requestWithdraw(
        @RequestBody @Valid AuthReqs.WithdrawRequest req,
        Authentication authentication, HttpServletRequest request
    )
    {
        authService.requestWithdraw(req, authentication, request);
        return ResponseEntity.ok(ApiOk.ok());
    }

    // 회원 탈퇴
    @GetMapping("/withdraw/apply")
    public void applyWithdraw(@RequestParam("token") String token, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        try
        {
            authService.applyWithdraw(token, request);
            response.sendRedirect("/?withdraw=done");
        }
        catch (IllegalArgumentException e) { response.sendRedirect("/?withdraw=expired"); }
    }
}