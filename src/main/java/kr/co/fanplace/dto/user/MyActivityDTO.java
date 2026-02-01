package kr.co.fanplace.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class MyActivityDTO
{
    // 글
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostItem
    {
        private Long postId;

        private String boardId;
        private String boardName;

        private String categoryId;
        private String categoryName;

        // PostLog 최신 기준 제목
        private String title;

        // Post 생성일 (UI의 "작성일시")
        private LocalDateTime createdAt;
    }

    // 댓글
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentItem
    {
        private String type;
        private Long id;

        private Long postId;

        private String boardId;
        private String boardName;

        private String categoryId;
        private String categoryName;

        private String content;
        private LocalDateTime createdAt;
    }
}