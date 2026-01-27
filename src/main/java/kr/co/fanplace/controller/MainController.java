package kr.co.fanplace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController
{
    // 메인
    @GetMapping("/")
    public String main()
    {
        return "main";
    }
}