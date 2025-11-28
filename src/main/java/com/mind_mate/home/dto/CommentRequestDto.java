package com.mind_mate.home.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequestDto {
    private Long boardId;     // 댓글이 달릴 게시글 ID
    private Long userId;   // 작성자 ID
    private String content;   // 댓글 내용
}
