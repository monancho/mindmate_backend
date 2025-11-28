package com.mind_mate.home.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mind_mate.home.entity.Board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponseDto {

	private Long id;
	private String title;
	private String content;
	private String writer; // 작성자 명 (user.nickname)
	private Long writerId; // user.id
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDateTime createdAt;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDateTime updatedAt;
	private Long viewCount;
	private List<CommentResponseDto> comments;
	private int commentCount;     
	private List<EmojiResponseDto> emojis;
	
	 private String hashtags;
	 
	 private boolean isPinned;
	 private String writerRole;
	
}
