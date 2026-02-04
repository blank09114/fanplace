package kr.co.fanplace.service;

import kr.co.fanplace.dto.MainDTO;
import kr.co.fanplace.repository.BoardCountRow;
import kr.co.fanplace.repository.board.post.PostRepository;
import kr.co.fanplace.repository.board.post.comment.CommentRepository;
import kr.co.fanplace.repository.board.post.comment.RecommentRepository;
import kr.co.fanplace.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MainService
{
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;

    // 사이트 통계
    @Transactional(readOnly = true)
    public MainDTO.SiteStat getSiteStat()
    {
        // 기준: 어제 하루
        LocalDate targetDate = LocalDate.now().minusDays(1);

        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = targetDate.plusDays(1).atStartOfDay();

        // 신규 가입자 수
        long newUserCount = userRepository.countJoinedInRange(from, to);

        // 전체 글 수
        long postCount = postRepository.countPostInRange(from, to);

        // 댓글/대댓글
        long commentCount = commentRepository.countCommentInRange(from, to);
        long recommentCount = recommentRepository.countRecommentInRange(from, to);
        long totalCommentCount = commentCount + recommentCount;

        // 게시판별 점수 계산용 데이터
        List<BoardCountRow> postByBoard =
            postRepository.countPostByBoardInRange(from, to);

        List<BoardCountRow> commentByBoard =
            commentRepository.countCommentByBoardInRange(from, to);

        List<BoardCountRow> recommentByBoard =
            recommentRepository.countRecommentByBoardInRange(from, to);

        String favoriteBoardName =
            pickFavoriteBoardName(postByBoard, commentByBoard, recommentByBoard);

        return new MainDTO.SiteStat(targetDate, newUserCount, postCount, totalCommentCount, favoriteBoardName);
    }

    private static class Stat
    {
        String boardId;
        String boardName;
        long post;
        long comment;
        long recomment;

        long score() { return post * 2 + comment + recomment; }
        long total() { return post + comment + recomment; }
    }

    // 게시판별 점수 계산 최다 이용 게시판명 반환
    public String pickFavoriteBoardName(
        List<BoardCountRow> postRows,
        List<BoardCountRow> commentRows,
        List<BoardCountRow> recommentRows
    ) {
        Map<String, Stat> map = new HashMap<>();

        merge(map, postRows, "post");
        merge(map, commentRows, "comment");
        merge(map, recommentRows, "recomment");

        Stat best = null;
        for (Stat s : map.values())
        {
            if (s.score() <= 0) continue;

            if (best == null) { best = s; continue; }

            long a = s.score();
            long b = best.score();
            if (a != b) { if (a > b) best = s; continue; }

            long ta = s.total();
            long tb = best.total();
            if (ta != tb) { if (ta > tb) best = s; continue; }

            String an = (s.boardName == null) ? "" : s.boardName;
            String bn = (best.boardName == null) ? "" : best.boardName;
            if (an.compareTo(bn) < 0) best = s;
        }

        return (best == null || best.boardName == null || best.boardName.isBlank())
                ? "없음"
                : best.boardName;
    }

    private void merge(Map<String, Stat> map, List<BoardCountRow> rows, String type)
    {
        if (rows == null) return;

        for (BoardCountRow r : rows)
        {
            if (r == null) continue;
            String boardId = r.getBoardId();
            if (boardId == null || boardId.isBlank()) continue;

            Stat s = map.computeIfAbsent(boardId, k ->
            {
                Stat ns = new Stat();
                ns.boardId = boardId;
                ns.boardName = r.getBoardName();
                return ns;
            });

            // boardName 보강
            if ((s.boardName == null || s.boardName.isBlank()) && r.getBoardName() != null)
                s.boardName = r.getBoardName();

            long cnt = r.getCnt();
            if ("post".equals(type)) s.post += cnt;
            else if ("comment".equals(type)) s.comment += cnt;
            else if ("recomment".equals(type)) s.recomment += cnt;
        }
    }
}