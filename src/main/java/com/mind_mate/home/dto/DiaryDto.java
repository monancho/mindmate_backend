package com.mind_mate.home.dto;

import java.time.LocalDate;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class DiaryDto {

	@NotBlank(message = "제목을 입력해 주세요")
	@Size(min = 5, message = "글 제목은 최소 5글자 이상이여야합니다.")
	private String title;
	
	@NotBlank(message = "내용을 입력해 주세요")
	@Size(min = 5, message = "글 내용은 최소 5글자 이상이여야합니다.")
	private String content;
	private String username;
	private String nickname;
	private Long userId;
	
	@PastOrPresent(message = "현재 시점 ,과거 날짜만 가능합니다.")
    private LocalDate date;
	
    private EmojiDto emoji; // Emoji 클래스 필요
    
    private UserResponseDto userResponseDto;
    private boolean deleteImage;
    public boolean isDeleteImage() { return deleteImage; }
    public void setDeleteImage(boolean deleteImage) { this.deleteImage = deleteImage; }
    private String aiComment;
    
    // 새 필드 추가
    private String imageUrl;

}