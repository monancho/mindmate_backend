package com.mind_mate.home.config;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.mind_mate.home.util.jwt.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {
	
	@Autowired
	private JwtAuthenticationFilter authenticationFilter;
	@Value("${spring.web.cors.allowed-origins}")
    private String allowedOrigins;
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
        .csrf(csrf -> csrf.disable()) //csrf 비활성화 
		.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()

				.requestMatchers(
						"/api/auth/signup", 
						"/api/auth/login", 
						"/api/auth/login/**",
						"/api/auth/refresh",
						"/api/auth/email/code",
						"/api/auth/check_username",
						"/api/auth/check_code",
						"/api/user/check_nickname",
						"/uploads/**",
						"/api/boards/**", "/api/comments/**", "/api/emoji/**").permitAll() //인증 없이 접근 가능한 요청들

				
				.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/logout")
                .authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/delete/**")
                .authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/auth/me")
                .authenticated()
                
				.anyRequest().authenticated() //위 요청을 제외한 나머지 요청들은 전부 인증 필요
				)
				.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
				// 로그인 하지 않고도 JWT 존재하면 요청을 받게 하는 설정
				.cors(cors -> cors.configurationSource(request -> {
					CorsConfiguration config = new CorsConfiguration();
					config.setAllowCredentials(true);
					config.setAllowedOrigins(List.of
							(allowedOrigins.split(",")
							)); //허용 ip주소
					config.setAllowedMethods(List.of("GET","POST","PUT","DELETE", "OPTIONS"));
					config.setAllowedHeaders(List.of("*"));
					return config;
				})
						
						);
        return http.build();
        
    }
        @Bean
    	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    		return config.getAuthenticationManager();
    		// 사용자 인증을 처리하는 객체 반환
        

    }
}