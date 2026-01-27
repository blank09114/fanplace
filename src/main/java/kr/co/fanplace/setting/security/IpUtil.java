package kr.co.fanplace.setting.security;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtil {
    private IpUtil() {}

    public static String resolveClientIp(HttpServletRequest request)
    {
        String ip = firstIp(request.getHeader("X-Forwarded-For"));
        if (ip != null) return ip;

        ip = firstIp(request.getHeader("X-Real-IP"));
        if (ip != null) return ip;

        ip = firstIp(request.getHeader("CF-Connecting-IP"));
        if (ip != null) return ip;

        return request.getRemoteAddr();
    }

    private static String firstIp(String headerValue)
    {
        if (headerValue == null || headerValue.isBlank()) return null;
        // XFF: "client, proxy1, proxy2"
        String first = headerValue.split(",")[0].trim();
        return first.isEmpty() ? null : first;
    }
}
