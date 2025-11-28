package com.mind_mate.home.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "email", timeToLive = 300) // 300초 = 5분
public class Email {
	
	@Id
	private String email;
	private String code;

}
