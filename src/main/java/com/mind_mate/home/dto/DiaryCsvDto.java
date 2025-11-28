package com.mind_mate.home.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaryCsvDto {
	private LocalDate date;      // 날짜
    private String nickname;     // 작성자
    private String title;        // 제목
    private String content;      // 내용
    private String emojiType;    // 감정 타입
    private String aiComment;    // AI 코멘트
}
