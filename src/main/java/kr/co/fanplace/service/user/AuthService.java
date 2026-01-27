package kr.co.fanplace.service.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.fanplace.dto.user.AuthReqs;
import kr.co.fanplace.entity.user.Token;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.repository.user.TokenRepository;
import kr.co.fanplace.repository.user.UserRepository;
import kr.co.fanplace.service.user.mail.MailComposer;
import kr.co.fanplace.service.user.mail.MailService;
import kr.co.fanplace.setting.geoip.GeoIpService;
import kr.co.fanplace.setting.security.AuthSessionKeys;
import kr.co.fanplace.setting.security.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService
{
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    private final MailComposer mailComposer;
    private final MailService mailService;
    private final GeoIpService geoIpService;
    private final LoginLogService loginLogService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.join-token-minutes:30}")
    private long joinTokenMinutes;

    @Value("${app.reset-token-minutes:30}")
    private long resetTokenMinutes;

    @Value("${app.withdraw-token-minutes:30}")
    private long withdrawTokenMinutes;

    // 로그인
    @Transactional
    public void login(AuthReqs.LoginRequest req, HttpServletRequest request)
    {
        String userId = req.getUserId() == null ? "" : req.getUserId().trim();
        String userPw = req.getUserPw() == null ? "" : req.getUserPw().trim();

        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        // 미인증 계정 로그인 불가
        if (!user.isEnabled()) throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");

        // 비밀번호 검증
        if (!passwordEncoder.matches(userPw, user.getPassword())) throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");

        // 탈퇴 계정이면 로그인 시 탈퇴 취소
        if (user.isWithdraw()) { user.cancelWithdraw(); }

        // 권한
        String role = "ROLE_" + user.getRole().name(); // USER/ADMIN
        var auth = new UsernamePasswordAuthenticationToken(
            user.getId(), null, List.of(new SimpleGrantedAuthority(role))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        HttpSession session = request.getSession(true);
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext()
        );

        // 비번 변경 시각 스냅샷 저장
        long pwAt = (user.getPasswordChangedAt() == null) ? 0L
        : user.getPasswordChangedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        session.setAttribute(AuthSessionKeys.PW_CHANGED_AT, pwAt);

        // 로그인 기록
        loginLogService.recordLogin(user, session, request);
    }

    // 로그아웃
    @Transactional
    public void logout(HttpServletRequest request)
    {
        HttpSession session = request.getSession(false);
        String sessionId = (session != null) ? session.getId() : null;

        try { if (sessionId != null) { loginLogService.markLogoutBySessionId(sessionId); } }
        finally
        {
            if (session != null)
            {
                try { session.invalidate(); }
                catch (IllegalStateException ignored) { }
            }
            SecurityContextHolder.clearContext();
        }
    }

    // 로그인 정보
    @Transactional(readOnly = true)
    public AuthReqs.MeResponse me(org.springframework.security.core.Authentication authentication)
    {
        if (authentication == null) return AuthReqs.MeResponse.empty();
        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) return AuthReqs.MeResponse.empty();

        String userId = authentication.getName();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return AuthReqs.MeResponse.empty();

        return AuthReqs.MeResponse.of(user.getId(), user.getName());
    }

    // ID 중복 검사
    @Transactional(readOnly = true)
    public boolean isUserIdAvailable(String userId)
    {
        String id = trim(userId);
        if (id.isEmpty()) return false;
        return !userRepository.existsById(id);
    }

    // 회원가입 요청
    @Transactional
    public void requestJoin(AuthReqs.JoinRequest req, String clientIp)
    {
        String userId = trim(req.getUserId());
        String userName = trim(req.getUserName());
        String userMail = lower(trim(req.getUserMail()));
        String userPw = req.getUserPw() == null ? "" : req.getUserPw().trim();

        validateJoinInput(userId, userName, userMail, userPw);
        validateNaverOnly(userMail);

        if (userRepository.existsById(userId)) throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        if (userRepository.existsByMail(userMail)) throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");

        geoIpService.resolveRegion(clientIp);

        User user = User.builder()
        .id(userId).name(userName).mail(userMail).password(passwordEncoder.encode(userPw))
        .ip(clientIp == null ? "" : clientIp).build();

        userRepository.save(user);

        issueJoinTokenAndSendMail(user);
    }

    // 인증 메일 재발급
    @Transactional
    public void resendJoin(String userMailRaw, String clientIp)
    {
        String userMail = lower(trim(userMailRaw));
        validateNaverOnly(userMail);

        User user = userRepository.findByMail(userMail)
        .orElseThrow(() -> new IllegalArgumentException("가입된 이메일이 아닙니다."));

        if (user.isEnabled()) throw new IllegalArgumentException("이미 인증이 완료된 계정입니다.");

        geoIpService.resolveRegion(clientIp);

        // 기존 JOIN 토큰 즉시 만료
        tokenRepository.expireActiveJoinTokens(user.getId(), LocalDateTime.now());

        issueJoinTokenAndSendMail(user);
    }

    // 인증 처리
    @Transactional
    public void verifyJoin(String rawToken)
    {
        if (rawToken == null || rawToken.isBlank()) throw new IllegalArgumentException("토큰이 비어있습니다.");

        String hash = TokenUtil.sha256Hex(rawToken);
        Token token = tokenRepository.findJoinByHash(hash)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        LocalDateTime now = LocalDateTime.now();
        if (token.getUsedAt() != null) throw new IllegalArgumentException("이미 사용된 토큰입니다.");
        if (!token.getExpiresAt().isAfter(now)) throw new IllegalArgumentException("만료된 토큰입니다.");

        token.markUsed(now);
        token.getUser().enable();
    }

    // 계정 복구 요청
    @Transactional
    public void requestReset(AuthReqs.ResetRequest req, String clientIp)
    {
        String userMail = lower(trim(req.getUserMail()));
        validateNaverOnly(userMail);

        User user = userRepository.findByMail(userMail)
        .orElseThrow(() -> new IllegalArgumentException("가입된 이메일이 아닙니다."));

        geoIpService.resolveRegion(clientIp);
        tokenRepository.expireActiveResetTokens(user.getId(), LocalDateTime.now());

        // 임시 비밀번호 생성
        String tempPw = TokenUtil.generateToken(16);
        String tempPwHash = passwordEncoder.encode(tempPw);

        // RESET 토큰 발급
        String rawToken = TokenUtil.generateToken(48);
        String tokenHash = TokenUtil.sha256Hex(rawToken);

        Token token = Token.builder()
        .user(user).type(Token.TokenType.RESET).hash(tokenHash).pwHash(tempPwHash)
        .expiresAt(LocalDateTime.now().plusMinutes(resetTokenMinutes)).usedAt(null).build();

        tokenRepository.save(token);

        String encoded = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String applyUrl = baseUrl + "/api/auth/reset/apply?token=" + encoded;

        var mail = mailComposer.resetFindAccount(
            user.getMail(), user.getName(), user.getId(),
            tempPw, applyUrl, resetTokenMinutes
        );

        mailService.sendHtml(mail.getTo(), mail.getSubject(), mail.getHtml());
    }

    // 임시 비밀번호 적용
    @Transactional
    public void applyReset(String rawToken)
    {
        if (rawToken == null || rawToken.isBlank()) throw new IllegalArgumentException("토큰이 비어있습니다.");

        String hash = TokenUtil.sha256Hex(rawToken);

        Token token = tokenRepository.findResetByHash(hash)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        LocalDateTime now = LocalDateTime.now();

        if (token.isUsed()) throw new IllegalArgumentException("이미 사용된 토큰입니다.");
        if (token.isExpired(now)) throw new IllegalArgumentException("만료된 토큰입니다.");

        String pwHash = token.getPwHash();
        if (pwHash == null || pwHash.isBlank()) throw new IllegalArgumentException("임시 비밀번호 정보가 없습니다.");

        LocalDateTime changedAt = changePassword(token.getUser(), pwHash);
        token.markUsed(changedAt);
    }

    // 비밀번호 변경
    @Transactional
    public void changePw(AuthReqs.ChangePwRequest req, org.springframework.security.core.Authentication authentication, HttpServletRequest request)
    {
        if (authentication == null || authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal()))
            throw new IllegalArgumentException("로그인이 필요합니다.");

        String userId = authentication.getName();
        User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        String oldPw = req.getOldPw() == null ? "" : req.getOldPw().trim();
        String newPw = req.getNewPw() == null ? "" : req.getNewPw().trim();

        if (oldPw.isEmpty()) throw new IllegalArgumentException("기존 비밀번호를 입력해주세요.");
        if (newPw.isEmpty()) throw new IllegalArgumentException("새 비밀번호를 입력해주세요.");
        if (newPw.length() > 255) throw new IllegalArgumentException("비밀번호가 너무 깁니다.");
        if (!passwordEncoder.matches(oldPw, user.getPassword()))
            throw new IllegalArgumentException("기존 비밀번호가 올바르지 않습니다.");
        if (passwordEncoder.matches(newPw, user.getPassword()))
            throw new IllegalArgumentException("새 비밀번호가 기존 비밀번호와 같습니다.");

        String encoded = passwordEncoder.encode(newPw);
        changePassword(user, encoded);

        logout(request);
    }

    // 회원 탈퇴 요청
    @Transactional
    public void requestWithdraw(AuthReqs.WithdrawRequest req, org.springframework.security.core.Authentication authentication, HttpServletRequest request)
    {
        if (authentication == null || authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal()))
            throw new IllegalArgumentException("로그인이 필요합니다.");

        String userId = authentication.getName();
        User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        String pw = req.getPw() == null ? "" : req.getPw().trim();
        if (pw.isEmpty()) throw new IllegalArgumentException("비밀번호를 입력해주세요.");

        if (!passwordEncoder.matches(pw, user.getPassword()))
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");

        tokenRepository.expireActiveWithdrawTokens(user.getId(), LocalDateTime.now());

        String rawToken = TokenUtil.generateToken(48);
        String tokenHash = TokenUtil.sha256Hex(rawToken);

        Token token = Token.builder()
        .user(user).type(Token.TokenType.WITHDRAW).hash(tokenHash)
        .expiresAt(LocalDateTime.now().plusMinutes(withdrawTokenMinutes)).usedAt(null).build();

        tokenRepository.save(token);

        String encoded = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String withdrawUrl = baseUrl + "/api/auth/withdraw/apply?token=" + encoded;

        var mail = mailComposer.withdrawConfirm(user.getMail(), user.getName(), withdrawUrl, withdrawTokenMinutes);
        mailService.sendHtml(mail.getTo(), mail.getSubject(), mail.getHtml());
    }

    // 회원 탈퇴
    @Transactional
    public void applyWithdraw(String rawToken, HttpServletRequest request)
    {
        if (rawToken == null || rawToken.isBlank()) throw new IllegalArgumentException("토큰이 비어있습니다.");

        String hash = TokenUtil.sha256Hex(rawToken);

        Token token = tokenRepository.findWithdrawByHash(hash)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        LocalDateTime now = LocalDateTime.now();
        if (token.isUsed()) throw new IllegalArgumentException("이미 사용된 토큰입니다.");
        if (token.isExpired(now)) throw new IllegalArgumentException("만료된 토큰입니다.");

        // 탈퇴 적용
        token.getUser().markWithdraw(now);
        token.markUsed(now);

        // 현재 로그인 세션이 있다면 정리(선택)
        if (request != null) logout(request);
    }

    // 메일
    private void issueJoinTokenAndSendMail(User user)
    {
        String rawToken = TokenUtil.generateToken(48);
        String tokenHash = TokenUtil.sha256Hex(rawToken);

        Token token = Token.builder().user(user).type(Token.TokenType.JOIN).hash(tokenHash)
        .expiresAt(LocalDateTime.now().plusMinutes(joinTokenMinutes)).usedAt(null).build();

        tokenRepository.save(token);

        String encoded = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String verifyUrl = baseUrl + "/api/auth/join/verify?token=" + encoded;

        var mail = mailComposer.joinVerify(user.getMail(), user.getName(), verifyUrl, joinTokenMinutes);
        mailService.sendHtml(mail.getTo(), mail.getSubject(), mail.getHtml());
    }

    // 유효성 검사
    private void validateJoinInput(String id, String name, String mail, String pw)
    {
        if (id.isEmpty()) throw new IllegalArgumentException("아이디를 입력해주세요.");
        if (name.isEmpty()) throw new IllegalArgumentException("닉네임을 입력해주세요.");
        if (mail.isEmpty()) throw new IllegalArgumentException("이메일을 입력해주세요.");
        if (pw.isEmpty()) throw new IllegalArgumentException("비밀번호를 입력해주세요.");

        if (id.length() > 20) throw new IllegalArgumentException("아이디는 20자 이하입니다.");
        if (name.length() > 10) throw new IllegalArgumentException("닉네임은 10자 이하입니다.");
        if (mail.length() > 255) throw new IllegalArgumentException("이메일이 너무 깁니다.");
        if (pw.length() > 255) throw new IllegalArgumentException("비밀번호가 너무 깁니다.");
    }

    // 메일 형식 검사
    private void validateNaverOnly(String mail)
    { if (!mail.endsWith("@naver.com")) throw new IllegalArgumentException("네이버 이메일(@naver.com)만 사용할 수 있습니다."); }

    private String trim(String s) { return s == null ? "" : s.trim(); }
    private String lower(String s) { return s == null ? "" : s.toLowerCase(); }

    // 비밀번호 변경 메서드
    private LocalDateTime changePassword(User user, String newEncodedPw)
    {
        LocalDateTime now = LocalDateTime.now();
        user.changePassword(newEncodedPw, now);
        return now;
    }
}