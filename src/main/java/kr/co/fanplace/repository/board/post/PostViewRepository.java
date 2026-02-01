package kr.co.fanplace.repository.board.post;

import kr.co.fanplace.entity.board.post.PostView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostViewRepository extends JpaRepository<PostView, Long>
{
    // 조회 기록 존재 여부 확인
    boolean existsByPost_IdAndUser_Id(Long postId, String userId);
    boolean existsByPost_IdAndUserIp(Long postId, String userIp);

    // 삭제
    @Modifying
    @Query("delete from PostView pv where pv.post.id in :postIds")
    int deleteByPostIds(@Param("postIds") List<Long> postIds);

    // 조회수 집계
    long countByPost_Id(Long postId);
}
