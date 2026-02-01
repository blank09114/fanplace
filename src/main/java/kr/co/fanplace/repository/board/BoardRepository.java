package kr.co.fanplace.repository.board;

import java.util.Optional;

import kr.co.fanplace.entity.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, String> { }