package com.mind_mate.home.util.jwt;

import java.io.IOException;

import javax.security.auth.login.AccountException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.CookieGenerator;

import com.mind_mate.home.entity.Account;
import com.mind_mate.home.entity.RefreshToken;
import com.mind_mate.home.repository.AccountRepository;
import com.mind_mate.home.repository.RefreshTokenRepository;

import ch.qos.logback.core.spi.ErrorCodes;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jdk.jshell.spi.ExecutionControl.UserException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter{
	
	public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
	
    
	@Autowired
	private JwtUtil jwtUtil;
	@Autowired
	private AccountRepository accountRepository;
	@Autowired
	private RefreshTokenRepository refreshTokenRepository;
	
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		 String path = request.getRequestURI();

		    return path.startsWith("/api/auth/login")
		        || path.startsWith("/api/auth/signup")
		        || path.startsWith("/api/auth/refresh")
		        || path.startsWith("/oauth") // 소셜 로그인 콜백 등ㄴ
		    	|| path.startsWith("/api/comments")
		    	;
	}	

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		
		String token = parseJwt(request); 
		
		if (token != null && !token.isEmpty()) {
			try {
				Long profileId = jwtUtil.extractUserId(token); // 토큰을 사용하는 사용자 이름
				
				// 토큰 인증 객체 생성
				UsernamePasswordAuthenticationToken authenticationToken =
						new UsernamePasswordAuthenticationToken(profileId, null, null);
				
				authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				// 토큰 인증 객체에 추가로 사용자 정보를 담음 -> 클라이언트의 ip 주소, 세션 id
				
				// Security에 토큰 인증 객체 정보를 저장
				SecurityContextHolder.getContext().setAuthentication(authenticationToken);
				
			} catch (ExpiredJwtException e) {
			    // 토큰 만료: 컨텍스트 정리, 401 응답 후 필터 체인 중단
			    System.out.println("JWT 토큰 만료: " + e.getMessage());
			    SecurityContextHolder.clearContext();

			    response.setStatus(HttpStatus.UNAUTHORIZED.value());
			    response.setContentType("application/json;charset=UTF-8");
			    response.getWriter().write("{\"message\":\"Access token expired\"}");
			    response.getWriter().flush();
			    return; // 필터 체인 진행 차단
			} catch (JwtException | IllegalArgumentException e) {
			    // 서명불일치 등 기타 JWT 오류도 401로 처리
			    System.out.println("JWT 인증 실패: " + e.getMessage());
			    SecurityContextHolder.clearContext();

			    response.setStatus(HttpStatus.UNAUTHORIZED.value());
			    response.setContentType("application/json;charset=UTF-8");
			    response.getWriter().write("{\"message\":\"Invalid token\"}");
			    response.getWriter().flush();
			    return;
			}

		}
		filterChain.doFilter(request, response);
	}
		
		private String parseJwt(HttpServletRequest request) {
			String headerAuth = request.getHeader(AUTHORIZATION_HEADER);
			if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(BEARER_PREFIX)) {
				return headerAuth.substring(7);
			}
			return null;
		}
		
		
		
		
		
		
}
