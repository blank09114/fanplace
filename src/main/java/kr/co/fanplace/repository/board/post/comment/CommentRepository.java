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
    interface UserActivityCommentRow
    {
        String getType();
        Long getId();

        Long getPostId();

        String getBoardId();
        String getBoardName();

        String getCategoryId();
        String getCategoryName();

        String getContent();
        java.time.LocalDateTime getCreatedAt();
    }

    long countByPost_IdAndDeletedFalse(Long postId);

    @Query("select c.id from Comment c where c.post.id in :postIds")
    List<Long> findIdsByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying
    @Query("delete from Comment c where c.post.id in :postIds")
    int deleteByPostIds(@Param("postIds") List<Long> postIds);

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
            u.id,
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

    @Query("""
        select c.id from Comment c
        where c.deleted = true
            and c.deletedAt is not null
            and (c.deletedReason is null or c.deletedReason = '')
            and c.deletedAt < :cutoff
        order by c.id asc
    """)
    List<Long> findPurgeTargetIdsNoReason(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Query("""
    select c.id from Comment c
    where c.deleted = true
        and c.deletedAt is not null
        and (c.deletedReason is not null and c.deletedReason <> '')
        and c.deletedAt < :cutoff
    order by c.id asc
    """)
    List<Long> findPurgeTargetIdsWithReason(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Query("""
        select count(c)
        from Comment c
        where c.post.id = :postId
            and c.deleted = false
            and c.id < :commentId
    """)
    long countVisibleBefore(@Param("postId") Long postId, @Param("commentId") Long commentId);

    @Query(value = """
    select * from (
        select
            'COMMENT' as type,
            c.comment_id as id,
            p.post_id as postId,
            b.board_id as boardId,
            b.board_name as boardName,
            ct.category_id as categoryId,
            ct.category_name as categoryName,
            c.comment_content as content,
            c.comment_date as createdAt
        from comment_tbl c
        join post_tbl p on p.post_id = c.post_id
        join board_tbl b on b.board_id = p.board_id
        join category_tbl ct on ct.category_id = p.category_id
        where c.user_id = :userId
            and (:admin = true or c.comment_is_deleted = false)
            and (:admin = true or p.post_is_deleted = false)
        union all
        select
            'RECOMMENT' as type,
            r.recomment_id as id,
            p.post_id as postId,
            b.board_id as boardId,
            b.board_name as boardName,
            ct.category_id as categoryId,
            ct.category_name as categoryName,
            r.recomment_content as content,
            r.recomment_date as createdAt
        from recomment_tbl r
        join comment_tbl c on c.comment_id = r.comment_id
        join post_tbl p on p.post_id = c.post_id
        join board_tbl b on b.board_id = p.board_id
        join category_tbl ct on ct.category_id = p.category_id
        where r.author_user_id = :userId
        and (:admin = true or r.recomment_is_deleted = false)
        and (:admin = true or p.post_is_deleted = false)
    ) t order by t.createdAt desc, t.type asc, t.id desc
    """, countQuery = """
    select count(*)
    from (
        select c.comment_id as id
        from comment_tbl c
        join post_tbl p on p.post_id = c.post_id
        where c.user_id = :userId
            and (:admin = true or c.comment_is_deleted = false)
            and (:admin = true or p.post_is_deleted = false)
        union all
        select r.recomment_id as id
        from recomment_tbl r
        join comment_tbl c on c.comment_id = r.comment_id
        join post_tbl p on p.post_id = c.post_id
        where r.author_user_id = :userId
        and (:admin = true or r.recomment_is_deleted = false)
        and (:admin = true or p.post_is_deleted = false)
    ) t
    """, nativeQuery = true)
    Page<UserActivityCommentRow> findUserActivityCommentPage(@Param("userId") String userId, @Param("admin") boolean admin, Pageable pageable);
}