package com.mind_mate.home.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mind_mate.home.entity.EmojiList;

public interface EmojiListRepository extends JpaRepository<EmojiList, Long>{
	Optional<EmojiList> findByType(String type);
}