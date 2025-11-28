package com.mind_mate.home.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mind_mate.home.dto.AICharacterDto;
import com.mind_mate.home.entity.AICharacter;
import com.mind_mate.home.entity.User;
import com.mind_mate.home.service.AICharacterService;
import com.mind_mate.home.service.AIService;
import com.mind_mate.home.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequiredArgsConstructor
public class AICharacterController {


	private final AICharacterService aiCharacterService;
	
//	private final ProfileService profileService;
	
	private final UserService userService;
	
	private final AIService aiService;

	
	@PostMapping("/ai/create")
	public ResponseEntity<?> createCharacter(@RequestHeader("Authorization") String header ,@RequestBody AICharacterDto aiCharacterDto) {
		try {
	        AICharacter aiCharacter = aiCharacterService.createCharacter(header, aiCharacterDto.getName());
	        return ResponseEntity.ok(aiCharacterService.toDto(aiCharacter));
	    } catch (RuntimeException e) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                             .body(Map.of("message", e.getMessage()));
	    }
	}
	@GetMapping("/ai/me")
	public ResponseEntity<?> getCharacter(@RequestHeader("Authorization") String header) {
		Optional<AICharacter> aiCharacterOpt = aiCharacterService.getCharacter(header);
		 if (aiCharacterOpt.isEmpty()) {
		        return ResponseEntity.status(HttpStatus.NOT_FOUND)
		                .body(Map.of("message", "캐릭터가 없습니다."));
		    }
		    return ResponseEntity.ok(aiCharacterService.toDto(aiCharacterOpt.get()));
		
	}
	@PutMapping("/ai/cheer")
	public ResponseEntity<?> cheerCharacter(@RequestHeader("Authorization") String header,
			@RequestParam(name = "addPoints", required = true) int addPoints, 
			@RequestParam(name = "moodChange", required = true) int moodChange) {
		try { 
				AICharacter updated = aiCharacterService.cheerCharacter(header, addPoints, moodChange); 

				return ResponseEntity.ok(aiCharacterService.toDto(updated)); 
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("message", e.getMessage())); 
		}
	}
	@PutMapping("/ai/update")
	public ResponseEntity<?> updateCharacter(@RequestHeader("Authorization") String header,
			@RequestParam("addPoints") int addPoints, 
			@RequestParam("moodChange") int moodChange) {
		Optional<AICharacter> aiCharacterOpt = aiCharacterService.getCharacter(header);
	    if (aiCharacterOpt.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(Map.of("message", "캐릭터가 없습니다."));
	    }

	    AICharacter aiCharacter = aiCharacterOpt.get();
	    AICharacter updated = aiCharacterService.updateAiCharacter(aiCharacter, addPoints, moodChange); 

	    return ResponseEntity.ok(aiCharacterService.toDto(updated));
	}
	@PostMapping("/ai/chat")
	public ResponseEntity<?> chatWithAI(@RequestHeader("Authorization") String header, @RequestBody Map<String, String> body) {
		String userMessage = body.get("message");
		if (userMessage == null || userMessage.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "메시지를 입력해주세요."));
        }
		 Optional<AICharacter> aiCharacterOpt = aiCharacterService.getCharacter(header);
		    if (aiCharacterOpt.isEmpty()) {
		        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "캐릭터가 없습니다."));
		    }
		    AICharacter aiCharacter = aiCharacterOpt.get();
		    
		    User user = userService.getProfile(header);
		    
		    String aiResponse = aiService.generateResponse(aiCharacter, userMessage, user.getNickname());

		    // 캐릭터 엔티티에 마지막 AI 응답 저장
		    aiCharacter.setLastAiResponse(aiResponse);
		    aiCharacterService.updateAiCharacter(aiCharacter, 2, 0); // 점수 변화 없으므로 0,0
		     
		    Map<String, Object> response = Map.of(
		        "character", aiCharacterService.toDto(aiCharacter),
		        "aiResponse", aiResponse
		    );

		    return ResponseEntity.ok(response);
	}
	@PutMapping("/ai/setName")
	public ResponseEntity<?> updateNameCharacter(@RequestHeader("Authorization") String header,
			@RequestParam("name") String name) {
		Optional<AICharacter> aiCharacterOpt = aiCharacterService.getCharacter(header);
	    if (aiCharacterOpt.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(Map.of("message", "캐릭터가 없습니다."));
	    }

	    AICharacter updated = aiCharacterService.setCharacterName(header, name); 

	    return ResponseEntity.ok(aiCharacterService.toDto(updated));
	}
}
