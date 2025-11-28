package com.mind_mate.home.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mind_mate.home.dto.EmojiRequestDto;
import com.mind_mate.home.dto.EmojiResponseDto;
import com.mind_mate.home.entity.*;
import com.mind_mate.home.repository.*;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final EmojiService emojiService;
    private final AIService aiService;

    public Page<Board> getBoards(Pageable pageable, String field, String keyword) {
    	 if (keyword == null || keyword.trim().isEmpty()) {
             return boardRepository.findAllByOrderByPinnedDescCreatedAtDesc(pageable);
         }

         switch (field) {
             case "title":
                 return boardRepository.findByTitleContainingOrderByPinnedDescCreatedAtDesc(keyword, pageable);
             case "content":
                 return boardRepository.findByContentContainingOrderByPinnedDescCreatedAtDesc(keyword, pageable);
             case "writer":
                 return boardRepository.findByUser_NicknameContainingOrderByPinnedDescCreatedAtDesc(keyword, pageable);
             default:
                 return boardRepository.findByTitleContainingOrContentContainingOrderByPinnedDescCreatedAtDesc(keyword, keyword, pageable);
         }
    }

    @Transactional
    public Board getBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        board.setViewCount(board.getViewCount() + 1); // 조회수 증가
        return boardRepository.save(board);
    }

    @Transactional
    public Board save(Board board, Long userId) {
//        
	    	 User user = userRepository.findById(userId)
	                 .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
	         board.setUser(user);
	         
	         board.setPinned("ADMIN".equals(user.getRole()));
	         
	         if (board.getViewCount() == null) {
	             board.setViewCount(0L);
	         }


            Board saved = boardRepository.save(board);

            // AI 해시태그
            if(!"ADMIN".equals(user.getRole())) {
            try {
                AIResult aiResult = aiService.generatResult("tags", board.getContent());
                saved.setHashtags(aiResult.getAicomment());
                saved.linkAIResult(aiResult);
                boardRepository.save(saved);
            } catch (Exception e) {
                System.err.println("AI 해시태그 생성 실패: " + e.getMessage());
            }
            }

            return saved;
        }
    

    @Transactional
    public Board update(Long id, Board newBoardData, Long userId) {
        Board existing = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 안전하게 관리자와 작성자 체크
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isWriter = existing.getUser() != null && existing.getUser().getId().equals(userId);
        
        if (!isAdmin && !isWriter) {
            throw new IllegalArgumentException("작성자 또는 관리자만 수정 가능합니다.");
        }
        existing.setTitle(newBoardData.getTitle());
        existing.setContent(newBoardData.getContent());
        existing.setUpdatedAt(LocalDateTime.now());
        
        if (!isAdmin) {
        	existing.setPinned(false);
        try {
        	if(!"ADMIN".equals(user.getRole())) {
            AIResult aiResult = aiService.generatResult("tags", newBoardData.getContent());
            existing.setHashtags(aiResult.getAicomment());
            existing.linkAIResult(aiResult);
        	}
        } catch (Exception e) {
            System.err.println("AI 해시태그 재생성 실패: " + e.getMessage());
        }
        }
        return boardRepository.save(existing);
    }

    @Transactional
    public void delete(Long boardid, Long userId) {
        Board board = boardRepository.findById(boardid)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isWriter = board.getUser() != null && board.getUser().getId().equals(userId);
        
        if (!isAdmin && !isWriter) {
            throw new IllegalArgumentException("작성자 또는 관리자만 삭제 가능합니다.");
        }
        
        boardRepository.delete(board);
    }

    @Transactional
    public EmojiResponseDto toggleEmoji(EmojiRequestDto request, Long userId) {
        return emojiService.toggleEmoji(request, userId);
    }
    //내글모아보기
    @Transactional(readOnly = true)
    public Page<Board> getBoardsByUser(Long userId, Pageable pageable) {
        return boardRepository.findByUserId(userId, pageable);
    }
    @Transactional
    public List<String> getHashtags(List<Board> boards, int days, int topN) {
    	 //int topN = 5; // 상위 5개 해시태그
    	LocalDateTime startDate = LocalDate.now().minusDays(days).atStartOfDay();
    	List<Board> recent = boardRepository.findByCreatedAtAfter(startDate, Pageable.unpaged()).getContent();
    	
    	Map<String, Long> countMap = recent.stream()
    		    .filter(b -> b.getHashtags() != null)
    		    .flatMap((Board b) -> Arrays.stream(b.getHashtags().split("\\s+")))
    		    .filter(t -> t.startsWith("#"))
    		    .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        
        // 사용량 기준 내림차순 정렬 후 상위 N개
        return countMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
