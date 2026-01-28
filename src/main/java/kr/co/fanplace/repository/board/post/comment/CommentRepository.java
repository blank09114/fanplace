package kr.co.fanplace.repository.board.post.comment;

import kr.co.fanplace.entity.board.post.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long>
{
    long countByPost_IdAndDeletedFalse(Long postId);

    @Query("select c.id from Comment c where c.post.id in :postIds")
    List<Long> findIdsByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying
    @Query("delete from Comment c where c.post.id in :postIds")
    int deleteByPostIds(@Param("postIds") List<Long> postIds);
}