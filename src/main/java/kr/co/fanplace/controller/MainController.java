package kr.co.fanplace.controller;

import kr.co.fanplace.dto.user.UserInfoDTO;
import kr.co.fanplace.service.MainService;
import kr.co.fanplace.service.board.PostService;
import kr.co.fanplace.service.user.UserService;
import kr.co.fanplace.setting.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController
{
    private final UserService userService;
    private final MainService mainService;
    private final PostService postService;

    // 메인
    @GetMapping("/")
    public String main(Model model)
    {
        model.addAttribute("siteStat", mainService.getSiteStat());
        model.addAttribute("recentNotices", postService.getRecentNotices());
        model.addAttribute("latestPosts", postService.getLatestPosts());

        if (SecurityContextHelper.userIdOrNull() != null)
        {
            UserInfoDTO.MainSummary me = userService.getMainUserSummary();
            model.addAttribute("mainMe", me);

            model.addAttribute("weekReport", userService.getActivityReport(7));
            model.addAttribute("moonReport", userService.getActivityReport(30));
        }

        return "main";
    }
}