package com.mind_mate.home.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Data;

@Data
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AIResult {
	
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id; // PK
   
   @Column(columnDefinition = "TEXT")
   private String aicomment; // 일기 코멘트
   
   private String status; // diary, fortune, weekly
   
   @CreationTimestamp
   private LocalDateTime created; // 생성일
   
   private LocalDate startdate;
   
   private LocalDate enddate;
   
   @OneToOne
   @JoinColumn(name = "diary_id")
   @JsonBackReference
   private Diary diary;
   
   
   @OneToOne
   @JoinColumn(name = "board_id")
   @JsonBackReference
   private Board board;
   
   @ManyToOne
   @JoinColumn(name = "user_id")
   @JsonBackReference
   private User user;
   
   @ManyToOne
   private AICharacter aiCharacter;
}
