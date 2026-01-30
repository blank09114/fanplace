package kr.co.fanplace.controller.user;

import kr.co.fanplace.dto.user.UserInfoDTO;
import kr.co.fanplace.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController
{
    private final UserService userService;

    // 회원정보
    @GetMapping("/{userId}")
    public String userInfo(@PathVariable String userId, Model model)
    {
        UserInfoDTO.Card card = userService.getUserCard(userId);

        model.addAttribute("card", card);
        model.addAttribute("pageTitle", card.getName() + " 님의 회원정보");

        return "account/user/userInfo";
    }

    // 알람
    @GetMapping("/alarm")
    public String alarm() { return "account/user/alarm"; }
}