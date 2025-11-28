package com.mind_mate.home.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mind_mate.home.entity.Board;

import java.time.LocalDate;
import java.time.LocalDateTime;


public interface BoardRepository extends JpaRepository<Board, Long> {

	// 제목 검색
    Page<Board> findByTitleContaining(String keyword, Pageable pageable);

    // 제목 또는 내용 검색
    Page<Board> findByTitleContainingOrContentContaining(
    		String titleKeyword, 
    		String contentKeyword, 
    		Pageable pageable);
	
    Page<Board> findByUserId(Long userId, Pageable pageable);
    
    Page<Board> findByContentContaining(String contentKeyword, Pageable pageable);

    Page<Board> findByUser_NicknameContaining(String nickname, Pageable pageable);

    // 전체 게시글 조회 (관리자 글 상단고정 + 최신순)
    Page<Board> findAllByOrderByPinnedDescCreatedAtDesc(Pageable pageable);

    // 제목 검색
    Page<Board> findByTitleContainingOrderByPinnedDescCreatedAtDesc(String keyword, Pageable pageable);

    // 내용 검색
    Page<Board> findByContentContainingOrderByPinnedDescCreatedAtDesc(String keyword, Pageable pageable);

    // 작성자 닉네임 검색
    Page<Board> findByUser_NicknameContainingOrderByPinnedDescCreatedAtDesc(String nickname, Pageable pageable);

    // 제목 OR 내용 검색
    Page<Board> findByTitleContainingOrContentContainingOrderByPinnedDescCreatedAtDesc(
        String titleKeyword, 
        String contentKeyword, 
        Pageable pageable
    );

    // 특정 유저 글 조회 (내글보기용)
    Page<Board> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
 // 문자열에 특정 해시태그 포함 게시글 검색
    @Query("SELECT b FROM Board b WHERE LOWER(b.hashtags) LIKE LOWER(CONCAT('%', :tag, '%')) ORDER BY b.createdAt DESC")
    List<Board> findByHashtagIgnoreCase(@Param("tag") String tag);
    
    Page<Board> findByCreatedAtAfter(LocalDateTime startDate, Pageable pageable);
}
