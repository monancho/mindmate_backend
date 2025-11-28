package com.mind_mate.home.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEmotionDto {

	private LocalDate date;
	
	private String emojiName;
	
	private int count;
}
