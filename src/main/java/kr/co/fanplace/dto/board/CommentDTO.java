package kr.co.fanplace.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class CommentDTO
{
    // 부모 댓글(페이지 단위)
    @Getter
    @AllArgsConstructor
    public static class Item
    {
        private final Long commentId;

        private final boolean deleted;
        private final String deletedReasonDisplay;
        private final LocalDateTime deletedAt;

        private String authorUserId;
        private final String authorName;
        private final LocalDateTime createdAt;

        private final String content;

        private final boolean mine;

        @Setter
        private List<RecommentItem> recomments;
    }

    // 대댓글(부모 댓글에 종속)
    @Getter
    @AllArgsConstructor
    public static class RecommentItem
    {
        private final Long recommentId;
        private final Long commentId;

        private final boolean deleted;
        private final String deletedReasonDisplay;
        private final LocalDateTime deletedAt;

        private String authorUserId;
        private final String authorName;
        private final String mentionName;

        private final LocalDateTime createdAt;
        private final String content;
        private final boolean mine;
    }

    // 댓글 작성 요청
    @Getter
    public static class WriteReq
    {
        @NotBlank(message = "내용을 입력하세요.")
        @Size(min = 1, max = 500, message = "내용은 500자 이내로 입력하세요.")
        private String content;
    }

    // 댓글 작성 응답
    @Getter
    @AllArgsConstructor
    public static class WriteRes { private final Long commentId; }

    // 대댓글 작성 요청
    @Getter
    public static class RecommentWriteReq
    {
        @NotBlank(message = "내용을 입력하세요.")
        @Size(min = 1, max = 500, message = "내용은 500자 이내로 입력하세요.")
        private String content;
    }

    // 대댓글 작성 응답
    @Getter
    @AllArgsConstructor
    public static class RecommentWriteRes { private final Long recommentId; }

    // 삭제된 댓글/대댓글 목록
    @Getter
    @AllArgsConstructor
    public static class DeletedListItem
    {
        private final String type;
        private final Long id;

        private final Long postId;
        private final String boardId;
        private final String boardName;
        private final String categoryId;
        private final String categoryName;

        private final String authorUserId;
        private final String authorName;

        private final String content;
        private final LocalDateTime createdAt;

        private final LocalDateTime deletedAt;
        private final String deletedReason;
    }
}