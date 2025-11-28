package com.mind_mate.home.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mind_mate.home.dto.AIDTo;
import com.mind_mate.home.dto.UserRequestDto;
import com.mind_mate.home.entity.AIResult;
import com.mind_mate.home.service.AIService;
import com.mind_mate.home.service.UserService;
import com.mind_mate.home.util.jwt.ExceptionUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {


    private final AIService aiService;



//    UserController(AIService AIService) {
//        this.aiService = AIService;
//    }

	
	private final UserService userService;
	private final ExceptionUtil exceptionUtil;
	@PostMapping
	public ResponseEntity<?> setProfile(@RequestHeader("Authorization") String header, @Valid @RequestBody UserRequestDto body, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			return exceptionUtil.checkValid(bindingResult); // 유효성 체크
		}
		
		return userService.setProfile(header, body);
	}
	
	@PutMapping
	public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String header, @Valid @RequestBody UserRequestDto body, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			return exceptionUtil.checkValid(bindingResult); // 유효성 체크
		}
		return userService.setProfile(header, body);
	}
	
	@DeleteMapping
	public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String header) {
		return userService.deleteUser(header);
	}

	@GetMapping("/check_nickname")
	public ResponseEntity<?> checkUsername(@RequestParam("nickname") String nickname) {
        return userService.checkNickname(nickname);
    }

	@PostMapping("/test")
	public ResponseEntity<?> generateAITest(@RequestHeader("Authorization") String header, @RequestBody AIDTo aiDto) {
		AIResult aiResult = userService.getDailyTest(header,aiDto.getContent());
		return ResponseEntity.ok(Map.of("aicomment", aiResult.getAicomment()));
	}
	@PostMapping("/result")
	public ResponseEntity<?> generateAIResult(@RequestHeader("Authorization") String header, @RequestBody AIDTo aiDto) {
		AIResult aiResult = userService.getDailyResult(header,aiDto.getContent());
		return ResponseEntity.ok(Map.of("aicomment", aiResult.getAicomment()));	
		}
	@PostMapping("/fortune")
	public ResponseEntity<?> generateAIFortune(@RequestHeader("Authorization") String header, @RequestBody AIDTo aiDto) {
		AIResult aiResult = userService.getFortune(header,aiDto.getContent());
		return ResponseEntity.ok(Map.of("aicomment", aiResult.getAicomment()));	
		}
	@GetMapping("/result")
	public ResponseEntity<?> getTodayResult (@RequestHeader("Authorization") String header,
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		AIResult aiResult = aiService.getResult(header, date);
		if (aiResult == null) {
		    Map<String, Object> map = new HashMap<>();
		    map.put("aicomment", null);
		    return ResponseEntity.ok(map);
		}
		return ResponseEntity.ok(Map.of(
            "aicomment", aiResult.getAicomment()
        ));
		
	}
	 @PostMapping("/profile-image")
	    public ResponseEntity<?> uploadProfileImage(
	            @RequestHeader("Authorization") String header,
	            @RequestPart("file") MultipartFile file
	    ) {
	        return userService.uploadProfileImage(header, file);
	    }

	    /** 프로필 이미지 삭제 */
	    @PostMapping("/profile-image/delete")
	    public ResponseEntity<?> deleteProfileImage(
	            @RequestHeader("Authorization") String header
	    ) {
	        return userService.deleteProfileImage(header);
	    }
	
}
