package kr.co.fanplace.service.admin;

import kr.co.fanplace.dto.AdminDTO;
import kr.co.fanplace.repository.BoardCountRow;
import kr.co.fanplace.repository.DayCountRow;
import kr.co.fanplace.repository.UserScoreRow;
import kr.co.fanplace.repository.board.post.PostRepository;
import kr.co.fanplace.repository.board.post.comment.CommentRepository;
import kr.co.fanplace.repository.board.post.comment.RecommentRepository;
import kr.co.fanplace.setting.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService
{
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;

    // 공통
    private void assertAdmin()
    {
        if (!SecurityContextHelper.isAdmin())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다.");
    }

    private static class Range
    {
        final LocalDate weekStart; // monday
        final LocalDateTime from;
        final LocalDateTime to; // exclusive

        Range(LocalDate weekStart)
        {
            this.weekStart = weekStart;
            this.from = weekStart.atStartOfDay();
            this.to = weekStart.plusDays(7).atStartOfDay();
        }
    }

    private Range weekRange(LocalDate weekStart)
    {
        if (weekStart == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return new Range(weekStart);
    }

    private static double diffPct(long curr, long prev)
    {
        if (prev <= 0)
        {
            if (curr <= 0) return 0.0;
            return 100.0; // 전주 0 → 이번주 양수는 +100%로 표시(대시보드용)
        }
        return ((double)(curr - prev) * 100.0) / (double)prev;
    }

    private static double safeDiv(long a, long b)
    {
        if (b <= 0) return 0.0;
        return (double)a / (double)b;
    }

    private Map<LocalDate, Long> toDayMap(List<DayCountRow> rows)
    {
        Map<LocalDate, Long> map = new HashMap<>();
        if (rows == null) return map;

        for (DayCountRow r : rows)
        {
            if (r == null) continue;
            LocalDate day = r.getDay();
            if (day == null) continue;
            map.put(day, r.getCnt());
        }
        return map;
    }

    // 활동 추이
    @Transactional(readOnly = true)
    public AdminDTO.DashboardActivityRes getDashboardActivity(LocalDate weekStart)
    {
        assertAdmin();

        Range cur = weekRange(weekStart);
        Range prev = weekRange(weekStart.minusDays(7));

        // 주간 합계
        long post = postRepository.countPostInRange(cur.from, cur.to);
        long comment = commentRepository.countCommentInRange(cur.from, cur.to)
                + recommentRepository.countRecommentInRange(cur.from, cur.to);

        long user = commentRepository.countActiveUserInRange(cur.from, cur.to);

        AdminDTO.WeekSum sum = new AdminDTO.WeekSum(post, comment, user);

        // 전주 대비(%)
        long prevPost = postRepository.countPostInRange(prev.from, prev.to);
        long prevComment = commentRepository.countCommentInRange(prev.from, prev.to)
                + recommentRepository.countRecommentInRange(prev.from, prev.to);
        long prevUser = commentRepository.countActiveUserInRange(prev.from, prev.to);

        AdminDTO.WeekDiff diff = new AdminDTO.WeekDiff(
                diffPct(post, prevPost),
                diffPct(comment, prevComment),
                diffPct(user, prevUser)
        );

        // 일별 7줄 (월~일) - 없는 날은 0으로 채움
        Map<LocalDate, Long> postByDay = toDayMap(postRepository.countPostByDayInRange(cur.from, cur.to));
        Map<LocalDate, Long> cByDay = toDayMap(commentRepository.countCommentByDayInRange(cur.from, cur.to));
        Map<LocalDate, Long> rByDay = toDayMap(recommentRepository.countRecommentByDayInRange(cur.from, cur.to));
        Map<LocalDate, Long> uByDay = toDayMap(commentRepository.countActiveUserByDayInRange(cur.from, cur.to));

        List<AdminDTO.DayRow> days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++)
        {
            LocalDate d = cur.weekStart.plusDays(i);

            long dp = postByDay.getOrDefault(d, 0L);
            long dc = cByDay.getOrDefault(d, 0L) + rByDay.getOrDefault(d, 0L);
            long du = uByDay.getOrDefault(d, 0L);

            days.add(new AdminDTO.DayRow(d, dp, dc, du));
        }

        return new AdminDTO.DashboardActivityRes(sum, diff, days);
    }

    // 게시판 상태
    @Transactional(readOnly = true)
    public AdminDTO.DashboardBoardStatusRes getDashboardBoardStatus(LocalDate weekStart)
    {
        assertAdmin();

        Range cur = weekRange(weekStart);

        List<BoardCountRow> postRows = postRepository.countPostByBoardInRange(cur.from, cur.to);
        List<BoardCountRow> commentRows = commentRepository.countCommentByBoardInRange(cur.from, cur.to);
        List<BoardCountRow> recommentRows = recommentRepository.countRecommentByBoardInRange(cur.from, cur.to);

        Map<String, Stat> map = new HashMap<>();
        merge(map, postRows, "post");
        merge(map, commentRows, "comment");
        merge(map, recommentRows, "recomment");

        List<AdminDTO.BoardRow> items = new ArrayList<>();
        for (Stat s : map.values())
        {
            long p = s.post;
            long c = s.comment + s.recomment;
            double cpr = safeDiv(c, p);

            items.add(new AdminDTO.BoardRow(
                    parseBoardIdOrNull(s.boardId),
                    (s.boardName == null) ? "" : s.boardName,
                    p, c, cpr
            ));
        }

        items.sort((a, b) -> {
            int c1 = Long.compare(b.getPost(), a.getPost());
            if (c1 != 0) return c1;
            int c2 = Long.compare(b.getComment(), a.getComment());
            if (c2 != 0) return c2;
            return a.getName().compareTo(b.getName());
        });

        return new AdminDTO.DashboardBoardStatusRes(items);
    }

    private static class Stat
    {
        String boardId;
        String boardName;
        long post;
        long comment;
        long recomment;

        long noComment;
        long authorOnly;
        long activeUser;
    }

    private void merge(Map<String, Stat> map, List<BoardCountRow> rows, String type)
    {
        if (rows == null) return;

        for (BoardCountRow r : rows)
        {
            if (r == null) continue;
            String boardId = r.getBoardId();
            if (boardId == null || boardId.isBlank()) continue;

            Stat s = map.computeIfAbsent(boardId, k -> {
                Stat ns = new Stat();
                ns.boardId = boardId;
                ns.boardName = r.getBoardName();
                return ns;
            });

            if ((s.boardName == null || s.boardName.isBlank()) && r.getBoardName() != null)
                s.boardName = r.getBoardName();

            long cnt = r.getCnt();
            if ("post".equals(type)) s.post += cnt;
            else if ("comment".equals(type)) s.comment += cnt;
            else if ("recomment".equals(type)) s.recomment += cnt;
            else if ("noComment".equals(type)) s.noComment += cnt;
            else if ("authorOnly".equals(type)) s.authorOnly += cnt;
            else if ("activeUser".equals(type)) s.activeUser += cnt;
        }
    }

    private Long parseBoardIdOrNull(String boardId)
    {
        if (boardId == null || boardId.isBlank()) return null;
        try { return Long.valueOf(boardId); }
        catch (Exception e) { return null; }
    }

    // 대화 품질
    @Transactional(readOnly = true)
    public AdminDTO.DashboardConversationQualityRes getDashboardConversationQuality(LocalDate weekStart)
    {
        assertAdmin();

        Range cur = weekRange(weekStart);

        // 기준이 되는 이번 주 작성된 글 수
        List<BoardCountRow> postRows = postRepository.countPostByBoardInRange(cur.from, cur.to);

        // 이번 주 작성된 글 중, 이번 주 댓글/대댓글이 없는 글
        List<BoardCountRow> noCommentRows = postRepository.countNoCommentPostByBoardInRange(cur.from, cur.to);

        // 이번 주 작성된 글 중, 이번 주 댓글/대댓글이 "작성자만" 단 글
        List<BoardCountRow> authorOnlyRows = postRepository.countAuthorOnlyCommentPostByBoardInRange(cur.from, cur.to);

        // 게시판 내 활동자 수 (이번 주 글/댓글/대댓글 작성자 union distinct)
        List<BoardCountRow> activeUserRows = commentRepository.countActiveUserByBoardInRange(cur.from, cur.to);

        Map<String, Stat> map = new HashMap<>();
        merge(map, postRows, "post");
        merge(map, noCommentRows, "noComment");
        merge(map, authorOnlyRows, "authorOnly");
        merge(map, activeUserRows, "activeUser");

        List<AdminDTO.QualityRow> items = new ArrayList<>();
        for (Stat s : map.values())
        {
            if (s.post <= 0) continue; // 이번 주 글이 없는 게시판은 품질 표에서 제외

            double avgUser = safeDiv(s.activeUser, s.post);

            items.add(new AdminDTO.QualityRow(
                    parseBoardIdOrNull(s.boardId),
                    (s.boardName == null) ? "" : s.boardName,
                    s.noComment,
                    s.authorOnly,
                    avgUser
            ));
        }

        // 정렬: 글 많은 순 → 댓글없는 글 많은 순(문제 보이는 순) → 이름
        items.sort((a, b) -> {
            int c1 = Long.compare(
                map.getOrDefault(String.valueOf(b.getBoardId()), new Stat()).post,
                map.getOrDefault(String.valueOf(a.getBoardId()), new Stat()).post
            );
            if (c1 != 0) return c1;

            int c2 = Long.compare(b.getNoComment(), a.getNoComment());
            if (c2 != 0) return c2;

            return a.getName().compareTo(b.getName());
        });

        return new AdminDTO.DashboardConversationQualityRes(items);
    }

    // 유저 행동 이상
    @Transactional(readOnly = true)
    public AdminDTO.DashboardUserAnomalyRes getDashboardUserAnomaly(LocalDate weekStart)
    {
        assertAdmin();

        Range cur = weekRange(weekStart);

        long postOver10 = postRepository.countPostOver10UsersInWeek(cur.from, cur.to);
        long commentOver50 = commentRepository.countCommentOver50UsersInWeek(cur.from, cur.to);

        // 상위 1% 점유율
        List<UserScoreRow> rows = commentRepository.sumUserScoreInRange(cur.from, cur.to);

        long total = 0L;
        List<UserScore> scores = new ArrayList<>();
        if (rows != null)
        {
            for (UserScoreRow r : rows)
            {
                if (r == null) continue;
                String uid = r.getUserId();
                long sc = r.getScore();
                if (uid == null || uid.isBlank()) continue;
                if (sc <= 0) continue;
                scores.add(new UserScore(uid, sc));
                total += sc;
            }
        }

        scores.sort((a, b) -> Long.compare(b.score, a.score));

        long n = scores.size();
        long topN = (n <= 0) ? 0 : (long)Math.ceil(n * 0.01);
        if (topN <= 0 && n > 0) topN = 1;

        long topSum = 0L;
        for (int i = 0; i < scores.size() && i < topN; i++) topSum += scores.get(i).score;

        double top1Pct = (total <= 0) ? 0.0 : ((double)topSum * 100.0) / (double)total;

        return new AdminDTO.DashboardUserAnomalyRes(
                postOver10,
                commentOver50,
                top1Pct,
                topN
        );
    }

    private static class UserScore
    {
        final String userId;
        final long score;
        UserScore(String userId, long score)
        {
            this.userId = userId;
            this.score = score;
        }
    }
}