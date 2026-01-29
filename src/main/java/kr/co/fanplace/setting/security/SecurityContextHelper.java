package kr.co.fanplace.setting.security;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.fanplace.setting.ip.IpUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

public final class SecurityContextHelper
{
    private SecurityContextHelper() {}

    // 로그인 여부
    public static boolean isLogin()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    // 로그인 유저 ID — 조회용
    public static String userIdOrNull()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if ("anonymousUser".equals(auth.getPrincipal())) return null;
        return auth.getName();
    }

    // 로그인 유저 ID — 쓰기/삭제용
    public static String requireUserId()
    {
        String userId = userIdOrNull();
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 필요");
        return userId;
    }

    // 관리자 여부
    public static boolean isAdmin()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if ("anonymousUser".equals(auth.getPrincipal())) return false;

        return auth.getAuthorities().stream().anyMatch
        (a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()));
    }

    // 현재 요청의 클라이언트 IP
    public static String clientIp()
    {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs == null) return "0.0.0.0";

        HttpServletRequest request = attrs.getRequest();
        return IpUtil.resolveClientIp(request);
    }
}