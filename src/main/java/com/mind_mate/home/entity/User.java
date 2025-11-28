package com.mind_mate.home.entity;


import java.time.LocalDate;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class User {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(unique = true, nullable = false)
	private String email;
	
	@Column(unique = true)
	private String nickname;
	private LocalDate birth_date;
	private String mbti;
	
	@Column(nullable = false)
	private String authType;

	@OneToOne(mappedBy ="user", cascade = CascadeType.ALL)
	private Account account;
	
	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
	private Social social;

	@Column(nullable = false)
	private String role = "USER";
	
	@Column(name = "profile_image_url")
	private String profileImageUrl; 
	

//	@Column(name = "profile_image_key")
//	private String profileImageKey; 
}
