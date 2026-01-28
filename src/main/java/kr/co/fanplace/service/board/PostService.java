package kr.co.fanplace.service.board;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.fanplace.dto.board.PostDTO;
import kr.co.fanplace.entity.board.Board;
import kr.co.fanplace.entity.board.Category;
import kr.co.fanplace.entity.board.post.Post;
import kr.co.fanplace.entity.board.post.PostLog;
import kr.co.fanplace.entity.user.User;
import kr.co.fanplace.repository.board.BoardRepository;
import kr.co.fanplace.repository.board.CategoryRepository;
import kr.co.fanplace.repository.board.PostLogRepository;
import kr.co.fanplace.repository.board.PostRepository;
import kr.co.fanplace.repository.user.UserRepository;
import kr.co.fanplace.setting.ip.IpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostService
{
    private final BoardRepository boardRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostLogRepository postLogRepository;

    // 게시글 작성
    @Transactional
    public Long createPost(String boardId, PostDTO.CreateForm form)
    {
        LocalDateTime now = LocalDateTime.now();
        String userId = resolveUserIdOrNull();
        String clientIp = resolveClientIpOrFallback();

        Board board = boardRepository.findById(boardId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시판입니다."));

        Category category = categoryRepository.findById(form.getCategoryId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리입니다."));

        // 카테고리-게시판 정합성
        if (!category.getBoard().getId().equals(boardId))
        { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시판과 카테고리가 일치하지 않습니다."); }

        User user = null;
        if (userId != null)
        {
            user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 유효하지 않습니다."));
        }

        Post post = Post.create(board, category, user, clientIp, now);
        postRepository.save(post);

        PostLog log = PostLog.create(post, form.getTitle(), form.getContent(), now);
        postLogRepository.save(log);

        return post.getId();
    }

    // 사용자 정보 추출
    private String resolveUserIdOrNull()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if ("anonymousUser".equals(auth.getPrincipal())) return null;
        return auth.getName();
    }

    // IP 추출
    private String resolveClientIpOrFallback()
    {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "0.0.0.0";
        HttpServletRequest request = attrs.getRequest();
        return IpUtil.resolveClientIp(request);
    }
}