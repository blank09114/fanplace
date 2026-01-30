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
}