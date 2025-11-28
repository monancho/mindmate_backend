package com.mind_mate.home.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mind_mate.home.entity.AICharacter;
import com.mind_mate.home.entity.User;

import java.util.List;


public interface AICharacterRepository extends JpaRepository<AICharacter, Long> {
	
	Optional<AICharacter> findByUser_Id(Long userId);
	void deleteByUser(User user);
}
