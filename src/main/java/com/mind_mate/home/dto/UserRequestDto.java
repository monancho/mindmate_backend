package com.mind_mate.home.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;


@Getter
public class UserRequestDto {
	@NotBlank(message = "닉네임을 입력해 주세여")
	@Size(min = 3, max = 20, message = "닉네임은 3~ 20글자로 입력할 수 있습니다.")
	@Pattern(
		    regexp = "^[a-zA-Z0-9가-힣]+$",
		    message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다."
		)
	private String nickname;
	
	@NotNull(message = "생일을 입력해 주세요")
	@PastOrPresent(message = "현재 시점 ,과거 날짜만 가능합니다.")
	private LocalDate birth_date;
	
	@NotNull(message = "Mbti를 선택해 주세요")
	private String mbti;
}
