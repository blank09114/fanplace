package kr.co.fanplace.setting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneId;

@RequiredArgsConstructor
public class PWChangedSessionFilter extends OncePerRequestFilter
{
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
    throws ServletException, IOException
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal()))
        {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null)
        {
            chain.doFilter(request, response);
            return;
        }

        String userId = auth.getName();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
        {
            safeLogout(session);
            chain.doFilter(request, response);
            return;
        }

        long userPwAt = (user.getPasswordChangedAt() == null) ? 0L
        : user.getPasswordChangedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Object snapObj = session.getAttribute(AuthSessionKeys.PW_CHANGED_AT);
        long sessionPwAt = (snapObj instanceof Number) ? ((Number) snapObj).longValue() : 0L;

        if (userPwAt > sessionPwAt) { safeLogout(session); }

        chain.doFilter(request, response);
    }

    private void safeLogout(HttpSession session)
    {
        try { session.invalidate(); } catch (IllegalStateException ignored) {}
        SecurityContextHolder.clearContext();
    }
}