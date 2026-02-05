package kr.co.fanplace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController
{
    // 대시보드
    @GetMapping("/")
    public String dashboard() { return "admin/dashboard"; }

    // 회원목록
    @GetMapping("/user-list")
    public String userList() { return "admin/userList"; }

    // 삭제된 글
    @GetMapping("/deleted-post")
    public String deletedPost() { return "admin/deletedPost"; }

    // 삭제된 댓글
    @GetMapping("/deleted-comment")
    public String deletedComment() { return "admin/deletedComment"; }
}