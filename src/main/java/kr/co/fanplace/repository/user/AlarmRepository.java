package kr.co.fanplace.repository.user;

import kr.co.fanplace.entity.user.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlarmRepository extends JpaRepository<Alarm, Long>
{
    @Modifying
    @Query("delete from Alarm a where a.comment.id in :commentIds")
    int deleteByCommentIds(@Param("commentIds") List<Long> commentIds);

    @Modifying
    @Query("delete from Alarm a where a.recomment.id in :recommentIds")
    int deleteByRecommentIds(@Param("recommentIds") List<Long> recommentIds);
}