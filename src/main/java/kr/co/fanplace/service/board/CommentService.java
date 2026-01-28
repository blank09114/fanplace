package kr.co.fanplace.service.board;

import kr.co.fanplace.repository.board.post.comment.CommentRepository;
import kr.co.fanplace.repository.board.post.comment.RecommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService
{
    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;

    // 댓글 수 카운트
    @Transactional(readOnly = true)
    public long getTotalCommentCount(Long postId)
    {
        long commentCount = commentRepository.countByPost_IdAndDeletedFalse(postId);
        long recommentCount = recommentRepository.countByComment_Post_IdAndDeletedFalseAndComment_DeletedFalse(postId);
        return commentCount + recommentCount;
    }
}