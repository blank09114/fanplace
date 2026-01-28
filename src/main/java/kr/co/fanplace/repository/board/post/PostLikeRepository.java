package kr.co.fanplace.repository.board.post;

import kr.co.fanplace.entity.board.post.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long>
{
    boolean existsByPost_IdAndUser_Id(Long postId, String userId);
    long deleteByPost_IdAndUser_Id(Long postId, String userId);
    long countByPost_Id(Long postId);

    @Modifying
    @Query("delete from PostLike pl where pl.post.id in :postIds")
    int deleteByPostIds(@Param("postIds") List<Long> postIds);
}