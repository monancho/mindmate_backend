package com.mind_mate.home.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
	
	@JsonProperty("userId")
	Long userId;
	@JsonProperty("nickname")
	String nickname;
	@JsonProperty("birth_date")                    // 스네이크 케이스 유지 시
    @JsonFormat(pattern = "yyyy-MM-dd") 
	LocalDate birth_date;
	@JsonProperty("mbti")
	String mbti;

	@JsonProperty("authType")
	String authType;
	
	
	public UserResponseDto(Long userId) {
	    this.userId = userId;
	}
	
	@JsonProperty("role") // ✅ 추가
    private String role;
	
	
	@JsonProperty("profile_image_url")
	private String profileImageUrl;
	
	@JsonProperty("email")
	private String email;
	
}
