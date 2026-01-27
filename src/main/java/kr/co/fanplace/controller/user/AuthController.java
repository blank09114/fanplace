package kr.co.fanplace.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController
{
    // 로그인
    @GetMapping("/login")
    public String login() { return "account/auth/login"; }

    // 회원가입
    @GetMapping("/join")
    public String join()
    {
        return "account/auth/join";
    }

    // 계정 찾기
    @GetMapping("/find-account")
    public String findAccount() { return "account/auth/findAccount"; }

    // 비밀번호 변경
    @GetMapping("/change-pw")
    public String changePw() { return "account/auth/changePw"; }

    // 회원 탈퇴
    @GetMapping("/withdraw")
    public String withdraw()
    {
        return "account/auth/withdraw";
    }
}