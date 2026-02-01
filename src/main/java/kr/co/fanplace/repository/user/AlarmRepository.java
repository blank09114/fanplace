package kr.co.fanplace.repository.user;

import kr.co.fanplace.entity.user.Alarm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<Alarm, Long>
{
    @EntityGraph(attributePaths =
    {
        "comment", "comment.post", "comment.post.board", "recomment",
        "recomment.comment", "recomment.comment.post", "recomment.comment.post.board"
    })
    Page<Alarm> findByUser_IdOrderByIdDesc(String userId, Pageable pageable);

    @EntityGraph(attributePaths =
    {
        "comment", "comment.post", "comment.post.board", "recomment",
        "recomment.comment", "recomment.comment.post", "recomment.comment.post.board"
    })
    Page<Alarm> findByUser_IdAndCheckedAtIsNullOrderByIdDesc(String userId, Pageable pageable);

    Optional<Alarm> findByIdAndUser_Id(Long id, String userId);

    @Modifying
    @Query("delete from Alarm a where a.comment.id in :commentIds")
    int deleteByCommentIds(@Param("commentIds") List<Long> commentIds);

    @Modifying
    @Query("delete from Alarm a where a.recomment.id in :recommentIds")
    int deleteByRecommentIds(@Param("recommentIds") List<Long> recommentIds);

    long countByUser_IdAndCheckedAtIsNull(String userId);
}