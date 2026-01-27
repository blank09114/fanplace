package kr.co.fanplace.setting.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.fanplace.setting.ip.IpUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IpCooldownInterceptor implements HandlerInterceptor
{
    private final ConcurrentHashMap<String, Long> lastHit = new ConcurrentHashMap<>();

    // 5초
    private static final long WINDOW_MS = 5_000L;

    // 레이트리밋
    private static final Set<String> TARGETS = Set.of(
        // 인증 관련 API
        "/api/auth/join/request",
        "/api/auth/join/resend",
        "/api/auth/login",
        "/api/auth/logout"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;

        String uri = request.getRequestURI();
        if (!TARGETS.contains(uri)) return true;

        // IP 기반 키
        String ip = IpUtil.resolveClientIp(request);
        String key = "ip:" + (ip == null ? "" : ip) + "|POST|" + uri;

        long now = System.currentTimeMillis();
        Long prev = lastHit.putIfAbsent(key, now);

        if (prev != null)
        {
            long delta = now - prev;
            if (delta < WINDOW_MS)
            {
                long retryAfterSec = (WINDOW_MS - delta + 999) / 1000;

                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(retryAfterSec));
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                response.getWriter().write("{\"ok\":false,\"message\":\"잠시 후 다시 시도해주세요.\"}");
                return false;
            }
            else
            {
                lastHit.put(key, now);
            }
        }

        if ((now & 0xFF) == 0) cleanupOld(now);

        return true;
    }

    private void cleanupOld(long now)
    {
        long threshold = now - (WINDOW_MS * 6);
        for (var e : lastHit.entrySet())
        { if (e.getValue() < threshold) lastHit.remove(e.getKey(), e.getValue()); }
    }
}