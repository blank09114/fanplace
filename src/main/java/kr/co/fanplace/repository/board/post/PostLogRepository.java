package kr.co.fanplace.repository.board.post;

import kr.co.fanplace.entity.board.post.PostLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostLogRepository extends JpaRepository<PostLog, Long>
{
    Optional<PostLog> findFirstByPost_IdOrderByUpdatedAtDescIdDesc(Long postId);

    @Modifying
    @Query("delete from PostLog pl where pl.post.id in :postIds")
    int deleteByPostIds(@Param("postIds") List<Long> postIds);
}