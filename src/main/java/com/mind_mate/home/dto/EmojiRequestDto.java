package com.mind_mate.home.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmojiRequestDto {

	private Long userId;
	
	private Long boardId; //게시글
	
	private Long commentId; // 댓글
	
	private String type; // 이모지 종류(코드)
	
	private String imageUrl; // 이미지경로
	
}
