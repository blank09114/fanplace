package kr.co.fanplace.repository.board;

import kr.co.fanplace.entity.board.post.PostLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLogRepository extends JpaRepository<PostLog, Long> { }