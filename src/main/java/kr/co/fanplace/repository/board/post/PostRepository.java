package kr.co.fanplace.repository.board.post;

import kr.co.fanplace.dto.board.PostDTO;
import kr.co.fanplace.entity.board.post.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>
{
    // 게시판 정보 조회
    Optional<Post> findByIdAndBoard_Id(Long postId, String boardId);

    // 이전글
    @Query("""
        select max(p.id)
        from Post p
        where p.board.id = :boardId
            and (:admin = true or p.deleted = false)
            and p.id < :postId
    """)
    Long findPrevPostId(@Param("boardId") String boardId, @Param("admin") boolean admin, @Param("postId") Long postId);

    // 다음글
    @Query("""
        select min(p.id)
        from Post p
        where p.board.id = :boardId
            and (:admin = true or p.deleted = false)
            and p.id > :postId
    """)
    Long findNextPostId(@Param("boardId") String boardId, @Param("admin") boolean admin, @Param("postId") Long postId);

    // 삭제 대상 찾기
    @Query("""
    select p.id from Post p
    where p.deleted = true
        and p.deletedAt is not null
        and (
            ( (p.deletedReason is null or trim(p.deletedReason) = '') and p.deletedAt < :cutoffNoReason )
            or ( (p.deletedReason is not null and trim(p.deletedReason) <> '') and p.deletedAt < :cutoffWithReason )
        )
    """)
    List<Long> findPurgeTargetIds(@Param("cutoffNoReason") LocalDateTime cutoffNoReason,
    @Param("cutoffWithReason") LocalDateTime cutoffWithReason, Pageable pageable);

    // 목록 조회
    @Query(
        value = "select new kr.co.fanplace.dto.board.PostDTO$ListItem( " +
            "p.id, " +
            "p.category.id, p.category.name, " +
            "u.id, " +
            "case when u is null then '탈퇴 회원' else u.name end, " +
            "p.createdAt, " +
            "pl.title, " +
            "(select count(v) from PostView v where v.post = p), " +
            "(select count(l) from PostLike l where l.post = p), " +
            "( (select count(c) from Comment c where c.post = p and c.deleted = false) + " +
            "  (select count(r) from Recomment r where r.comment.post = p and r.deleted = false and r.comment.deleted = false) )" +
            ") " +
            "from Post p " +
            "join PostLog pl on pl.post = p and pl.id = (select max(pl2.id) from PostLog pl2 where pl2.post = p) " +
            "left join p.user u " +
            "where p.board.id = :boardId and p.deleted = false " +
            "and (:categoryId is null or p.category.id = :categoryId) " +
            "and (:hot = false or (select count(l2) from PostLike l2 where l2.post = p) >= :hotCut) " +
            "order by p.id desc",
        countQuery = "select count(p) from Post p " +
            "where p.board.id = :boardId and p.deleted = false " +
            "and (:categoryId is null or p.category.id = :categoryId) " +
            "and (:hot = false or (select count(l2) from PostLike l2 where l2.post = p) >= :hotCut)"
    )
    Page<PostDTO.ListItem> findPostList(
        @Param("boardId") String boardId, @Param("categoryId") String categoryId,
        @Param("hot") boolean hot, @Param("hotCut") long hotCut, Pageable pageable
    );

    // 검색
    @Query(
        value =
            "select new kr.co.fanplace.dto.board.PostDTO$ListItem( " +
                "p.id, " +
                "p.category.id, p.category.name, " +
                "u.id, " +
                "case when u is null then '탈퇴 회원' else u.name end, " +
                "p.createdAt, " +
                "pl.title, " +
                "(select count(v) from PostView v where v.post = p), " +
                "(select count(l) from PostLike l where l.post = p), " +
                "( (select count(c) from Comment c where c.post = p and c.deleted = false) + " +
                "  (select count(r) from Recomment r where r.comment.post = p and r.deleted = false and r.comment.deleted = false) )" +
                ") " +
                "from Post p " +
                "join PostLog pl on pl.post = p and pl.id = (select max(pl2.id) from PostLog pl2 where pl2.post = p) " +
                "left join p.user u " +
                "where p.board.id = :boardId and p.deleted = false " +
                "and (:categoryId is null or p.category.id = :categoryId) " +
                "and (:hot = false or (select count(l2) from PostLike l2 where l2.post = p) >= :hotCut) " +
                "and lower(pl.title) like lower(concat('%', :q, '%')) " +
                "order by p.id desc",
        countQuery =
        "select count(p) " +
            "from Post p " +
            "join PostLog pl on pl.post = p and pl.id = (select max(pl2.id) from PostLog pl2 where pl2.post = p) " +
            "where p.board.id = :boardId and p.deleted = false " +
            "and (:categoryId is null or p.category.id = :categoryId) " +
            "and (:hot = false or (select count(l2) from PostLike l2 where l2.post = p) >= :hotCut) " +
            "and lower(pl.title) like lower(concat('%', :q, '%')) "
    )
    Page<PostDTO.ListItem> searchPostListByTitle(
        @Param("boardId") String boardId, @Param("categoryId") String categoryId,
        @Param("hot") boolean hot, @Param("hotCut") long hotCut,
        @Param("q") String q, Pageable pageable
    );

    // 통합 검색
    @Query(value = """
        select new kr.co.fanplace.dto.board.PostDTO$UnivListItem(
            p.id, p.board.id, p.board.name,
            case when u is null then '탈퇴 회원' else u.name end,
            p.createdAt, pl.title,
            (select count(v) from PostView v where v.post = p),
            (select count(l) from PostLike l where l.post = p),
            (
                (select count(c) from Comment c where c.post = p and c.deleted = false)
                + (select count(r) from Recomment r where r.comment.post = p and r.deleted = false and r.comment.deleted = false)
            )
        ) from Post p
        join PostLog pl on pl.post = p and pl.id = (select max(pl2.id) from PostLog pl2 where pl2.post = p)
        left join p.user u
        where p.deleted = false and lower(pl.title) like lower(concat('%', :q, '%'))
        order by p.id desc
    """, countQuery = """
        select count(p) from Post p
        join PostLog pl on pl.post = p and pl.id = (select max(pl2.id) from PostLog pl2 where pl2.post = p)
        where p.deleted = false and lower(pl.title) like lower(concat('%', :q, '%'))
    """)
    Page<PostDTO.UnivListItem> searchUnivByTitle(@Param("q") String q, Pageable pageable);
}