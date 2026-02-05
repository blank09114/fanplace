package kr.co.fanplace.repository.board.post.comment;

import kr.co.fanplace.dto.board.CommentDTO;
import kr.co.fanplace.entity.board.post.comment.Recomment;
import kr.co.fanplace.repository.BoardCountRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
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
            au.id,
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

    // purge 대상 조회
    @Query("""
        select r.id from Recomment r
        where r.deleted = true
            and r.deletedAt is not null
            and (
                ( (r.deletedReason is null or trim(r.deletedReason) = '') and r.deletedAt < :cutoffNoReason )
                or ( (r.deletedReason is not null and trim(r.deletedReason) <> '') and r.deletedAt < :cutoffWithReason )
            )
        order by r.id asc
    """)
    List<Long> findPurgeTargetIds(@Param("cutoffNoReason") LocalDateTime cutoffNoReason, @Param("cutoffWithReason") LocalDateTime cutoffWithReason, Pageable pageable);

    // purge 대상 하드 삭제
    @Modifying
    @Query("delete from Recomment r where r.id in :recommentIds")
    int deleteByIds(@Param("recommentIds") List<Long> recommentIds);

    @Query("""
        select r.id from Recomment r
        where r.deleted = true
            and r.deletedAt is not null
            and (r.deletedReason is null or r.deletedReason = '')
            and r.deletedAt < :cutoff
        order by r.id asc
    """)
    List<Long> findPurgeTargetIdsNoReason(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Query("""
    select r.id from Recomment r
    where r.deleted = true
        and r.deletedAt is not null
        and (r.deletedReason is not null and r.deletedReason <> '')
        and r.deletedAt < :cutoff
    order by r.id asc
    """)
    List<Long> findPurgeTargetIdsWithReason(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    long countByAuthorUser_IdAndDeletedFalse(String userId);

    // 유저 기간 내 대댓글 수
    @Query("""
        select count(r)
        from Recomment r
        where r.authorUser is not null
            and r.authorUser.id = :userId
            and r.deleted = false
            and r.comment.deleted = false
            and r.comment.post.deleted = false
            and r.createdAt >= :from and r.createdAt < :to
    """)
    long countUserRecommentInRange(@Param("userId") String userId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 유저 기간 내 게시판별 대댓글 수
    @Query("""
        select
            r.comment.post.board.id as boardId,
            r.comment.post.board.name as boardName,
            count(r) as cnt
        from Recomment r
        where r.authorUser is not null
            and r.authorUser.id = :userId
            and r.deleted = false
            and r.comment.deleted = false
            and r.comment.post.deleted = false
            and r.createdAt >= :from and r.createdAt < :to
        group by r.comment.post.board.id, r.comment.post.board.name
    """)
    List<BoardCountRow> countUserRecommentByBoardInRange(@Param("userId") String userId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 전체 기간 내 대댓글 수
    @Query("""
        select count(r)
        from Recomment r
        where r.deleted = false
            and r.comment.deleted = false
            and r.comment.post.deleted = false
            and r.createdAt >= :from and r.createdAt < :to
    """)
    long countRecommentInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 전체 기간 내 게시판별 대댓글 수
    @Query("""
        select
            r.comment.post.board.id as boardId,
            r.comment.post.board.name as boardName,
            count(r) as cnt
        from Recomment r
        where r.deleted = false
            and r.comment.deleted = false
            and r.comment.post.deleted = false
            and r.createdAt >= :from and r.createdAt < :to
        group by r.comment.post.board.id, r.comment.post.board.name
    """)
    List<BoardCountRow> countRecommentByBoardInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        select date(r.recomment_date) as day, count(*) as cnt
        from recomment_tbl r
        join comment_tbl c on c.comment_id = r.comment_id
        join post_tbl p on p.post_id = c.post_id
        where r.recomment_is_deleted = false
          and c.comment_is_deleted = false
          and p.post_is_deleted = false
          and r.recomment_date >= :from and r.recomment_date < :to
        group by date(r.recomment_date)
        order by day asc
    """, nativeQuery = true)
    List<kr.co.fanplace.repository.DayCountRow> countRecommentByDayInRange
    (@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        select count(*) from (
            select x.uid
            from (
                select date(c.comment_date) as day, c.user_id as uid, count(*) as cnt
                from comment_tbl c
                join post_tbl p on p.post_id = c.post_id
                where c.comment_is_deleted = false
                  and p.post_is_deleted = false
                  and c.user_id is not null
                  and c.comment_date >= :from and c.comment_date < :to
                group by date(c.comment_date), c.user_id
    
                union all
    
                select date(r.recomment_date) as day, r.author_user_id as uid, count(*) as cnt
                from recomment_tbl r
                join comment_tbl c on c.comment_id = r.comment_id
                join post_tbl p on p.post_id = c.post_id
                where r.recomment_is_deleted = false
                  and c.comment_is_deleted = false
                  and p.post_is_deleted = false
                  and r.author_user_id is not null
                  and r.recomment_date >= :from and r.recomment_date < :to
                group by date(r.recomment_date), r.author_user_id
            ) x
            group by x.day, x.uid
            having sum(x.cnt) >= 50
        ) t
    """, nativeQuery = true)
    long countCommentOver50UsersInWeek(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}