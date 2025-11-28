package com.mind_mate.home.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CommentResponseDto {
    private Long id;
    private String writer; // 작성자 이름 (username)
    private Long writerId; // user.id
    private String writerRole;
    private String content;
    private LocalDate createdate;
    private LocalDate updatedAt;
    private List<EmojiResponseDto> emojis; 
}
