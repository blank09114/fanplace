package kr.co.fanplace.service.user;

import kr.co.fanplace.dto.user.auth.AuthReqs;
import kr.co.fanplace.entity.user.Token;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.repository.user.TokenRepository;
import kr.co.fanplace.repository.user.UserRepository;
import kr.co.fanplace.service.user.mail.MailComposer;
import kr.co.fanplace.service.user.mail.MailService;
import kr.co.fanplace.setting.geoip.GeoIpService;
import kr.co.fanplace.setting.security.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

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

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.join-token-minutes:30}")
    private long joinTokenMinutes;

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
}