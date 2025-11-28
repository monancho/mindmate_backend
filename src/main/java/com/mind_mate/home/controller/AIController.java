package com.mind_mate.home.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.mind_mate.home.dto.AIDTo;
import com.mind_mate.home.entity.AIResult;
import com.mind_mate.home.entity.Board;
import com.mind_mate.home.entity.Diary;
import com.mind_mate.home.repository.BoardRepository;
import com.mind_mate.home.repository.DiaryRepository;
import com.mind_mate.home.service.AIService;


@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class AIController {
	
	@Autowired
	AIService aiService;
	
	@Autowired
	DiaryRepository diaryRepository;
	
	@Autowired
	BoardRepository boardRepository;
	

//	@PostMapping("/ai/comment")
//	public AIResult generateAIComment(@RequestBody AIDTo aiDto ) {
//		String status = "diary";
//		Diary diary = diaryRepository.findById(aiDto.getDiaryId()).orElseThrow(() -> new RuntimeException("Diary Not Found"));
//		String content = diary.getContent();
		
//		return aiService.generatResult(status, content);
//	}
//	@PostMapping("/ai/tags")
//	public AIResult generateAITags(@RequestBody AIDTo aiDto ) {
//		String status = "tags";
//		Board board = boardRepository.findById(aiDto.getBoardId()).orElseThrow(() -> new RuntimeException("Board Not Found"));
//		String content =board.getContent();
//	
//		return aiService.generatResult(status, content);
//	}
//	@PostMapping("/ai/test")
//	public AIResult generateAITest(@RequestBody AIDTo aiDto ) {
//		String status = "daily_test";
//		String content =aiDto.getContent();
//	
//		return aiService.generatResult(status, content);
//	}
//	@PostMapping("/ai/result")
//	public AIResult generateAIResult(@RequestBody AIDTo aiDto ) {
//		String status = "daily_result";
//		String content =aiDto.getContent();
//	
//		return aiService.generatResult(status, content);
//	}
//	@GetMapping("/ai/result")
//	public ResponseEntity<?> generateResult(@RequestHeader("Authorization") String header , @RequestBody AIDTo aiDto) {
//		AIResult todayResult = aiService.getResult(header);
//		if (todayResult == null) {
//            return ResponseEntity.ok().body("오늘 결과 없음");
//        }
//	}
//	@PostMapping("/ai/fortune")
//	public AIResult generateAIFortune(@RequestBody AIDTo aiDto ) {
//		String status = "fortune";
//		String content = aiDto.getContent();
//		
//		return aiService.generatResult(status, content);
//	}
//	@PostMapping("/ai/weekly")
//	public AIResult generateAIWeekly(@RequestBody AIDTo aiDto ) {
//		String status = "weekly";
//		String content = aiDto.getContent();
//		
//		return aiService.generatResult(status, content);
//	}
//
//	@PostMapping("/ai/tags/test")
//	public AIResult generateAIWeekly(@RequestBody Map<String, String> request ) {
//		String status = "tags";
//		String content = request.get("content");
//		
//		return aiService.generatResult(status, content);
//	}
}

