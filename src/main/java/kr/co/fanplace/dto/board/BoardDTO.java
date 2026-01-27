package kr.co.fanplace.dto.board;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class BoardDTO
{
    // 게시판
    @Getter
    @AllArgsConstructor
    public static class Header
    {
        private final String boardId;
        private final String boardName;
        private final List<CategoryDTO.Tab> categories;
    }
}
