package com.mind_mate.home.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@RedisHash(value = "refreshToken", timeToLive = 60 * 60 * 24 * 3) // 유효기간 3일 (레디스 기준)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class RefreshToken {
	
	@Id
	private Long userId;

	private String refreshToken;
	
	@Indexed
	private String accessToken;
	
	public RefreshToken(Long userId, String refreshToken, String accessToken) {
		this.userId = userId;
		this.refreshToken = refreshToken;
		this.accessToken = accessToken;
	}
	
	
	
	
}
