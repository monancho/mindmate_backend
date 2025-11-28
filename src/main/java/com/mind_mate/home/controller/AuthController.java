package com.mind_mate.home.controller;

import java.util.Map;
import java.util.Optional;

import com.mind_mate.home.service.SocialAuthService;
import com.mind_mate.home.util.jwt.ExceptionUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mind_mate.home.dto.AccountRequestDto;
import com.mind_mate.home.entity.User;
import com.mind_mate.home.service.AccountService;
import com.mind_mate.home.service.EmailService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {



	private final AccountService accountService;
	private final SocialAuthService socialAuthService;
	private final ExceptionUtil exceptionUtil;
	private final EmailService emailService;
	
	@PostMapping("/signup")
	public ResponseEntity<?> signup(
			@Valid @RequestBody AccountRequestDto  body, 
			BindingResult bindingResult, 
			HttpServletRequest request,
			HttpServletResponse response
			) {
		if(bindingResult.hasErrors()) {
			return exceptionUtil.checkValid(bindingResult); // 유효성 체크
		}
		
		
		String email = body.getEmail();
		String code = body.getCode();
		
		boolean isEmailCode = emailService.isEmailCode(email, code, request);
		
		if (!isEmailCode) {
			return ResponseEntity
	                .status(HttpStatus.UNPROCESSABLE_ENTITY)
	                .body("이메일 인증코드가 틀렸거나 만료되었습니다.");
		}
		return accountService.signup(body, response);
	}
	
//	@PostMapping("/email/code")
//	public ResponseEntity<?> sendEmailCode(@RequestParam("email") String email,
//            HttpServletRequest  request) {
//		return emailService.sendMailWithCode(email, request);
//}
	
	@PostMapping("/refresh")
	public ResponseEntity<?> checkToken(
			@RequestBody Map<String, String> body, 
			@CookieValue(value = "refreshToken", required = false) String refreshToken,
			HttpServletResponse response
			) {
//		System.out.println("생성 동작");
		String oldAccessToken = body.get("accessToken");
		return accountService.generationToken(oldAccessToken, refreshToken, response);
	}
	 @PostMapping("/logout")
	    public ResponseEntity<?> logout(@RequestHeader("Authorization") String header,
				HttpServletResponse response) {
	       return accountService.logout(header, response);
	    }
	

	 @PostMapping("/login")
	 public ResponseEntity<?> login(@RequestBody Map<String, String> body,
			 HttpServletResponse response) {
		 return accountService.login(body, response);
	 }
	 @GetMapping("/login/{type}")
	    public ResponseEntity<?> socialLogin(
	            @PathVariable("type") String type,
	            @RequestParam("code") String code,
	            @RequestParam(value = "state", required = false) String state,
	            HttpServletResponse response
	    ) {
	        String lowerType = type.toLowerCase();

	        switch (lowerType) {
	            case "kakao":
	                return socialAuthService.kakaoLogin(code, response);
	            case "naver":
	                if (state == null || state.isBlank()) {
	                    return ResponseEntity.badRequest().body("네이버 state 값이 없습니다.");
	                }
	                return socialAuthService.naverLogin(code, state, response);
	            case "google":
	                return socialAuthService.googleLogin(code, response);
	            default:
	                return ResponseEntity.badRequest().body("지원하지 않는 로그인 타입입니다: " + type);
	        }
	    }
	@PostMapping("/delete") //TODO : 삭제 본인 인증 절차 추가
	public ResponseEntity<?> deleteAccount(@RequestHeader("Authorization") String header) {
			return accountService.localDelete(header);
	}
	 
	 @PostMapping("/delete/{type}")
	 public ResponseEntity<?> socialDelete(
	         @RequestHeader("Authorization") String header,
	         @PathVariable("type") String type,
	         @RequestBody Map<String, String> body
	 ) {
	     String lowerType = type.toLowerCase();
	     switch (lowerType) {
	         case "kakao":
	             return socialAuthService.kakaoDelete(header, body);
	         case "naver":
	             return socialAuthService.naverDelete(header, body);
	         case "google":
	             return socialAuthService.googleDelete(header, body);
	         default:
	             return ResponseEntity.badRequest().body("지원하지 않는 소셜 타입입니다: " + type);
	     }
	 }
	
	@GetMapping("/me") 
	public ResponseEntity<?> me (@RequestHeader("Authorization") String header) {
		return accountService.getUser(header);
		
	}
	@GetMapping("/check_username")
	public ResponseEntity<?> checkUsername(@RequestParam("username") String username, HttpServletRequest request) {
		if (username == null || username.isBlank()) {
		    return ResponseEntity
		        .status(HttpStatus.BAD_REQUEST)
		        .body("이메일을 입력해 주세요.");
		}
		
        Optional<User> _user = accountService.checkUsername(username);
        if (_user.isPresent()) {
        	 return new ResponseEntity<>("이미 사용중인 이메일입니다", HttpStatus.CONFLICT);
        }
        
        return emailService.sendMailWithCode(username, request);
        
    }
	
	@PostMapping("/check_code") // 이메일 코드 유효성 체크 (코드확인 버튼용)
	public ResponseEntity<?> checkCode(@RequestBody Map<String, String> body, HttpServletRequest request) {
		String email = body.get("email");
	    String code  = body.get("code");
	    
	    if (email == null || email.isBlank() || code == null || code.isBlank()) {
	        return ResponseEntity
	                .status(HttpStatus.BAD_REQUEST)
	                .body("이메일과 코드를 모두 입력해 주세요.");
	    }
	    boolean isValid = emailService.checkEmailCodeOnly(email, code, request); // 여기서는 코드만 확인
	    if (!isValid) {
	        return ResponseEntity
	                .status(HttpStatus.UNPROCESSABLE_ENTITY)
	                .body("인증코드가 올바르지 않거나 만료되었습니다.");
	    }

	    return ResponseEntity.ok("인증코드가 유효합니다.");
	    }

	
	
	
}
