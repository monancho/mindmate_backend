package com.mind_mate.home.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;

@Configuration
public class RedisConfig {
	@Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;
    
    @Bean // redis와 연결을 위한 커넥션 생성
    public RedisConnectionFactory redisConnectionFactory() {

        // Redis 서버 기본 설정 (host, port)
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(host);
        serverConfig.setPort(port);

        // 연결 타임아웃 500ms => 레디스 죽을시 500ms 시도하고 안되면 포기
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(500)) // 레디스 죽을시 500ms 시도하고 안되면 포기
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .build();

        //커맨드 타임아웃 500ms  => 레디스 요청시 500ms안으로 응답없으면 에러 던지기
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(500)) // 명령 응답 대기 시간
                .clientOptions(clientOptions)
                .build();

        // 4) 최종 커넥션 팩토리 생성
        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }
}
