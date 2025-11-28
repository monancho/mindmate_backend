package com.mind_mate.home.repository;

import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mind_mate.home.entity.Diary;
import com.mind_mate.home.entity.User;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    // 단일 날짜 조회 (emojiList, airesult EAGER)
    @EntityGraph(attributePaths = {"emojiList", "airesult","user"})
    Optional<Diary> findByUserIdAndDate(Long userId, LocalDate date);

    // 날짜 범위 조회 (emojiList, airesult EAGER)
    @EntityGraph(attributePaths = {"emojiList", "airesult","user"})
    List<Diary> findByUserIdAndDateBetween(Long userId,LocalDate startDate, LocalDate endDate);

    // 특정 사용자 일기 조회
    List<Diary> findByUser(User user);
    
    void deleteAllByUser(User user);
//    // 특정 날짜 + 특정 사용자 일기 조회
//    Optional<Diary> findByDateAndUser(LocalDate date, User user);
}

