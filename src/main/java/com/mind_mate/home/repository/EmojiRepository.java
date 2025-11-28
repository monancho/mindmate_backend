package com.mind_mate.home.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


import com.mind_mate.home.entity.Board;
import com.mind_mate.home.entity.Comment;
import com.mind_mate.home.entity.Emoji;
import com.mind_mate.home.entity.User;

public interface EmojiRepository extends JpaRepository<Emoji, Long>{

	Optional<Emoji> findByBoardAndUserAndType(Board board, User user, String type);
	Optional<Emoji> findByCommentAndUserAndType(Comment comment, User user, String type);
	
	List<Emoji> findByBoard(Board board); // 특정 게시물 이모지
	List<Emoji> findByComment(Comment comment); // 특정 댓글 이모지
	
	List<Emoji> findByBoardId(Long boardId);
	
	// 게시글 기준 특정 이모지 카운트
	int countByBoardAndType(Board board, String type);
	// 댓글 기준 특정 이모지 카운트
	int countByCommentAndType(Comment comment, String type);
	
	//유저 한 명 당 한 게시글/댓글 이모지 확인용
	Optional<Emoji> findByBoardAndUser(Board board, User user);
    Optional<Emoji> findByCommentAndUser(Comment comment, User user);
}
