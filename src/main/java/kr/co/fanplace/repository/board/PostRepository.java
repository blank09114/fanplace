package kr.co.fanplace.repository.board;

import kr.co.fanplace.entity.board.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> { }