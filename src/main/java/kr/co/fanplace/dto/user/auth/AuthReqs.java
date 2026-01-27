package kr.co.fanplace.dto.user.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class AuthReqs
{
    // 회원가입 요청
    @Getter @Setter
    public static class JoinRequest
    {
        @NotBlank
        @Size(max = 20)
        private String userId;

        @NotBlank
        @Size(max = 10)
        private String userName;

        @NotBlank
        @Size(max = 255)
        private String userPw;

        @NotBlank
        @Email
        @Size(max = 255)
        private String userMail;
    }

    // ID 중복 검사
    @Getter @Setter
    public static class CheckIdRequest
    {
        @NotBlank
        @Size(max = 20)
        private String userId;
    }

    // ID 중복 검사 응답
    @Getter @Setter
    public static class CheckIdResponse
    {
        private boolean available;

        public static CheckIdResponse of(boolean available)
        {
            CheckIdResponse res = new CheckIdResponse();
            res.setAvailable(available);
            return res;
        }
    }

    // 인증 메일 재발급 요청
    @Getter @Setter
    public static class ResendJoinRequest
    {
        @NotBlank
        @Email
        @Size(max = 255)
        private String userMail;
    }

    // 인증 처리
    @Getter @Setter
    public static class VerifyJoinRequest
    {
        @NotBlank
        private String token;
    }
}