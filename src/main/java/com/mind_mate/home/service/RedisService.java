package com.mind_mate.home.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {
	private final StringRedisTemplate redisTemplate;
	
	public String getData(String key) {
		
		try {
			ValueOperations<String, String> ops = redisTemplate.opsForValue();
			return ops.get(key);
		} catch (Exception e) {
			throw new IllegalStateException("Redis를 사용할수 없습니다.", e);
		}
    }


    public void setDataExpire(String key, String value, long durationSeconds) {
	 try {
	        ValueOperations<String, String> ops = redisTemplate.opsForValue();
	        Duration expire = Duration.ofSeconds(durationSeconds);
	        ops.set(key, value, expire);
	 } catch (Exception e) {
			throw new IllegalStateException("Redis를 사용할수 없습니다.", e);
		}
    }

    public void deleteData(String key) {
    try {
        redisTemplate.delete(key);
    } catch (Exception e) {
		throw new IllegalStateException("Redis를 사용할수 없습니다.", e);
	}
    }
}
