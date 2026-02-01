package kr.co.fanplace.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class LoginLogDTO
{
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item
    {
        private Long id;
        private String ip;
        private String region;
        private LocalDateTime loginAt;
        private LocalDateTime logoutAt;
    }
}