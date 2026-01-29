package kr.co.fanplace.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class PostDTO
{
    // 게시글 목록
    @Getter
    @AllArgsConstructor
    public static class ListItem
    {
        private final Long postId;

        private final String categoryId;
        private final String categoryName;

        // 작성자 표시
        private String authorUserId;
        private final String authorName;

        private final LocalDateTime createdAt;

        // 최신 로그 기준
        private final String title;

        // 수치
        private final long viewCount;
        private final long likeCount;
        private final long commentCount;
    }

    // 게시글 상세 정보
    @Getter
    @AllArgsConstructor
    public static class DetailView
    {
        private final Long postId;

        private final String boardId;
        private final String boardName;

        private final String categoryId;
        private final String categoryName;

        private final String authorUserId;
        private final String authorName;

        private final LocalDateTime createdAt;

        private final String ip;
        private final String region;

        private final boolean deleted;
        private final String deletedReasonDisplay;
        private final LocalDateTime deletedAt;

        // 최신 로그 기준
        private final String title;
        private final String content;
        private final LocalDateTime updatedAt;

        // 수치
        private final long viewCount;
        private final long likeCount;
        private final long commentCount;
    }

    // 핸들러
    @Getter
    @AllArgsConstructor
    public static class PostPage
    {
        private final DetailView post;

        // 이전/다음은 ID만
        private final Long prevPostId;
        private final Long nextPostId;

        // 뷰 표시 제어용
        private final boolean login;
        private final boolean canLike;
        private final boolean canEdit;
        private final boolean canDelete;
        private final boolean canAdminDelete;
        private final boolean canChangeDeletedReason;
    }

    // 좋아요
    @Getter
    @AllArgsConstructor
    public static class LikeRes
    {
        private final boolean liked;
        private final long likeCount;
    }

    // 게시글 작성
    @Getter @Setter
    public static class CreateForm
    {
        @NotBlank(message = "카테고리를 선택하세요.")
        private String categoryId;

        @NotBlank(message = "제목을 입력하세요.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        private String title;

        @NotBlank(message = "내용을 입력하세요.")
        private String content;
    }

    // 통합 검색 결과
    @Getter
    @AllArgsConstructor
    public static class UnivListItem
    {
        private final Long postId;

        private final String boardId;
        private final String boardName;

        private final String authorName;
        private final LocalDateTime createdAt;
        private final String title;

        private final long viewCount;
        private final long likeCount;
        private final long commentCount;
    }
}