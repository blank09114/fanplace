package kr.co.fanplace.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class PostDTO
{
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
}