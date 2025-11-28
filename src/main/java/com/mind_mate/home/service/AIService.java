package com.mind_mate.home.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.mind_mate.home.dto.DiaryEmojiDto;
import com.mind_mate.home.dto.DiaryEmotionDto;
import com.mind_mate.home.entity.AICharacter;
import com.mind_mate.home.entity.AIResult;
import com.mind_mate.home.entity.User;
import com.mind_mate.home.repository.AIRepository;
import com.mind_mate.home.repository.DiaryRepository;
import com.mind_mate.home.repository.UserRepository;
import com.mind_mate.home.util.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AIService {

    private final DiaryRepository diaryRepository;
	
	private final Client geminiClient;
	
	private final UserRepository userRepository;
	
	@Autowired
	AIRepository aiRepository;
	
	@Autowired
	JwtUtil jwtUtil;

	private String buildPrompt (String status, String content) {
		return switch (status) {
		case "diary" -> 
			"다음의 내 일기를 읽고 상황에 맞게 위로 또는 조언을 50자 이내로 해줘.\n"
			+ "반환 형식은 다음과같아.\n"
			+ "오늘하루도 고생하셨어요!\n"
			+ "(위로 또는 조언) \n"
			+ "영어단어: (영어단어)\n\n"
			+ "일기인지 판단하되, 판단 과정이나 설명은 절대 출력하지 마.\n"
			+ "단, 다음의 내용이 일기내용이 아니라면 너는 '일기 내용이 아니네요. 대답할 수 없는 내용입니다. 영어단어: unknown' 로만 답변해줘.\n"
			+ "내용: " + content + "\n\n"
			+ "그리고 위 내용을 한단어로 요약해서 맞는 영어단어를 반환해줘 "
			+ "단 영어단어는 다음중 하나만 사용할 수 있어\n"
			+ "(heart, love, happy, relax, smile, wow, joy, meh, unsure, sad, spin, tears, shock, unwell, angry)";
		case "weekly" -> 
			"다음의 감정 통계는 이번주 감정 상태를 간단하게 요약한 것이야.\n "
			+ "이번 주 감정 상태를 바탕으로 다음주의 나에게 조언 또는 위로의 말을 100자 이내로 해줘 .\n"
			+ "반환 형식은 다음과 같아.\n"
			+ "이번 주간에는 (위로 또는 조언)\n"
			+ "다음 주간에는 (감정상태에 따른 긍정적인 미래).\n"
			+ "단 1번, 다음의 내용이 감정 상태와 연관이 없다면 너는 '감정상태를 파악할 수 없습니다.' 로 통일해줘.\n"
			+ "2번, 다음의 감정상태의 총 합이 7개가 아니라면 이번주에 관한 위로 또는 조언만 해줘."
			+ "내용: "+ content;
		case "tags" -> 
			"다음의 내 글을 읽고 상황이나 주제에 맞는 키워드 5개를 추출해서 해시태그 형식으로 반환해줘.\n"
			+ "반환 형식은 다음과 같아.\n"
			+ "#키워드1 #키워드2 #키워드3 #키워드4 #키워드5\n"
			+ "단 내용이 50자 이내이거나 단순한 일상 표현이라면 "
			+ "키워드는 3개를 추출해서 위의 반환 형식으로 반환해줘.\n"
			+ "또는 내용의 의미를 찾을 수 없다면 #내용없음 만 반환해줘."
			+ "내용: "+ content; 
		case "fortune" -> 
			"사용자의 생일 : '"+ content + "'을(를) 기반으로 오늘의 운세를 별자리 운세로 알려줘.\n "
			+ "반환 형식은 다음과 같아.\n"
			+ "(생일)은 (별자리)자리군요!\n"
			+ "**오늘의 운세**\n "
			+ "(운세 핵심 요약) \n"
			+ "행운의 시간: \n "
			+ "행운의 물건: \n"
			+ "행운의 색상: \n "
			+ "100자 이내로 작성해줘.\n"
			+ "단, 생일 형식이 아닌 내용이라면 '유효하지 않은 생일 정보입니다.' 로 통일해줘.";
		case "daily_test" -> 
			"사용자의 MBTI 성격 유형은 : '"+ content + "'입니다.\n "
			+ "이 MBTI 유형의 사람의 성향에 맞는 오늘의 심리 테스트를 만들어줘.\n"
			+ "반환 형식은 다음과 같아.\n"
			+ "질문 : (감정, 인간관계, 자기성찰과 관련된 랜덤 질문 1개)"
			+ "A: (보기 1) "
			+ "B: (보기 2) "
			+ "C: (보기 3) "
			+ "D: (보기 4) "
			+ "단 보기 4개는 서로 다른 선택지를 가져야 함.";
		case "daily_result" -> 
			"너는 심리테스트 해석가야.\n"
			+ "다음은 사용자의 MBTI, 질문, 그리고 사용자가 고른 선택지야.\n"
			+ "이 정보를 바탕으로 사용자의 오늘 심리상태를 짧게 해석해줘 (50자 이내).\n\n"
	        + "데이터:\n" +content + "\n\n"
	        + "반환 형식:\n"
	        + "오늘의 심리 결과: (감정 분석)\n"
	        + "짧은 조언: (짧은 문장)";
//		case "emoji" -> 
//			"다음 일기 내용을 읽고 감정을 판단한 뒤 가장 잘 맞는 감정을 영어단어로 3개 추천해줘.\n"
//			+ "반환 형식은 반드시 다음과 같아 : \n"
//	        + "단어1, 단어2, 단어3\n"
//	        + "단 영어단어는 "
//	        + "(heart, love, happy, relax, smile, wow, joy, meh, unsure, sad, spin, tears, shock, unwell, angry)"
//	        + "만 사용할 수 있어."
//	        + "일기 내용: " + content;
		default ->  "다음 내용을 바탕으로 간단히 감정 분석 코멘트를 작성해줘:\n" + content;
		};
	}

	public AIResult generatResult(String status, String content) {
	    String prompt = buildPrompt(status, content);

	    int maxRetry = 3;
	    int retryCount = 0;

	    while (retryCount < maxRetry) {
	        try {
	            GenerateContentResponse response =
	                geminiClient.models.generateContent("gemini-2.5-flash", prompt, null);

	            String text = response.text();

	            AIResult result = new AIResult();
	            result.setStatus(status);
	            result.setAicomment(text);

	            if (!status.equals("daily_test") && !status.equals("weekly")) {
	                aiRepository.save(result);
	            }

	            return result;

	        } catch (Exception e) {
	        	e.printStackTrace();
	            retryCount++;
	            System.out.println("Gemini API 실패, 재시도: " + retryCount);

	            // 503 또는 network error일 때 재시도
	            if (retryCount >= maxRetry) {
	                AIResult result = new AIResult();
	                result.setStatus(status);
	                result.setAicomment("현재 AI 서버가 혼잡합니다. 잠시 후 다시 시도해주세요.");
	                return result;
	            }

	            try {
	                Thread.sleep(1000 * retryCount); // 점진적 대기
	            } catch (InterruptedException ie) {
	                Thread.currentThread().interrupt();
	            }
	        }
	    }

	    AIResult fallback = new AIResult();
	    fallback.setStatus(status);
	    fallback.setAicomment("AI 서버가 응답하지 않습니다. 잠시 후 다시 시도해주세요.");
	    return fallback;
	}

	
	public AIResult generateWeeklyResult(String header,LocalDate start, LocalDate end, List<DiaryEmotionDto> dtos) {
		Long userId = jwtUtil.findUserIdByHeader(header);
		User user = userRepository.findById(userId).orElseThrow();
		LocalDate today = LocalDate.now();
		boolean weekEnded = !today.isBefore(end); // today >= end
		
		 if (weekEnded) {
		        Optional<AIResult> existing = aiRepository.findByUserIdAndStatusAndStartdateAndEnddate(user.getId(),"weekly", start, end);
		        if (existing.isPresent()) {
		            System.out.println("기존 주간 코멘트 재사용됨");
		            return existing.get();
		        }
		    }
		 
		StringBuilder content = new StringBuilder();
        content.append("이번 주 감정 통계: \n");
        for (DiaryEmotionDto dto : dtos) {
            content.append(dto.getEmojiName())
                    .append(" : ")
                    .append(dto.getCount())
                    .append("회\n");
        }
        AIResult aiResult = generatResult("weekly", content.toString());

        if(weekEnded) {
        	aiResult.setUser(user);
        	 aiResult.setStartdate(start);
             aiResult.setEnddate(end);
             aiRepository.save(aiResult);
        }
       

        return aiResult;
		
	}
	public AIResult getResult (String header, LocalDate date) {
		Long userId = jwtUtil.findUserIdByHeader(header);
		
		LocalDateTime start = date.atStartOfDay();           
		LocalDateTime end = date.plusDays(1).atStartOfDay();
		
		return aiRepository.findByUserIdAndCreatedBetween(userId, start, end)
		        .orElseThrow(() -> new RuntimeException("해당 날짜의 AI 결과가 없습니다. userId=" + userId + ", date=" + date));

	}
	public String generateResponse(AICharacter character, String userMessage, String nickName) {
		String prompt = "당신은 사용자가 만든 AI 캐릭터 '" + character.getName() + "'입니다.\n"
	            + "캐릭터의 현재 기분 점수: " + character.getMoodscore() + "\n"
	            + "사용자 '" + nickName + "'가 이렇게 말했습니다: \"" + userMessage + "\"\n"
	            + "이 상황에서 캐릭터가 친근하고 자연스럽게 50자 이내로 답변하도록 작성해주세요.\n"
	            + "답변만 출력하고, 추가 설명은 하지 마세요.";
 
 GenerateContentResponse response = geminiClient.models.generateContent("gemini-2.5-flash", prompt, null);
 return response.text();
    }
	
}
