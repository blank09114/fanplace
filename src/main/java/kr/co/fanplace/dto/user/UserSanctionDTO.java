package kr.co.fanplace.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class UserSanctionDTO
{
    // 생성 요청
    @Getter @Setter
    public static class CreateReq
    {
        @Min(0)
        @Max(30)
        private int sanctionLong;

        @NotBlank
        @Size(max = 100)
        private String reason;
    }

    // 생성 응답
    @Getter
    public static class CreateRes
    {
        private final long sanctionId;
        public CreateRes(long sanctionId) { this.sanctionId = sanctionId; }
    }

    // 제재 내역
    @Getter
    @AllArgsConstructor
    public static class LogItem
    {
        private final Long sanctionId;
        private final int sanctionLong;
        private final String reason;
        private final LocalDateTime sanctionedAt;
    }

    // 리스트
    @Getter
    @AllArgsConstructor
    public static class LogListRes { private final List<LogItem> items; }
}