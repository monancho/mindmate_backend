package com.mind_mate.home.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsyncMailService {
	private final JavaMailSender javaMailSender;
    private final RedisService redisService;
    
    private static final String EMAIL_STATUS_KEY_PREFIX = "email:verify:status:";
    private static final long EMAIL_STATUS_TTL_SECONDS = 60 * 10; // 10분
    
    
    @Async
    public void sendEmailAsync(String email, MimeMessage message) {
        String statusKey = EMAIL_STATUS_KEY_PREFIX + email;

        try {
            javaMailSender.send(message);

            // 전송 성공 => SENT
            try {
                redisService.setDataExpire(statusKey, "SENT", EMAIL_STATUS_TTL_SECONDS);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            // 전송 실패 => FAILED
            try {
                redisService.setDataExpire(statusKey, "FAILED", EMAIL_STATUS_TTL_SECONDS);
            } catch (Exception ignored) {}
        }
    }
}
