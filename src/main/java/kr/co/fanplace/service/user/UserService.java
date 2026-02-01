package kr.co.fanplace.service.user;

import kr.co.fanplace.dto.user.UserInfoDTO;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.repository.user.UserRepository;
import kr.co.fanplace.setting.ip.GeoIpService;
import kr.co.fanplace.setting.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService
{
    private final UserRepository userRepository;
    private final UserSanctionService userSanctionService;
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
}