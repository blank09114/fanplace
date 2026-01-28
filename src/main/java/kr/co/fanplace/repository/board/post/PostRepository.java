package kr.co.fanplace.repository.board.post;

import kr.co.fanplace.entity.board.post.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>
{
    Optional<Post> findByIdAndBoard_Id(Long postId, String boardId);

    @Query("select p.id from Post p " +
    "where p.board.id = :boardId and p.deleted = false and p.id < :postId " +
    "order by p.id desc")
    List<Long> findPrevPostId(String boardId, Long postId, Pageable pageable);

    @Query("select p.id from Post p " +
    "where p.board.id = :boardId and p.deleted = false and p.id > :postId " +
    "order by p.id asc")
    List<Long> findNextPostId(String boardId, Long postId, Pageable pageable);

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
}