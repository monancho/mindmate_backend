package com.mind_mate.home.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import lombok.Setter;

@Entity
@Getter
@Setter

@AllArgsConstructor
@NoArgsConstructor
@Builder


public class Emoji {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)

	private Long id;
	
	private String type; //이모지 종류 (코드)
	private String imageUrl; // 이모지 이미지(url,static파일경로)
	
	private int count = 0;
	
	@ManyToOne(fetch = FetchType.LAZY) // 해당 이모지를 누른 유저 파악
	@JoinColumn(name = "user_id")
	private User user; //이모지 누른 유저 (Account → User 변경)
	
	@ManyToOne(fetch = FetchType.LAZY) // 어떤 게시물에 달린 이모지인지 파악
	@JoinColumn(name = "board_id")
	private Board board; //게시글에 달린 이모지일 경우 연결
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "comment_id")
	private Comment comment; //댓글에 달린 이모지일 경우 연결
	
	
	public void increaseCount() {
		this.count++; //해당 이모지의 전체 누적 수
	}
	
	public void decreaseCount() {
		if(this.count > 0)
			this.count--;
	}
	
	
	

}

