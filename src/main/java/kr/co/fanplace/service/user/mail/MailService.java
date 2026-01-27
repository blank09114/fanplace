package kr.co.fanplace.service.user.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MailService
{
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String from;

    public void sendHtml(String to, String subject, String html)
    {
        try
        {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (from != null && !from.isBlank()) helper.setFrom(from);
            mailSender.send(msg);
        } catch (Exception e) { throw new RuntimeException("메일 발송 실패", e); }
    }
}