package com.mind_mate.home.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.mind_mate.home.entity.Email;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
	
	private final EmailRepository emailRepository;
	private final RedisService redisService;
	private final JavaMailSender javaMailSender;   
	private final AsyncMailService asyncMailService;
    private static final String senderEmail = "dbstjq23@gmail.com";
    
	// 1시간 안에 최대 5번까지만 코드 요청 허용
    private static final int MAX_REQUESTS_PER_HOUR = 5;
    private static final String EMAIL_COUNT_KEY_PREFIX = "email:verify:count:";
    
 // 이메일 인증 상태 키 => 이메일 코드 비동기로 전송하기 위한것
    private static final String EMAIL_STATUS_KEY_PREFIX = "email:verify:status:";
    private static final long EMAIL_STATUS_TTL_SECONDS = 60 * 10; // 10분
    
    public ResponseEntity<?> getEmailStatus(String email) {
        String statusKey = EMAIL_STATUS_KEY_PREFIX + email;

        try {
            String status = redisService.getData(statusKey);

            if (status == null) {
                // 상태 정보가 없을 때
                return ResponseEntity.ok("NONE");
            }

            return ResponseEntity.ok(status); // REQUESTED / SENT / FAILED

        } catch (Exception e) {
            // Redis 장애 등으로 상태를 못 불러오는 경우
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("이메일 상태를 조회할 수 없습니다.");
        }
    }
    
    public ResponseEntity<?> sendMailWithCode(String email, HttpServletRequest request) {
    	String statusKey = EMAIL_STATUS_KEY_PREFIX + email;
    	
    	try { // REQUESTED => 
    		redisService.setDataExpire(statusKey, "REQUESTED", EMAIL_STATUS_TTL_SECONDS);
    	} catch (Exception e) {
    		
    	}
    	
        try {
            // 1시간 5회 제한 + 코드 생성 + Redis/세션 저장
            String code = createAndStoreCode(email, request);

            //  메일 생성 및 발송은 비동기로 처리
            MimeMessage message = createMail(email, code);
            asyncMailService.sendEmailAsync(email, message);

            return ResponseEntity.ok("이메일 전송 요청을 완료 했습니다.");

        } catch (IllegalStateException e) {
        	try {
                redisService.setDataExpire(statusKey, "FAILED", EMAIL_STATUS_TTL_SECONDS);
            } catch (Exception ignored) { }

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(e.getMessage());

        } catch (Exception e) {
        	try {
                redisService.setDataExpire(statusKey, "FAILED", EMAIL_STATUS_TTL_SECONDS);
            } catch (Exception ignored) { }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("이메일 전송 처리 중 오류가 발생했습니다.");
        }
    }
    
    private MimeMessage createMail(String email, String code) {
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            message.setFrom(senderEmail);
            message.setRecipients(MimeMessage.RecipientType.TO, email);
            message.setSubject("MindMate 이메일 인증 코드 안내");

            String body = """
                    <table width="100%%" cellpadding="0" cellspacing="0" border="0" 
                           style="margin:0;padding:0;background-color:#f5f5f7;">
                      <tr>
                        <td align="center" style="padding:24px 12px;">
                          <table width="420" cellpadding="0" cellspacing="0" border="0"
                                 style="max-width:420px;background-color:#ffffff;
                                        border-radius:18px;padding:28px 24px 22px;
                                        box-sizing:border-box;font-family:'Segoe UI',
                                        -apple-system,BlinkMacSystemFont,sans-serif;
                                        box-shadow:0 8px 20px rgba(0,0,0,0.03);">
                            
                            <!-- 로고/브랜드 -->
                            <tr>
                              <td style="font-size:17px;font-weight:600;color:#d08852;padding-bottom:4px;">
                                MindMate
                              </td>
                            </tr>

                            <!-- 타이틀 -->
                            <tr>
                              <td style="font-size:21px;font-weight:600;color:#7a4427;padding-bottom:6px;">
                                이메일 인증 코드
                              </td>
                            </tr>

                            <!-- 서브텍스트 -->
                            <tr>
                              <td style="font-size:13px;color:#a0775b;line-height:1.6;padding-bottom:18px;">
                                아래 인증 코드를 회원가입 화면에 입력해 주세요.
                              </td>
                            </tr>

                            <!-- 코드 박스 -->
                            <tr>
                              <td align="center" 
                                  style="padding:14px 12px;border-radius:14px;
                                         background-color:#fff1e4;border:1px solid #f2c7a2;
                                         letter-spacing:4px;font-size:24px;font-weight:600;
                                         color:#c06830;">
                                %s
                              </td>
                            </tr>

                            <!-- 안내 문구 -->
                            <tr>
                              <td style="font-size:12px;color:#a0775b;line-height:1.7;padding-top:16px;">
                                · 이 코드는 <b>일정 시간 동안만 유효</b>합니다.<br>
                                · 본인이 요청하지 않은 경우, 이 메일은 무시하셔도 됩니다.
                              </td>
                            </tr>

                            <!-- 푸터 -->
                            <tr>
                              <td style="font-size:11px;color:#c4aa96;border-top:1px solid #f3e2d5;
                                         padding-top:10px;margin-top:8px;">
                                https://mindmate.co.kr
                              </td>
                            </tr>

                          </table>
                        </td>
                      </tr>
                    </table>
                    """.formatted(code);

            message.setContent(body, "text/html; charset=UTF-8");
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return message;
    }
    
    private String generateCode() {// 100000 ~ 999999 범위에서 랜덤 6자리 
        return Integer.toString((int) (Math.random() * 900000) + 100000);
    }
    
    public String createAndStoreCode(String email, HttpServletRequest request) {
        // 1시간 5회 제한 체크
        if (isOverLimitAndIncrease(email, request)) {
            throw new IllegalStateException("인증코드 요청 가능 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");
        }

        String code = generateCode();

        // Redis에 Email 엔티티 저장 (유효시간 300초)
        try {
            Email emailEntity = new Email(email, code);
            emailRepository.save(emailEntity); // @RedisHash(timeToLive=300)
        } catch (Exception e) {
            // Redis가 죽어 있으면 세션에 저장
            HttpSession session = request.getSession(true);
            session.setAttribute("emailCode:" + email, code);
        }

        return code;
    }
    
    
    private boolean isOverLimitAndIncrease(String email, HttpServletRequest request) {
        String key = EMAIL_COUNT_KEY_PREFIX + email; 
        // 예: email:verify:count:tiger@abc.com

        try {
            // redis에서 현재 카운트 조회
            String current = redisService.getData(key);
            int count = (current == null) ? 0 : Integer.parseInt(current);

            // 5번 초과시 최대 횟수 도달 => 리턴
            if (count >= MAX_REQUESTS_PER_HOUR) {
                return true;
            }

            // 카운트 여유 있을시, 카운트 1 추가 및 유효시간 1시간 설정
            int newCount = count + 1;
            long oneHourSeconds = 60 * 60;
            redisService.setDataExpire(key, String.valueOf(newCount), oneHourSeconds);
            return false;

        } catch (Exception e) {
            // redis가 작동 안할시 세션으로 대체 (Fallback 처리)
            HttpSession session = request.getSession(true);
            String sessionKey = "emailCodeCount:" + email;
            Object value = session.getAttribute(sessionKey);

            int count = 0;
            if (value instanceof Integer) {
                count = (Integer) value;
            }

            if (count >= MAX_REQUESTS_PER_HOUR) {
                return true;
            }

            session.setAttribute(sessionKey, count + 1);
            return false;
        }
    }
    
    private String findCode(String email, HttpServletRequest request) {
    	try {
    		Optional<Email> _email = emailRepository.findById(email);
    		if (_email.isPresent()) {
    			return _email.get().getCode();
    		} 
    	} catch (Exception e) {
			// redis 장애 => 세션에서 처리
		}
    	
    	// 세션에서 찾기 (redis에서 찾지 못하거나 redis가 죽었을때)
    	HttpSession session = request.getSession(false);
    	if (session == null) return null;
    	
    	Object sessionCode = session.getAttribute("emailCode:" + email);
    	if (sessionCode == null) return null;
    	
    	return sessionCode.toString();
    }
    
    private void deleteCode(String email, HttpServletRequest request) {
    	try {
    		emailRepository.deleteById(email);
    	} catch (Exception e) {
			// 에러 무시
		}
    	
    	HttpSession session = request.getSession(false);
    	if (session != null) {
    		session.removeAttribute("emailCode:" + email);
    	}
    }
    
    // 코드체크 사용 (소프트 체크, 코드 삭제 X)
    public boolean checkEmailCodeOnly (String email, String code, HttpServletRequest request) {
    	// 코드가 유효한지 체크
    	String savedCode = findCode(email, request);
    	if (savedCode == null) return false;
    	return savedCode.equals(code);
    }
    
    // 회원생성 사용 (하드 체크, 코드 삭제 O)
    public boolean isEmailCode(String email, String code, HttpServletRequest request) {
    	if (!checkEmailCodeOnly(email, code, request)) {
    		return false;
    	}
    	
    	deleteCode(email, request);
    	return true;
    }

}
