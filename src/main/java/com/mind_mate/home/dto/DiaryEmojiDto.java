package com.mind_mate.home.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEmojiDto {
	private LocalDate date;
    private Long emojiId;
    
}