package com.mind_mate.home.service;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.mind_mate.home.dto.AccountRequestDto;
import com.mind_mate.home.dto.UserResponseDto;
import com.mind_mate.home.entity.Account;
import com.mind_mate.home.entity.RefreshToken;
import com.mind_mate.home.entity.Social;
import com.mind_mate.home.entity.User;
import com.mind_mate.home.repository.AccountRepository;
import com.mind_mate.home.repository.RefreshTokenRepository;
import com.mind_mate.home.repository.SocialRepository;
import com.mind_mate.home.repository.UserRepository;
import com.mind_mate.home.util.jwt.JwtUtil;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService  {


	private final UserRepository userRepository;
	private final UserService userService;
    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;


 
	
	
	

	public ResponseEntity<?> signup (AccountRequestDto body, HttpServletResponse response) {
		
		String email = body.getEmail();
		String password = body.getPassword();
		
		 if (email == null || email.isBlank() || password == null || password.isBlank()) {
		        return ResponseEntity
		                .status(HttpStatus.BAD_REQUEST)
		                .body("이메일과 비밀번호를 모두 입력해 주세요.");
		    }
		
		if (userRepository.findByEmail(email).isPresent()) {
		    return ResponseEntity
		            .status(HttpStatus.CONFLICT) 
		            .body("이미 이 이메일로 가입된 계정이 있습니다.");
		}
		
		User user = new User();
		user.setEmail(email);
		user.setAuthType("LOCAL");

		Account account = new Account();
		account.setUsername(email);
		account.setPassword(passwordEncoder.encode(password));
		account.setUser(user);
		
		user.setAccount(account);
		userRepository.save(user);
		


		
	
		Long userId = user.getId();
		String accessToken = jwtUtil.createAccessToken(userId);
		String refreshToken = jwtUtil.createRefreshToken();
		
		try {
			refreshTokenRepository.save(new RefreshToken(userId, refreshToken, accessToken));
		} catch (Exception e) {
		}
		
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
	            .httpOnly(true)
	            .secure(false)     // 로컬(http) 개발이라면 false, https 배포 시 true
	            .sameSite("Lax")   // localhost:3000 ↔ 8888 은 same-site라 Lax로 충분
	            .path("/")
	            .build();

	    response.addHeader("Set-Cookie", refreshCookie.toString());
	    
		Map<String, String> result = new HashMap<>();
		result.put("accessToken", accessToken);
//		result.put("refreshToken", refreshToken);
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	    
	    
	}
	
	public ResponseEntity<?> login (Map<String, String> body ,HttpServletResponse response) {
		
	
		
		String username = body.get("username");
		String password = body.get("password");
		
		try {
			
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
		} catch (BadCredentialsException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			        .body("아이디 또는 비밀번호가 올바르지 않습니다");
		}
		
		Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
		
		
		User user = account.getUser();
		Long userId = user.getId();
		
		String accessToken = jwtUtil.createAccessToken(userId);
		String refreshToken = jwtUtil.createRefreshToken();
		try {
			refreshTokenRepository.save(new RefreshToken(userId, refreshToken, accessToken));
		} catch (Exception e) {
		}
		
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
	            .httpOnly(true)
	            .secure(false)    // 배포 시 https + true로 변경
	            .sameSite("Lax")
	            .path("/")
	            .build();
		
		response.addHeader("Set-Cookie", refreshCookie.toString());
		
		Map<String, String> result = new HashMap<>();
		result.put("accessToken", accessToken);
//		result.put("refreshToken", refreshToken);
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	
	    public ResponseEntity<?> logout(String header, HttpServletResponse response) {
	        String accessToken = jwtUtil.findTokenByHeader(header);
	        if (accessToken == null || accessToken.isBlank()) {
	            return new ResponseEntity<>("토큰 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
	        }

	        try {
	            refreshTokenService.removeRefreshToken(accessToken);
	            System.out.println("레디스 지우기 성공");
	        } catch (Exception e) {
	        	System.out.println("레디스 지우기 실패");
	            e.printStackTrace();
	        }
	        
	        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
	                .httpOnly(true)
	                .secure(false)
	                .sameSite("Lax")
	                .path("/")
	                .maxAge(0)  // 즉시 만료
	                .build();
	        response.addHeader("Set-Cookie", deleteCookie.toString());
	        
	        return new ResponseEntity<>("로그아웃 되었습니다.", HttpStatus.OK);
	    }
	    
	    public ResponseEntity<?> localDelete(String header) {
	        // 1) 토큰에서 accessToken 추출
	        String accessToken = jwtUtil.findTokenByHeader(header);
	        if (accessToken == null || accessToken.isBlank()) {
	            return new ResponseEntity<>("토큰 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
	        }

	        // 2) refreshToken 삭제 (레디스/DB)
	        try {
	            refreshTokenService.removeRefreshToken(accessToken);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }

	        // 3) 실제 유저 soft-delete 처리 (공통 로직 재사용)
	        return userService.deleteUser(header);
	        // 또는 위에 적어둔 deleteUser 내용을 여기로 옮겨도 OK
	    }
//	public ResponseEntity<?> generationToken (Map<String, String> body ) {
//		// 만료된 accessToken으로 redis에서 refreshToken을 검색
//		System.out.println("생성 시작");
//		String oldAccessToken = body.get("accessToken");
//		String refreshToken = body.get("refreshToken");
//		
//		if (refreshToken == null || refreshToken.isBlank()) {
//		    System.out.println("새로생성실패, refreshToken 없음");
//		    return new ResponseEntity<>("인증 정보가 유효하지 않습니다.", HttpStatus.UNAUTHORIZED);
//		}
//		
//		Optional<RefreshToken> foundTokenInfo = jwtUtil.checkRefreshToken(oldAccessToken);
//		
//		
//		
//		if (foundTokenInfo.isEmpty() || // redis에 refreshToken이 존재 하지 않을때
//			!foundTokenInfo.get().getRefreshToken().equals(refreshToken) || //redis에 존재하는 refreshToken 과 일치하지 않을때
//			refreshToken.isBlank() // 클라이언트에게 refreshToken이 없을때
//			) {
//			System.out.println("새로생성실패");
//			return new ResponseEntity<>("인증 정보가 유효하지 않습니다.", HttpStatus.UNAUTHORIZED);
//		} 
//			RefreshToken tokenEntity = foundTokenInfo.get(); // redis에 저장되는 엔티티 객체
//			Long userId = tokenEntity.getUserId(); // 저장된 유저아이디 
//			
//			String newAccessToken = jwtUtil.createAccessToken(userId); // 유저 아이디로 새로 accessToken 생성
//			
//			tokenEntity.setAccessToken(newAccessToken); // 앤티티에 AccessToken 교체 old -> new
//			refreshTokenRepository.save(tokenEntity); // redis에 저장 (update)
//			
//			Map<String, String> result = new HashMap<>();
//			System.out.println("새로생성");
//			result.put("accessToken", newAccessToken);
//			return new ResponseEntity<>(result, HttpStatus.OK);
//		
//		
//	}
	    public ResponseEntity<?> generationToken(String oldAccessToken, String refreshToken, HttpServletResponse response) {
	        System.out.println("생성 시작");

	        if (refreshToken == null || refreshToken.isBlank()) {
	            System.out.println("새로생성실패, refreshToken 없음");
	           
	            return new ResponseEntity<>("인증 정보가 유효하지 않습니다.", HttpStatus.UNAUTHORIZED);
	        }

	        Optional<RefreshToken> foundTokenInfo = jwtUtil.checkRefreshToken(oldAccessToken);

	        if (foundTokenInfo.isEmpty() ||
	            !foundTokenInfo.get().getRefreshToken().equals(refreshToken) ||
	            refreshToken.isBlank()
	        ) {
	            System.out.println("새로생성실패");
	            
	            // TODO: 유효하지 않은 접근으로, User.email을 통해, 이메일로 경고 보내기
	            refreshTokenService.removeRefreshToken(oldAccessToken);
	            
	            ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
	                    .httpOnly(true)
	                    .secure(false)   // 로컬: false / 배포 시 true + SameSite 조정
	                    .sameSite("Lax")
	                    .path("/")
	                    .maxAge(0)       // 즉시 만료
	                    .build();
	            response.addHeader("Set-Cookie", deleteCookie.toString());
	            return new ResponseEntity<>("인증 정보가 유효하지 않습니다.", HttpStatus.UNAUTHORIZED);
	        }

	        RefreshToken tokenEntity = foundTokenInfo.get();
	        Long userId = tokenEntity.getUserId();

	        String newAccessToken = jwtUtil.createAccessToken(userId);

	        tokenEntity.setAccessToken(newAccessToken);
	        refreshTokenRepository.save(tokenEntity);

	        Map<String, String> result = new HashMap<>();
	        System.out.println("새로생성");
	        result.put("accessToken", newAccessToken);
	        return new ResponseEntity<>(result, HttpStatus.OK);
	    }
	
	public ResponseEntity<?> getUser (String header) {
		Long userId = jwtUtil.findUserIdByHeader(header);
		if (userId == null) {
            return new ResponseEntity<>("토큰 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
		
		Optional<User> _user =  userRepository.findById(userId);
		if (_user.isEmpty()) {
            return new ResponseEntity<>("유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
		User user = _user.get();
		Long UserId = user.getId();
		String nickname = user.getNickname();
		LocalDate birth_date = user.getBirth_date();
		String mbti = user.getMbti();
		String authType = user.getAuthType();
		String role = user.getRole();

		String profileImageUrl = user.getProfileImageUrl();
		String email = user.getEmail();
		UserResponseDto responseDto = new UserResponseDto(userId ,nickname, birth_date, mbti, authType, role, profileImageUrl, email);

		return new ResponseEntity<>(responseDto, HttpStatus.OK);
	}
	
	public Optional<User> checkUsername (String username) {
		return userRepository.findByEmail(username);
		
	}
	
	
	
}
