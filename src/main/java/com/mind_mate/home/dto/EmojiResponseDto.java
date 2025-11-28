package com.mind_mate.home.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmojiResponseDto {

	private String type;
	
	private boolean selected; // 유저가 누름 여부
	
	private String imageUrl;
	
	private int count;
	
	

	
}
