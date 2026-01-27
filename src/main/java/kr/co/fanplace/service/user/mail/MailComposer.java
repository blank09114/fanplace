package kr.co.fanplace.service.user.mail;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MailComposer
{
    private final TemplateRenderer templateRenderer;

    // 인증 메일 발송
    public ComposedMail joinVerify(String to, String userName, String verifyUrl, long expiresMinutes)
    {
        String subject = "[FANPLACE] 이메일 인증을 완료해주세요";
        String html = templateRenderer.render(
            "templates/mail/verify.html",
            Map.of(
                "{{userName}}", safe(userName),
                "{{verifyUrl}}", safe(verifyUrl),
                "{{expiresMinutes}}", String.valueOf(expiresMinutes)
            )
        );
        return new ComposedMail(to, subject, html);
    }

    // 계정 찾기 메일 발송
    public ComposedMail resetFindAccount(String to, String userName, String userId, String tempPassword, String applyUrl, long expiresMinutes)
    {
        String subject = "[FANPLACE] 계정 복구 안내";
        String html = templateRenderer.render(
            "templates/mail/reset.html",
            Map.of(
                "{{userName}}", safe(userName),
                "{{userId}}", safe(userId),
                "{{tempPassword}}", safe(tempPassword),
                "{{applyUrl}}", safe(applyUrl),
                "{{expiresMinutes}}", String.valueOf(expiresMinutes)
            )
        );
        return new ComposedMail(to, subject, html);
    }

    private String safe(String s) { return s == null ? "" : s; }

    @Getter
    public static class ComposedMail
    {
        private final String to;
        private final String subject;
        private final String html;

        public ComposedMail(String to, String subject, String html) {
            this.to = to;
            this.subject = subject;
            this.html = html;
        }
    }
}