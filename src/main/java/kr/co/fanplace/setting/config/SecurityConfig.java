package kr.co.fanplace.setting.config;

import kr.co.fanplace.repository.user.UserRepository;
import kr.co.fanplace.setting.security.PWChangedSessionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig
{
    private final UserRepository userRepository;

    // 비밀번호 암호화
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    // 접근 제어
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        http.csrf(csrf -> csrf.disable())
        .formLogin(f -> f.disable())
        .httpBasic(b -> b.disable())
        .authorizeHttpRequests(auth -> auth
            // 비로그인 사용자만 허용
            .requestMatchers("/login", "/join", "/find-account").anonymous()
            .requestMatchers("/api/auth/login", "/api/auth/join/**", "/api/auth/reset/**").anonymous()

            // 로그인 사용자만 허용
            .requestMatchers("/change-pw", "/withdraw").authenticated()
            .requestMatchers("/api/auth/pw/**", "/api/auth/withdraw/**").authenticated()

            // 게시판 관련
            .requestMatchers("/**/write", "/**/post/*/delete", "/**/post/*/deleted-reason").authenticated()
            .requestMatchers(
                "/api/post/*/comment", "/api/post/comment/*/recomment",
                "/api/post/comment/*/delete", "/api/post/recomment/*/delete"
            ).authenticated()

            // 나머지는 전부 허용
            .anyRequest().permitAll()
        );
        http.addFilterBefore(new PWChangedSessionFilter(userRepository),
        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}