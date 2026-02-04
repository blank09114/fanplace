package kr.co.fanplace.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

public class MainDTO
{
    // 사이트 통계
    @Getter
    @AllArgsConstructor
    public static class SiteStat
    {
        private final LocalDate date;
        private final long newUserCount;
        private final long postCount;
        private final long commentCount;
        private final String favoriteBoardName;
    }
}
