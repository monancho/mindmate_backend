package com.mind_mate.home.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import com.mind_mate.home.dto.UserRequestDto;
import com.mind_mate.home.dto.UserResponseDto;
import com.mind_mate.home.entity.AICharacter;
import com.mind_mate.home.entity.AIResult;
import com.mind_mate.home.entity.Account;
import com.mind_mate.home.entity.Social;
import com.mind_mate.home.entity.User;
import com.mind_mate.home.repository.AICharacterRepository;
import com.mind_mate.home.repository.AIRepository;
import com.mind_mate.home.repository.AccountRepository;
import com.mind_mate.home.repository.DiaryRepository;
import com.mind_mate.home.repository.SocialRepository;
import com.mind_mate.home.repository.UserRepository;
import com.mind_mate.home.util.jwt.JwtUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
	private final SocialRepository socialRepository;
	private final AccountRepository accountRepository;
	private final DiaryRepository diaryRepository;
	private final AIRepository aiRepository;
	private final AICharacterRepository aiCharacterRepository;
	private final RefreshTokenService refreshTokenService;
	private final JwtUtil jwtUtil;
	private final AIService aiService;
	private final S3Service s3Service;
	
	public ResponseEntity<?> checkNickname (String nickname) {
		Optional<User> _user = userRepository.findByNickname(nickname);
		if (_user.isEmpty()) {
            return new ResponseEntity<>("사용 가능한 닉네임입니다", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("사용중인 닉네임입니다", HttpStatus.CONFLICT);
        }
	}
	public ResponseEntity<?> setProfile (String header, UserRequestDto body) {
		Long userId = jwtUtil.findUserIdByHeader(header);
		if (userId == null) {
            return new ResponseEntity<>("토큰 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
		Optional<User>_user = userRepository.findById(userId);
	
		if (_user.isEmpty()) {
            return new ResponseEntity<>("유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
	
		User user = _user.get();
		String currentNickname = user.getNickname();
		
		String nickname = body.getNickname();
		LocalDate birth_date = body.getBirth_date();
		String mbti = body.getMbti();
		String email = user.getEmail();
		
		if(currentNickname == null || !currentNickname.equals(nickname)) {
			
			if(userRepository.findByNickname(nickname).isPresent()) {
				return new ResponseEntity<>("사용중인 닉네임입니다.", HttpStatus.CONFLICT);
			}
		}
		
		
		user.setNickname(nickname);
		user.setBirth_date(birth_date);
		user.setMbti(mbti);
		userRepository.save(user);
		
		
		 Optional<AICharacter> existingChar = aiCharacterRepository.findByUser_Id(user.getId());
		    if (existingChar.isEmpty()) {
		        AICharacter aiChar = new AICharacter();
		        aiChar.setUser(user);
		        aiChar.setName("이름을 지어주세요"); // 필요하면 랜덤 이름
		        aiChar.setLevel(1);
		        aiChar.setPoints(0);
		        aiChar.setMoodscore(50); // 기본 moodscore
		        aiCharacterRepository.save(aiChar);
		    }
		

		UserResponseDto responseDto = new UserResponseDto(userId ,nickname, birth_date, mbti, user.getAuthType(), user.getRole(), user.getProfileImageUrl(), user.getEmail());

		return new ResponseEntity<>(responseDto, HttpStatus.OK);
	}
	public User getProfile (String header) {
		//Long userId = jwtUtil.findUserIdByHeader(header);
		Long userId = jwtUtil.findUserIdByHeader(header);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다. ID: " + userId));
		
		return user;
	}
	
	@Transactional
	public ResponseEntity<?> deleteUser (String header) {
		String accessToken = jwtUtil.findTokenByHeader(header);
		if (accessToken != null && !accessToken.isBlank()) {
			try {
	            refreshTokenService.removeRefreshToken(accessToken);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
		
		Long userId = jwtUtil.findUserIdByHeader(header);
		if (userId == null) {
            return new ResponseEntity<>("토큰 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
		Optional<User>_user = userRepository.findById(userId);
	
		if (_user.isEmpty()) {
            return new ResponseEntity<>("유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
		
		User user = _user.get();
		
		Account account = user.getAccount();
		Social social = user.getSocial();
		
		// 회원 탈퇴 시 해당 유저 프로필, 다이어리 이미지 전체 삭제
		try {
	        s3Service.deleteAllUserImages(userId);
	    } catch (Exception e) {
	        // S3 삭제 실패해도 회원 탈퇴는 계속 진행
	        e.printStackTrace();
	    }
		
		user.setBirth_date(null);
		user.setMbti(null);
		user.setProfileImageUrl(null);
		user.setEmail("@deleteEmail#"+userId);
		user.setNickname("탈퇴회원#" +userId);
		
		if (social != null ) {
			socialRepository.delete(social);
			user.setSocial(null);
		}
		
		if (account != null) {
			accountRepository.delete(account);
			user.setAccount(null);
		}
		
		diaryRepository.deleteAllByUser(user);
		aiRepository.deleteAllByUser(user);
		aiCharacterRepository.deleteByUser(user);
			
		userRepository.save(user);
		return new ResponseEntity<>("회원 탈퇴가 완료됐습니다.", HttpStatus.OK);
	}
	
	
	public AIResult getDailyTest(String header, String content) {
		return aiService.generatResult("daily_test",content);
	}
	public AIResult getDailyResult(String header, String content) {
		return aiService.generatResult("daily_result",content);
	}
	public AIResult getFortune(String header, String content) {
		return aiService.generatResult("fortune",content);
	}
	
	@Transactional
    public ResponseEntity<?> uploadProfileImage(String header, MultipartFile file) {
        Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) {
            return new ResponseEntity<>("토큰 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        if (file == null || file.isEmpty()) {
            return new ResponseEntity<>("파일이 비어 있습니다.", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        try {
            // 기존 이미지가 있으면 S3에서 먼저 삭제
            String oldUrl = user.getProfileImageUrl();
            if (oldUrl != null && !oldUrl.isBlank()) {
                s3Service.deleteFileByUrl(oldUrl);
            }

            // S3에 새 이미지 업로드
            String newUrl = s3Service.uploadProfileImage(userId, file);

            // DB에 URL 저장
            user.setProfileImageUrl(newUrl);
            userRepository.save(user);

            UserResponseDto dto = new UserResponseDto(
                    user.getId(),
                    user.getNickname(),
                    user.getBirth_date(),
                    user.getMbti(),
                    user.getAuthType(),
                    user.getRole(),
                    user.getProfileImageUrl(),
                    user.getEmail()
            );

            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("이미지 업로드 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
	
	@Transactional
    public ResponseEntity<?> deleteProfileImage(String header) {
        Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) {
            return new ResponseEntity<>("토큰 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        String imageUrl = user.getProfileImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            return new ResponseEntity<>("등록된 프로필 이미지가 없습니다.", HttpStatus.BAD_REQUEST);
        }

        try {
            // S3에서 삭제
            s3Service.deleteFileByUrl(imageUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("이미지 삭제 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // DB에서 URL 지우기
        user.setProfileImageUrl(null);
        userRepository.save(user);

        UserResponseDto dto = new UserResponseDto(
                user.getId(),
                user.getNickname(),
                user.getBirth_date(),
                user.getMbti(),
                user.getAuthType(),
                user.getRole(),
                user.getProfileImageUrl(), // null
                user.getEmail()
        );

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
}

