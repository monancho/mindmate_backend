package com.mind_mate.home.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mind_mate.home.entity.AIResult;
import com.mind_mate.home.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;


public interface AIRepository extends JpaRepository<AIResult, Long> {
	
	Optional<AIResult> findByUserIdAndStatusAndStartdateAndEnddate(Long userId,String status, LocalDate start, LocalDate end);

	Optional<AIResult> findByUserIdAndCreatedBetween(Long userId, LocalDateTime start, LocalDateTime end);
	
	void deleteAllByUser(User user);
}
