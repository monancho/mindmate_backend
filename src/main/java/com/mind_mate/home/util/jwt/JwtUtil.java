package com.mind_mate.home.util.jwt;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

//import com.mind_mate.home.controller.AuthController;
//import com.mind_mate.home.entity.Account;
import com.mind_mate.home.entity.RefreshToken;
import com.mind_mate.home.entity.User;
//import com.mind_mate.home.repository.AccountRepository;
import com.mind_mate.home.repository.RefreshTokenRepository;
import com.mind_mate.home.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final UserRepository userRepository;

//    private final AuthController authController;
	
//	@Autowired
//    private AccountRepository accountRepository;
	

	private final RefreshTokenRepository refreshTokenRepository;
	
	public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
	
    
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 30 ;            // 30분 배포할떄는 10분으로 줄이기
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7;  // 7일

//	private static final Long Long = null;

    
	@Value("${jwt.secret}") // 토큰을 생성하는 비밀키
	private String secret; // application.properties 내에 선언되어 있는 jwt.secret 값 가져와 저장
	
	@Value("${jwt.expiration}") // 토큰의 유효시간
	private Long expiration;

//    JwtUtil(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }

//    JwtUtil(AccountRepository accountRepository, AuthController authController) {
//        this.accountRepository = accountRepository;
//        this.authController = authController;
//    }
	
	public String findTokenByHeader(String header) {
		if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
			return null;
		}
		String token = header.replace("Bearer ", "");
		return token;
	}
	
	public boolean isExpired(String token) {
		if (!StringUtils.hasText(token)) return false;
		try {
			Jwts.parser()
			.setSigningKey(secret)
			.parseClaimsJws(token);
			return true;
			
		} catch (ExpiredJwtException e) { 
			// 정상 토큰이지만 만료
			return false;
		} catch (JwtException | IllegalArgumentException e) {
			// 위조 및 잘못된 형식 가능
			return false;
		}
		
	}
	

	// 엑세스 토큰 생성
	public String createAccessToken(Long profileId) { 
		return Jwts.builder()
				.setSubject(String.valueOf(profileId)) // 인증 받을 사용자 이름
				.setIssuedAt(new Date()) // 토큰 발급된 시간
				.setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE_TIME))
				.signWith(SignatureAlgorithm.HS256, secret)
				.compact();
	}
	
	// 리프레시 토큰 생성
	public String createRefreshToken() {
			
			Claims claims = Jwts.claims();			
			return Jwts
					.builder()
					.setClaims(claims)
					.setIssuedAt(new Date(System.currentTimeMillis())) // 현재 시간
					.setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_TIME)) // 유효 시간 (3일)
					.signWith(SignatureAlgorithm.HS256, secret)
					.compact();
		}
	
	// 만료된 엑세스 토큰으로 redish에 저장된 리프레시 토큰 검색 후 사용자의 키와 비교
	
	public Optional<RefreshToken> checkRefreshToken(String accessToken) {
		if (!StringUtils.hasText(accessToken)) {
	        return Optional.empty();
	    }
		try {
	        return refreshTokenRepository.findByAccessToken(accessToken);
	    } catch (DataAccessException e) {
//	        e.printStackTrace();
	        return Optional.empty(); // → 호출 측에서 "인증 정보가 유효하지 않습니다" 처리
	    }
		
	}
	
//	public String findUsernameByHeader (String header) {
//		String token = header.replace("Bearer ", "");
//		String username = Jwts.parser()
//				.setSigningKey(secret)
//				.parseClaimsJws(token)
//				.getBody()
//				.getSubject();
//		
//		return username;
//	}
//	
//	public Optional<Account> findAccountByHeader (String header) {
//		String username = findUsernameByHeader(header);
//	Optional<Account> _account	= accountRepository.findByUsername(username);
//	return _account;
//	}
//	
//	public Long findUserIDByHeader (String header) {
//		Optional<Account> _account = findAccountByHeader(header);
//		return _account.get().getId();
//	}
	
	
//	// 토큰에서 사용자 이름 추출(username)
//		public String extractUsername(String token) {
//			return Jwts.parser()
//					.setSigningKey(secret)
//					.parseClaimsJws(token)
//					.getBody()
//					.getSubject();
//		}
	
		public Optional<User> findUserEntityByHeader(String header) {
			Long userId = findUserIdByHeader(header);
			Optional<User> _user = userRepository.findById(userId);
 			return _user;
		}
	
	// 토큰에서 프로필 아이디 추출
		public Long extractUserId(String token) {
			String subject = Jwts.parser()
					.setSigningKey(secret)
					.parseClaimsJws(token)
					.getBody()
					.getSubject();
			return Long.valueOf(subject);
		}
		
		 public Long findUserIdByHeader(String header) {
		        String token = findTokenByHeader(header);
		        if (token == null) {
		            return null;
		        }
		        
		        Long userId = extractUserId(token);
		        return userId;
		    }
}
