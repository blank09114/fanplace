package kr.co.fanplace.dto.board;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class CategoryDTO
{
    // 카테고리
    @Getter
    @AllArgsConstructor
    public static class Tab
    {
        private final String categoryId;
        private final String categoryName;
    }
}
