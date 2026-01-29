package kr.co.fanplace.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

public class AlarmDTO
{
    public enum Type { COMMENT, RECOMMENT }

    // 조회
    @Getter
    @AllArgsConstructor
    public static class Item
    {
        private final Long alarmId;
        private final Type type;

        private final boolean unread;
        private final LocalDateTime alarmAt;

        private final String actorName;
        private final String preview;
        private final String targetUrl;
    }

    // 푸시
    @Getter
    @AllArgsConstructor
    public static class UnreadCount { private final long unreadCount; }
}