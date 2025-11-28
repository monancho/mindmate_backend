package com.mind_mate.home.dto;

import java.time.LocalDate;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIDTo {

	private String status;

	private Long boardId;
	
	private Long diaryId;
	
	private Long userId;
	
	private String content;
	
	private LocalDate date;
	
}
