package kr.co.fanplace.repository.board.post.comment;

import kr.co.fanplace.dto.board.CommentDTO;
import kr.co.fanplace.entity.board.post.comment.Comment;
import kr.co.fanplace.repository.BoardCountRow;
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

    long countByUser_IdAndDeletedFalse(String userId);

    // 유저 기간 내 댓글 수
    @Query("""
        select count(c)
        from Comment c
        where c.user is not null
            and c.user.id = :userId
            and c.deleted = false
            and c.post.deleted = false
            and c.createdAt >= :from and c.createdAt < :to
    """)
    long countUserCommentInRange(@Param("userId") String userId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 유저 기간 내 게시판별 댓글 수
    @Query("""
        select
            c.post.board.id as boardId,
            c.post.board.name as boardName,
            count(c) as cnt
        from Comment c
        where c.user is not null
            and c.user.id = :userId
            and c.deleted = false
            and c.post.deleted = false
            and c.createdAt >= :from and c.createdAt < :to
        group by c.post.board.id, c.post.board.name
    """)
    List<BoardCountRow> countUserCommentByBoardInRange(@Param("userId") String userId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 전체 기간 내 댓글 수
    @Query("""
        select count(c)
        from Comment c
        where c.deleted = false
            and c.post.deleted = false
            and c.createdAt >= :from and c.createdAt < :to
    """)
    long countCommentInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 전체 기간 내 게시판별 댓글 수
    @Query("""
        select
            c.post.board.id as boardId,
            c.post.board.name as boardName,
            count(c) as cnt
        from Comment c
        where c.deleted = false
            and c.post.deleted = false
            and c.createdAt >= :from and c.createdAt < :to
        group by c.post.board.id, c.post.board.name
    """)
    List<BoardCountRow> countCommentByBoardInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 삭제된 댓글/대댓글
    interface DeletedCommentRow
    {
        String getType();
        Long getId();

        Long getPostId();
        String getBoardId();
        String getBoardName();
        String getCategoryId();
        String getCategoryName();

        String getAuthorUserId();
        String getAuthorName();

        String getContent();
        LocalDateTime getCreatedAt();

        LocalDateTime getDeletedAt();
        String getDeletedReason();
    }

    @Query(value =
        "select * from ( " +
            "   select " +
            "     'COMMENT' as type, " +
            "     c.comment_id as id, " +
            "     p.post_id as postId, " +
            "     b.board_id as boardId, " +
            "     b.board_name as boardName, " +
            "     ct.category_id as categoryId, " +
            "     ct.category_name as categoryName, " +
            "     u.user_id as authorUserId, " +
            "     u.user_name as authorName, " +
            "     c.comment_content as content, " +
            "     c.comment_date as createdAt, " +
            "     c.comment_deleted_at as deletedAt, " +
            "     c.comment_deleted_reason as deletedReason " +
            "   from comment_tbl c " +
            "   join post_tbl p on p.post_id = c.post_id " +
            "   join board_tbl b on b.board_id = p.board_id " +
            "   join category_tbl ct on ct.category_id = p.category_id " +
            "   left join user_tbl u on u.user_id = c.user_id " +
            "   where c.comment_is_deleted = true " +
            "     and (:q is null or :q = '' or c.comment_content like concat('%', :q, '%')) " +
            "   union all " +
            "   select " +
            "     'RECOMMENT' as type, " +
            "     r.recomment_id as id, " +
            "     p.post_id as postId, " +
            "     b.board_id as boardId, " +
            "     b.board_name as boardName, " +
            "     ct.category_id as categoryId, " +
            "     ct.category_name as categoryName, " +
            "     u.user_id as authorUserId, " +
            "     u.user_name as authorName, " +
            "     r.recomment_content as content, " +
            "     r.recomment_date as createdAt, " +
            "     r.recomment_deleted_at as deletedAt, " +
            "     r.recomment_deleted_reason as deletedReason " +
            "   from recomment_tbl r " +
            "   join comment_tbl c on c.comment_id = r.comment_id " +
            "   join post_tbl p on p.post_id = c.post_id " +
            "   join board_tbl b on b.board_id = p.board_id " +
            "   join category_tbl ct on ct.category_id = p.category_id " +
            "   left join user_tbl u on u.user_id = r.author_user_id " +
            "   where r.recomment_is_deleted = true " +
            "     and (:q is null or :q = '' or r.recomment_content like concat('%', :q, '%')) " +
            ") t " +
            "order by t.deletedAt desc, t.id desc ", countQuery =
        "select count(*) from ( " +
            "   select c.comment_id as id " +
            "   from comment_tbl c " +
            "   where c.comment_is_deleted = true " +
            "     and (:q is null or :q = '' or c.comment_content like concat('%', :q, '%')) " +
            "   union all " +
            "   select r.recomment_id as id " +
            "   from recomment_tbl r " +
            "   where r.recomment_is_deleted = true " +
            "     and (:q is null or :q = '' or r.recomment_content like concat('%', :q, '%')) " +
            ") t ",
    nativeQuery = true)
    Page<DeletedCommentRow> findDeletedCommentPage(@Param("q") String q, Pageable pageable);

    @Query(value = """
        select date(c.comment_date) as day, count(*) as cnt
        from comment_tbl c
        join post_tbl p on p.post_id = c.post_id
        where c.comment_is_deleted = false
          and p.post_is_deleted = false
          and c.comment_date >= :from and c.comment_date < :to
        group by date(c.comment_date)
        order by day asc
    """, nativeQuery = true)
    List<kr.co.fanplace.repository.DayCountRow> countCommentByDayInRange
    (@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 주간 활성 유저 수 (글/댓글/대댓글 중 하나라도)
    @Query(value = """
    select count(*) from (
        select distinct t.uid
        from (
            select p.user_id as uid
            from post_tbl p
            where p.post_is_deleted = false
              and p.user_id is not null
              and p.post_date >= :from and p.post_date < :to

            union

            select c.user_id as uid
            from comment_tbl c
            join post_tbl p on p.post_id = c.post_id
            where c.comment_is_deleted = false
              and p.post_is_deleted = false
              and c.user_id is not null
              and c.comment_date >= :from and c.comment_date < :to

            union

            select r.author_user_id as uid
            from recomment_tbl r
            join comment_tbl c on c.comment_id = r.comment_id
            join post_tbl p on p.post_id = c.post_id
            where r.recomment_is_deleted = false
              and c.comment_is_deleted = false
              and p.post_is_deleted = false
              and r.author_user_id is not null
              and r.recomment_date >= :from and r.recomment_date < :to
        ) t
    ) x
""", nativeQuery = true)
    long countActiveUserInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 일별 활성 유저 수
    @Query(value = """
        select t.day as day, count(distinct t.uid) as cnt
        from (
            select date(p.post_date) as day, p.user_id as uid
            from post_tbl p
            where p.post_is_deleted = false
              and p.user_id is not null
              and p.post_date >= :from and p.post_date < :to
    
            union all
    
            select date(c.comment_date) as day, c.user_id as uid
            from comment_tbl c
            join post_tbl p on p.post_id = c.post_id
            where c.comment_is_deleted = false
              and p.post_is_deleted = false
              and c.user_id is not null
              and c.comment_date >= :from and c.comment_date < :to
    
            union all
    
            select date(r.recomment_date) as day, r.author_user_id as uid
            from recomment_tbl r
            join comment_tbl c on c.comment_id = r.comment_id
            join post_tbl p on p.post_id = c.post_id
            where r.recomment_is_deleted = false
              and c.comment_is_deleted = false
              and p.post_is_deleted = false
              and r.author_user_id is not null
              and r.recomment_date >= :from and r.recomment_date < :to
        ) t
        group by t.day
        order by t.day asc
    """, nativeQuery = true)
    List<kr.co.fanplace.repository.DayCountRow> countActiveUserByDayInRange
    (@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        select t.uid as userId, sum(t.score) as score
        from (
            select p.user_id as uid, count(*) * 2 as score
            from post_tbl p
            where p.post_is_deleted = false
              and p.user_id is not null
              and p.post_date >= :from and p.post_date < :to
            group by p.user_id
    
            union all
    
            select c.user_id as uid, count(*) as score
            from comment_tbl c
            join post_tbl p on p.post_id = c.post_id
            where c.comment_is_deleted = false
              and p.post_is_deleted = false
              and c.user_id is not null
              and c.comment_date >= :from and c.comment_date < :to
            group by c.user_id
    
            union all
    
            select r.author_user_id as uid, count(*) as score
            from recomment_tbl r
            join comment_tbl c on c.comment_id = r.comment_id
            join post_tbl p on p.post_id = c.post_id
            where r.recomment_is_deleted = false
              and c.comment_is_deleted = false
              and p.post_is_deleted = false
              and r.author_user_id is not null
              and r.recomment_date >= :from and r.recomment_date < :to
            group by r.author_user_id
        ) t
        group by t.uid
    """, nativeQuery = true)
    List<kr.co.fanplace.repository.UserScoreRow> sumUserScoreInRange
    (@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 일 50회 이상 댓글 작성 유저 수 (댓글 + 대댓글 합산, 주간 범위 중 '어느 하루라도' 50회 이상이면 포함)
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

    // 게시판별 활동자 수 (글/댓글/대댓글 작성자 union distinct)
    @Query(value = """
        select t.boardId as boardId, t.boardName as boardName, count(distinct t.uid) as cnt
        from (
            select p.board_id as boardId, b.board_name as boardName, p.user_id as uid
            from post_tbl p
            join board_tbl b on b.board_id = p.board_id
            where p.post_is_deleted = false
              and p.user_id is not null
              and p.post_date >= :from and p.post_date < :to
    
            union all
    
            select p.board_id as boardId, b.board_name as boardName, c.user_id as uid
            from comment_tbl c
            join post_tbl p on p.post_id = c.post_id
            join board_tbl b on b.board_id = p.board_id
            where c.comment_is_deleted = false
              and p.post_is_deleted = false
              and c.user_id is not null
              and c.comment_date >= :from and c.comment_date < :to
    
            union all
    
            select p.board_id as boardId, b.board_name as boardName, r.author_user_id as uid
            from recomment_tbl r
            join comment_tbl c on c.comment_id = r.comment_id
            join post_tbl p on p.post_id = c.post_id
            join board_tbl b on b.board_id = p.board_id
            where r.recomment_is_deleted = false
              and c.comment_is_deleted = false
              and p.post_is_deleted = false
              and r.author_user_id is not null
              and r.recomment_date >= :from and r.recomment_date < :to
        ) t
        group by t.boardId, t.boardName
    """, nativeQuery = true)
    List<kr.co.fanplace.repository.BoardCountRow> countActiveUserByBoardInRange
    (@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}