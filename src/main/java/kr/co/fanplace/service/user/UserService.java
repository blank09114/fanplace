package kr.co.fanplace.service.user;

import kr.co.fanplace.dto.user.UserInfoDTO;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.repository.BoardCountRow;
import kr.co.fanplace.repository.board.post.PostRepository;
import kr.co.fanplace.repository.board.post.comment.CommentRepository;
import kr.co.fanplace.repository.board.post.comment.RecommentRepository;
import kr.co.fanplace.repository.user.UserRepository;
import kr.co.fanplace.service.MainService;
import kr.co.fanplace.setting.ip.GeoIpService;
import kr.co.fanplace.setting.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService
{
    private final UserRepository userRepository;
    private final UserSanctionService userSanctionService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;
    private final MainService mainService;
    private final GeoIpService geoIpService;

    // 회원정보 카드
    @Transactional(readOnly = true)
    public UserInfoDTO.Card getUserCard(String targetUserId)
    {
        if (targetUserId == null || targetUserId.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        User target = userRepository.findById(targetUserId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean isAdmin = SecurityContextHelper.isAdmin();
        String viewerId = SecurityContextHelper.userIdOrNull();
        boolean owner = viewerId != null && viewerId.equals(target.getId());
        boolean canSeePrivate = owner || isAdmin;

        if (target.isWithdraw() && !canSeePrivate)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        boolean blocked = userSanctionService.isBlocked(target.getId());

        String joinIp = null;
        if (canSeePrivate && target.getIp() != null)
        {
            String region = geoIpService.resolveRegion(target.getIp());
            joinIp = (region != null && !region.isBlank())
            ? target.getIp() + " (" + region + ")" : target.getIp();
        }

        String mail = canSeePrivate ? target.getMail() : null;

        return new UserInfoDTO.Card(
            target.getId(), target.getName(), target.getCreatedAt(),
            mail, joinIp, target.isWithdraw(), blocked
        );
    }

    // 닉네임 변경
    @Transactional
    public void changeName(String targetUserId, String newNameRaw)
    {
        if (targetUserId == null || targetUserId.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        String viewerId = SecurityContextHelper.userIdOrNull();
        if (viewerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        boolean isAdmin = SecurityContextHelper.isAdmin();
        boolean owner = viewerId.equals(targetUserId);
        if (!owner && !isAdmin) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        String newName = (newNameRaw == null) ? "" : newNameRaw.trim();
        if (newName.length() < 2 || newName.length() > 10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        User target = userRepository.findById(targetUserId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (target.isWithdraw()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        if (newName.equals(target.getName())) return;

        target.changeName(newName);
    }

    // 회원 정보 위젯
    @Transactional(readOnly = true)
    public UserInfoDTO.MainSummary getMainUserSummary()
    {
        String viewerId = SecurityContextHelper.userIdOrNull();
        if (viewerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        User me = userRepository.findById(viewerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (me.isWithdraw()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        boolean blocked = userSanctionService.isBlocked(me.getId());

        long postCount = postRepository.countByUser_IdAndDeletedFalse(me.getId());
        long commentCount =
            commentRepository.countByUser_IdAndDeletedFalse(me.getId())
            + recommentRepository.countByAuthorUser_IdAndDeletedFalse(me.getId());

        return new UserInfoDTO.MainSummary
        (me.getId(), me.getName(), postCount, commentCount, blocked);
    }

    // 활동 리포트
    @Transactional(readOnly = true)
    public UserInfoDTO.ActivityReport getActivityReport(int days)
    {
        if (days <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        String userId = SecurityContextHelper.userIdOrNull();
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        // 유저 존재/탈퇴 방어는 기존 패턴 유지
        User me = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (me.isWithdraw()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(days - 1L).atStartOfDay();
        LocalDateTime to = today.plusDays(1L).atStartOfDay();

        long postCount = postRepository.countUserPostInRange(userId, from, to);
        long commentCount = commentRepository.countUserCommentInRange(userId, from, to);
        long recommentCount = recommentRepository.countUserRecommentInRange(userId, from, to);

        // 게시판별 점수 계산용 데이터
        List<BoardCountRow> postByBoard =
            postRepository.countUserPostByBoardInRange(userId, from, to);
        List<kr.co.fanplace.repository.BoardCountRow> commentByBoard =
            commentRepository.countUserCommentByBoardInRange(userId, from, to);
        List<kr.co.fanplace.repository.BoardCountRow> recommentByBoard =
            recommentRepository.countUserRecommentByBoardInRange(userId, from, to);

        String favoriteBoardName =
            mainService.pickFavoriteBoardName(postByBoard, commentByBoard, recommentByBoard);

        long totalCommentCount = commentCount + recommentCount;

        return new UserInfoDTO.ActivityReport(days, postCount, totalCommentCount, favoriteBoardName);
    }

    // 회원목록
    @Transactional(readOnly = true)
    public Page<UserInfoDTO.Card> getUserListPageAdmin(int page, int size)
    {
        if (!SecurityContextHelper.isAdmin())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다.");

        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size)); // 방어

        var pageable = PageRequest.of
        (safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        return userRepository.findAll(pageable).map(u ->
        {
            boolean withdraw = u.isWithdraw();
            boolean blocked = false;

            if (!withdraw) blocked = userSanctionService.isBlocked(u.getId());

            return new UserInfoDTO.Card(
                u.getId(), u.getName(), u.getCreatedAt(), u.getMail(), null,
                u.isWithdraw(), blocked
            );
        });
    }
}