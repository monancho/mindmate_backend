package com.mind_mate.home.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mind_mate.home.entity.Board;
import com.mind_mate.home.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long>{

	List<Comment> findByBoardOrderByCreatedateAsc(Board board);
	
	List<Comment> findByBoardId(Long boardId);
	
}
