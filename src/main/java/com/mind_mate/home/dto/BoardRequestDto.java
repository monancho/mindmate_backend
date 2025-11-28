package com.mind_mate.home.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardRequestDto {

	private Long userId;
	private String title;
	private String content;
	
}
