package kr.co.fanplace.setting.config;

import kr.co.fanplace.setting.security.IpCooldownInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer
{
    private final IpCooldownInterceptor ipCooldownInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(ipCooldownInterceptor).addPathPatterns(
            // 인증 관련 API
            "/api/auth/join/request",
            "/api/auth/join/resend",
            "/api/auth/login",
            "/api/auth/logout",

            // 글/댓글/대댓글 쓰기 쿨다운
            "/*/write",
            "/api/post/*/comment",
            "/api/post/comment/*/recomment"
        );
    }
}