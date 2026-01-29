package kr.co.fanplace.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController
{
    // 회원정보
    @GetMapping("/{userId}")
    public String userInfo() { return "account/user/userInfo"; }

    // 알람
    @GetMapping("/alarm")
    public String alarm() { return "account/user/alarm"; }
}