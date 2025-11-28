package com.mind_mate.home.dto;

import lombok.Data;

@Data
public class AICharacterDto {

	private Long userId;
	
	private String name;
	
	private Long id;
	
	private int moodscore;
	
	private int level;
	
	private int points;
	
	private Long todayEmojiId;
}
