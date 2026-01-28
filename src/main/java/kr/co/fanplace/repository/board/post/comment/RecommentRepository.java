package kr.co.fanplace.repository.board.post.comment;

import kr.co.fanplace.entity.board.post.comment.Recomment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecommentRepository extends JpaRepository<Recomment, Long>
{
    long countByComment_Post_IdAndDeletedFalseAndComment_DeletedFalse(Long postId);

    @Query("select r.id from Recomment r where r.comment.id in :commentIds")
    List<Long> findIdsByCommentIds(@Param("commentIds") List<Long> commentIds);

    @Modifying
    @Query("delete from Recomment r where r.comment.id in :commentIds")
    int deleteByCommentIds(@Param("commentIds") List<Long> commentIds);
}