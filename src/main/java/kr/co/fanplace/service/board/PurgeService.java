package kr.co.fanplace.service.board;

import kr.co.fanplace.repository.board.post.PostLikeRepository;
import kr.co.fanplace.repository.board.post.PostLogRepository;
import kr.co.fanplace.repository.board.post.PostRepository;
import kr.co.fanplace.repository.board.post.PostViewRepository;
import kr.co.fanplace.repository.board.post.comment.CommentRepository;
import kr.co.fanplace.repository.board.post.comment.RecommentRepository;
import kr.co.fanplace.repository.user.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurgeService
{
    private final PostRepository postRepository;
    private final PostLogRepository postLogRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostViewRepository postViewRepository;

    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;
    private final AlarmRepository alarmRepository;

    // 글 하드 삭제
    @Transactional
    public int purgeOnce(int batchSize)
    {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffNoReason = now.minusDays(30);
        LocalDateTime cutoffWithReason = now.minusDays(1);

        List<Long> postIds = postRepository.findPurgeTargetIds
                (cutoffNoReason, cutoffWithReason, PageRequest.of(0, batchSize));

        if (postIds.isEmpty()) return 0;

        // 댓글 id 수집
        List<Long> commentIds = commentRepository.findIdsByPostIds(postIds);

        // 알람 삭제 후 대댓글 삭제
        List<Long> recommentIds = commentIds.isEmpty() ? List.of() : recommentRepository.findIdsByCommentIds(commentIds);
        if (!recommentIds.isEmpty()) alarmRepository.deleteByRecommentIds(recommentIds);
        if (!commentIds.isEmpty())  recommentRepository.deleteByCommentIds(commentIds);

        // 알람 삭제 후 댓글 삭제
        if (!commentIds.isEmpty()) alarmRepository.deleteByCommentIds(commentIds);
        commentRepository.deleteByPostIds(postIds);

        // 글 하위 로그/좋아요/조회 삭제
        postLikeRepository.deleteByPostIds(postIds);
        postViewRepository.deleteByPostIds(postIds);
        postLogRepository.deleteByPostIds(postIds);

        // 마지막으로 글 삭제
        postRepository.deleteAllByIdInBatch(postIds);

        return postIds.size();
    }

    // 댓글 하드 삭제
    @Transactional
    public int purgeCommentsOnce(int batchSize)
    {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime cutoffNoReason = now.minusDays(30);
        LocalDateTime cutoffWithReason = now.minusYears(1);

        List<Long> commentIds = commentRepository.findPurgeTargetIdsNoReason
        (cutoffNoReason, PageRequest.of(0, batchSize));

        if (commentIds.size() < batchSize)
        {
            int remain = batchSize - commentIds.size();
            List<Long> more = commentRepository.findPurgeTargetIdsWithReason
            (cutoffWithReason, PageRequest.of(0, remain));
            if (!more.isEmpty()) commentIds.addAll(more);
        }

        if (commentIds.isEmpty()) return 0;

        // 대댓글 알람 삭제 → 대댓글 삭제
        List<Long> recommentIds = recommentRepository.findIdsByCommentIds(commentIds);
        if (!recommentIds.isEmpty()) alarmRepository.deleteByRecommentIds(recommentIds);
        recommentRepository.deleteByCommentIds(commentIds);

        // 댓글 알람 삭제 → 댓글 삭제
        alarmRepository.deleteByCommentIds(commentIds);
        commentRepository.deleteByIds(commentIds);

        return commentIds.size();
    }

    // 대댓글 하드 삭제
    @Transactional
    public int purgeRecommentsOnce(int batchSize)
    {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime cutoffNoReason = now.minusDays(30);
        LocalDateTime cutoffWithReason = now.minusYears(1);

        List<Long> recommentIds = recommentRepository.findPurgeTargetIds
        (cutoffNoReason, cutoffWithReason, PageRequest.of(0, batchSize));

        if (recommentIds.isEmpty()) return 0;

        // 대댓글 알람 삭제 → 대댓글 하드 삭제
        alarmRepository.deleteByRecommentIds(recommentIds);
        recommentRepository.deleteByIds(recommentIds);

        return recommentIds.size();
    }
}