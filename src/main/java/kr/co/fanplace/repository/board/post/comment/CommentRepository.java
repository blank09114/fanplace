package kr.co.fanplace.repository.board.post.comment;

import kr.co.fanplace.dto.board.CommentDTO;
import kr.co.fanplace.entity.board.post.comment.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long>
{
    long countByPost_IdAndDeletedFalse(Long postId);

    @Query("select c.id from Comment c where c.post.id in :postIds")
    List<Long> findIdsByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying
    @Query("delete from Comment c where c.post.id in :postIds")
    int deleteByPostIds(@Param("postIds") List<Long> postIds);

    // purge 대상 id 조회
    @Query("""
        select c.id from Comment c
        where c.deleted = true
            and c.deletedAt is not null
            and (
                ( (c.deletedReason is null or trim(c.deletedReason) = '') and c.deletedAt < :cutoffNoReason )
                or ( (c.deletedReason is not null and trim(c.deletedReason) <> '') and c.deletedAt < :cutoffWithReason )
            )
        order by c.id asc
    """)
    List<Long> findPurgeTargetIds(@Param("cutoffNoReason") LocalDateTime cutoffNoReason, @Param("cutoffWithReason") LocalDateTime cutoffWithReason, Pageable pageable);

    // purge 대상 하드 삭제
    @Modifying
    @Query("delete from Comment c where c.id in :commentIds")
    int deleteByIds(@Param("commentIds") List<Long> commentIds);

    // 댓글 조회
    @Query(value = """
        select new kr.co.fanplace.dto.board.CommentDTO$Item(
            c.id,
            c.deleted,
            case
                when c.deleted = true then(case when c.deletedReason is null or c.deletedReason = '' then '본인 삭제' else c.deletedReason end)
                else null
            end,
            c.deletedAt,
            case when u is null then '탈퇴 회원' else u.name end,
            c.createdAt,
            c.content,
            (case when :loginUserId is not null and u is not null and u.id = :loginUserId then true else false end),
            null
        )
        from Comment c
        left join c.user u
        where c.post.id = :postId and (:admin = true or c.deleted = false)
        order by c.id asc
    """, countQuery = """
        select count(c)
        from Comment c
        where c.post.id = :postId and (:admin = true or c.deleted = false)
    """)
    Page<CommentDTO.Item> findCommentPage
    (@Param("postId") Long postId, @Param("admin") boolean admin, @Param("loginUserId") String loginUserId, Pageable pageable);
}