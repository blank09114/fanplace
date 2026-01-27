package kr.co.fanplace.service.board;

import kr.co.fanplace.dto.board.BoardDTO;
import kr.co.fanplace.dto.board.CategoryDTO;
import kr.co.fanplace.repository.board.BoardRepository;
import kr.co.fanplace.repository.board.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService
{
    private final BoardRepository boardRepository;
    private final CategoryRepository categoryRepository;

    // 게시판 정보 읽기
    @Transactional(readOnly = true)
    public BoardDTO.Header getBoardHeader(String boardId)
    {
        var board = boardRepository.findById(boardId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시판입니다."));

        List<CategoryDTO.Tab> tabs = categoryRepository
        .findByBoard_IdOrderByNameAsc(boardId)
        .stream()
        .map(c -> new CategoryDTO.Tab(c.getId(), c.getName()))
        .toList();

        return new BoardDTO.Header(board.getId(), board.getName(), tabs);
    }
}