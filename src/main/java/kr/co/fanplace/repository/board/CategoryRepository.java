package kr.co.fanplace.repository.board;


import kr.co.fanplace.entity.board.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, String> {
    // 탭 목록 조회
    List<Category> findByBoard_IdOrderByNameAsc(String boardId);
}