package com.mind_mate.home.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mind_mate.home.entity.User;



public interface UserRepository extends JpaRepository<User, Long>{
	
	Optional<User> findByNickname(String nickname);
	Optional<User> findByEmail(String email);
}
