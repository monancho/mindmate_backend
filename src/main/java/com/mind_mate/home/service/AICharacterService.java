
package com.mind_mate.home.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mind_mate.home.entity.User;
import com.mind_mate.home.dto.AICharacterDto;
import com.mind_mate.home.entity.AICharacter;
import com.mind_mate.home.entity.EmojiList;
import com.mind_mate.home.repository.AICharacterRepository;
import com.mind_mate.home.repository.DiaryRepository;
import com.mind_mate.home.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AICharacterService {

    private final SocialAuthService socialAuthService;

	private final AICharacterRepository characterRepository;
	private final UserService userService;
	private final DiaryRepository diaryRepository;
	private final AIService aiService;

	public AICharacter createCharacter(String header, String characterName) {

		User user = userService.getProfile(header);
		
		AICharacter character = new AICharacter();
		character.setLastCheer(null);
		character.setUser(user);
		character.setName(characterName);
		character.setLevel(1);
		character.setPoints(0);
		character.setMoodscore(50);
		
		return characterRepository.save(character);
	}
	
	@Transactional
	public Optional<AICharacter> getCharacter(String header) {
		User user  = userService.getProfile(header);
		return characterRepository.findByUser_Id(user.getId())
	            .map(aiCharacter -> {
	                if (aiCharacter.getLastCheer() != null &&
	                    !aiCharacter.getLastCheer().toLocalDate().equals(LocalDate.now())) {
	                    aiCharacter.setMoodscore(50);
	                    characterRepository.save(aiCharacter);
	                }
	                return aiCharacter;
	            });
	}
 
	@Transactional
	public AICharacterDto applyDiaryEmojiMood(String header, EmojiList emojiList) {
		User user = userService.getProfile(header);
		Optional<AICharacter> aiCharacterOpt = getCharacter(header);
		
		if (aiCharacterOpt.isEmpty()) {
	        return null; // 캐릭터 없으면 null 반환
	    }
		 AICharacter aiCharacter = aiCharacterOpt.get();
		int moodchange = 50;
		
		if (emojiList.getId() >= 1 && emojiList.getId() <= 7) {
			moodchange += (8 - emojiList.getId().intValue()) * 5; // 5~35
		} else if (emojiList.getId() >= 8 && emojiList.getId() <= 15) {
		    // 부정 이모지
			moodchange -= (emojiList.getId().intValue() - 7) * 5; // -5 ~ -40
		}
		aiCharacter.setMoodscore(Math.min(100, Math.max(0, moodchange)));
	    characterRepository.save(aiCharacter);

	    // 오늘 선택한 이모지 저장
	    AICharacterDto dto = toDto(aiCharacter);
	    dto.setTodayEmojiId(emojiList.getId());
	    System.out.println("무드 스코어 : " + moodchange);
	    return dto;
	}
	
	@Transactional
	public AICharacter cheerCharacter(String header, int addPoints, int moodChange) {
//		User user = userService.getProfile(header);
		Optional<AICharacter> aiCharacterOpt = getCharacter(header);
		 if (aiCharacterOpt.isEmpty()) {
		        throw new RuntimeException("캐릭터가 없어 응원할 수 없습니다.");
		    }
		AICharacter character = aiCharacterOpt.get();

	    if (character.getLastCheer() != null &&
	        character.getLastCheer().toLocalDate().equals(LocalDate.now())) {
	        throw new RuntimeException("오늘은 이미 응원했어요 💖 내일 다시 만나요!");
	    }

	    updateAiCharacter(character, addPoints, moodChange);

	    character.setLastCheer(LocalDateTime.now());
	    return character;
	}

	@Transactional
	public AICharacter updateAiCharacter(AICharacter character, int addPoints, int moodChange) {

		int newPoints = character.getPoints() + addPoints;
		int nextevelPoints = getNextLevelPoints(character.getLevel());
		
		if (newPoints >= nextevelPoints) {
			character.setLevel(character.getLevel() + 1);
			newPoints = newPoints - nextevelPoints;
		}
		character.setPoints(newPoints);
		
		int newMoodscore = Math.min(100, Math.max(0,character.getMoodscore() + moodChange));
		character.setMoodscore(newMoodscore);
		System.out.println(character.getLevel());
		
		return character;
	}
	@Transactional
	public AICharacter setCharacterName(String header, String newName) {
		Optional<AICharacter> aiCharacterOpt = getCharacter(header);
		 if (aiCharacterOpt.isEmpty()) {
		        throw new RuntimeException("캐릭터가 존재하지 않습니다.");
		    }
		 AICharacter character = aiCharacterOpt.get();
		 character.setName(newName);
		 return character;
	}
	
	private int getNextLevelPoints(int currentLevel) {
		return 10 * currentLevel;
	}
	public AICharacterDto toDto (AICharacter aiCharacter) {
		AICharacterDto dto = new AICharacterDto();
		dto.setId(aiCharacter.getId());
		dto.setName(aiCharacter.getName());
		dto.setMoodscore(aiCharacter.getMoodscore());
		dto.setLevel(aiCharacter.getLevel());
		dto.setPoints(aiCharacter.getPoints());
		
		Optional<Long> todayEmoji = diaryRepository.findByUserIdAndDate(aiCharacter.getUser().getId(), LocalDate.now())
				.map(diary -> diary.getEmojiList().getId());
		
		dto.setTodayEmojiId(todayEmoji.orElse(null));
		
		return dto;
	}
	 @Transactional
	    public String chatWithAI(String header, String userMessage) {
	        User user = userService.getProfile(header);
	        AICharacter character = characterRepository.findByUser_Id(user.getId())
	                .orElseThrow(() -> new RuntimeException("Character not found"));

	        // AI 호출 → 캐릭터 정보와 함께
	        String aiResponse = aiService.generateResponse(character, userMessage, user.getNickname());

	        // 캐릭터 상태 업데이트
	        character.setLastAiResponse(aiResponse);
	        characterRepository.save(character);

	        return aiResponse;
	    }
	
	
		}
		 
