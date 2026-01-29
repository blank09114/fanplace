package kr.co.fanplace.repository.board.post.comment;

import kr.co.fanplace.dto.board.CommentDTO;
import kr.co.fanplace.entity.board.post.comment.Recomment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecommentRepository extends JpaRepository<Recomment, Long>
{
    long countByComment_Post_IdAndDeletedFalseAndComment_DeletedFalse(Long postId);

    @Query("select r.id from Recomment r where r.comment.id in :commentIds")
    List<Long> findIdsByCommentIds(@Param("commentIds") List<Long> commentIds);

    @Modifying
    @Query("delete from Recomment r where r.comment.id in :commentIds")
    int deleteByCommentIds(@Param("commentIds") List<Long> commentIds);

    @Query("""
        select new kr.co.fanplace.dto.board.CommentDTO$RecommentItem(
            r.id,
            r.comment.id,
            r.deleted,
            case
                when r.deleted = true then(case when r.deletedReason is null or r.deletedReason = '' then '본인 삭제' else r.deletedReason end)
                else null
            end,
            r.deletedAt,
            case when au is null then '탈퇴 회원' else au.name end,
            case when mu is null then null else mu.name end,
            r.createdAt,
            r.content,
            (case when :loginUserId is not null and au is not null and au.id = :loginUserId then true else false end)
        )
        from Recomment r
        left join r.authorUser au
        left join r.mentionUser mu
        where r.comment.id in :commentIds and (:admin = true or r.deleted = false)
        order by r.id asc
    """)

    List<CommentDTO.RecommentItem> findRecommentItems
    (@Param("commentIds") List<Long> commentIds, @Param("admin") boolean admin, @Param("loginUserId") String loginUserId);
    @Modifying
    @Query("""
        update Recomment r
            set r.deleted = true, r.deletedReason = :reason, r.deletedAt = :now
        where r.comment.id = :commentId and r.deleted = false
    """)
    int softDeleteByCommentId(@Param("commentId") Long commentId, @Param("now") LocalDateTime now, @Param("reason") String reason);
}