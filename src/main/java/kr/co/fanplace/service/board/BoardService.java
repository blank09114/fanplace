package kr.co.fanplace.service.board;

import kr.co.fanplace.dto.board.BoardDTO;
import kr.co.fanplace.dto.board.CategoryDTO;
import kr.co.fanplace.dto.board.PostDTO;
import kr.co.fanplace.repository.board.BoardRepository;
import kr.co.fanplace.repository.board.CategoryRepository;
import kr.co.fanplace.repository.board.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final PostRepository postRepository;

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

    // 게시글 목록 조회
    @Transactional(readOnly = true)
    public Page<PostDTO.ListItem> getPostList(String boardId, String categoryId, boolean hot, int page)
    {
        // 게시판 존재 검증
        boardRepository.findById(boardId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시판입니다."));

        // 카테고리 검증
        if (categoryId != null)
        {
            var category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리입니다."));

            if (!boardId.equals(category.getBoard().getId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시판과 카테고리가 일치하지 않습니다.");
        }

        int safePage = Math.max(page, 0);
        PageRequest pageable = PageRequest.of(safePage, 10);

        return postRepository.findPostList(boardId, categoryId, hot, 10L, pageable);
    }

    // 게시글 검색
    @Transactional(readOnly = true)
    public Page<PostDTO.ListItem> searchPostList
    (String boardId, String categoryId, boolean hot, int page, String q)
    {
        boardRepository.findById(boardId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시판입니다."));

        if (q == null || q.trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어를 입력하세요.");

        if (categoryId != null)
        {
            var category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리입니다."));
            if (!boardId.equals(category.getBoard().getId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시판과 카테고리가 일치하지 않습니다.");
        }

        int safePage = Math.max(page, 0);
        PageRequest pageable = PageRequest.of(safePage, 10);

        return postRepository.searchPostListByTitle(boardId, categoryId, hot, 10L, q.trim(), pageable);
    }

    // 통합 검색
    @Transactional(readOnly = true)
    public Page<PostDTO.UnivListItem> searchUnivPostList(int page, String q)
    {
        if (q == null || q.trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어를 입력하세요.");

        int safePage = Math.max(page, 0);
        PageRequest pageable = PageRequest.of(safePage, 10);

        return postRepository.searchUnivByTitle(q.trim(), pageable);
    }
}