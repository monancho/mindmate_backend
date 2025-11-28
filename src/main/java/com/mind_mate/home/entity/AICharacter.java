package com.mind_mate.home.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Data;

@Entity
@Data
public class AICharacter {

	@Id
	@GeneratedValue(strategy =  GenerationType.IDENTITY)
	private Long id;
	
	private String name;
	
	private int moodscore;
	
	private int level;
	
	private int points;
	
	private String lastAiResponse;
	
	@UpdateTimestamp
	private LocalDateTime lastupdate;
	
	private LocalDateTime lastCheer;
	
	@OneToOne
	@JoinColumn(name = "user_id")
	private User user;
}