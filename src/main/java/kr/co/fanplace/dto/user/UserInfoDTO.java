package kr.co.fanplace.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

public class UserInfoDTO
{
    // 회원정보
    @Getter
    @AllArgsConstructor
    public static class Card
    {
        private final String userId;
        private final String name;
        private final LocalDateTime joinedAt;

        // 아래 2개는 본인/관리자만
        private final String mail;
        private final String joinIp;

        // 계정 상태
        private final boolean withdraw;

        // 제재 상태
        private final boolean blocked;
    }

    // 닉네임 변경 요청
    public static class ChangeNameReq
    {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // 메인 페이지 회원 정보 위젯
    @Getter
    @AllArgsConstructor
    public static class MainSummary
    {
        private final String userId;
        private final String name;

        private final long postCount;
        private final long commentCount;

        private final boolean blocked;
    }

    // 활동 리포트
    @Getter
    @AllArgsConstructor
    public static class ActivityReport
    {
        private final int days;
        private final long postCount;
        private final long commentCount;
        private final String favoriteBoardName;
    }
}