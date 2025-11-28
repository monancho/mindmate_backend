package com.mind_mate.home.service;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mind_mate.home.entity.Social;
import com.mind_mate.home.entity.User;
import com.mind_mate.home.repository.SocialRepository;
import com.mind_mate.home.repository.UserRepository;
import com.mind_mate.home.util.jwt.JwtUtil;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialAuthService {
	
	private final UserService userService;
	private final KakaoOauthService kakaoOauthService;
	private final NaverOauthService naverOauthService;
	private final GoogleOauthService googleOauthService;
    private final SocialRepository socialRepository;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    
    @Value("${sns.kakao.redirect-uri-login}")
	private String kakaoRedirectUriLogin;
    @Value("${sns.naver.redirect-uri-login}")
    private String naverRedirectUriLogin;
    @Value("${sns.google.redirect-uri-login}")
    private String googleRedirectUriLogin;
    
    @Value("${sns.kakao.redirect-uri-delete}")
    private String kakaoRedirectUriDelete;
    @Value("${sns.naver.redirect-uri-delete}")
    private String naverRedirectUriDelete;
    @Value("${sns.google.redirect-uri-delete}")
    private String googleRedirectUriDelete;
    
    
    public ResponseEntity<?> kakaoLogin(String code, HttpServletResponse response) {
    	String bearerToken = null;
        try {
        	bearerToken = kakaoOauthService.fetchAccessToken(code, kakaoRedirectUriLogin);
            if (bearerToken == null || bearerToken.isBlank()) {
                throw new IllegalArgumentException("카카오 토큰 발급 실패");
            }
            
            LinkedHashMap<String, Object> kakaoUser = kakaoOauthService.fetchKakaoUser(bearerToken);
            if (kakaoUser == null || !kakaoUser.containsKey("id")) {
                throw new IllegalArgumentException("카카오 사용자 정보를 가져올 수 없습니다.");
            }
            
            String email = kakaoOauthService.extractEmailFromUser(kakaoUser);
            String profileImageUrl = kakaoOauthService.extractProfileImageFromUser(kakaoUser);
            
            Social social = getOrCreateKakaoSocial(kakaoUser);
            
            User user = getOrCreateUserForSocial(social, email, profileImageUrl);
            Map<String, String> result = issueTokensForUser(user, response);

            return new ResponseEntity<>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
        	String msg = e.getMessage();
        	// 회원가입 시도중 이메일 중복일때만 언링크 실행
            if ("이미 이 이메일로 가입된 계정이 있습니다.".equals(msg)) {
                try {
                    if (bearerToken != null && !bearerToken.isBlank()) {
                        kakaoOauthService.unlinkKakaoAccountByToken(bearerToken);
                    }
                } catch (Exception unlinkEx) {
                    // 언링크 실패해도 로그인 실패 응답은 그대로 보냄
                    unlinkEx.printStackTrace();
                }
            }
            
        	return handleSocialLoginException(e);
        } catch (URISyntaxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("카카오 인증 서버 요청 오류");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("카카오 로그인 처리 중 서버 오류");
        }
    }

    public ResponseEntity<?> naverLogin(String code, String state, HttpServletResponse response) {
    	String bearerToken = null;
        try {
        	bearerToken = naverOauthService.fetchAccessToken(code, state, naverRedirectUriLogin);
            if (bearerToken == null || bearerToken.isBlank()) {
                throw new IllegalArgumentException("네이버 토큰 발급 실패");
            }
            
            LinkedHashMap<String, Object> naverUser = naverOauthService.fetchNaverUser(bearerToken);
            
            
            String email = naverOauthService.extractEmailFromUser(naverUser);
            String profileImageUrl = naverOauthService.extractProfileImageFromUser(naverUser);
            
            Social social = getOrCreateNaverSocial(naverUser);
            User user = getOrCreateUserForSocial(social, email, profileImageUrl);
            
            Map<String, String> result = issueTokensForUser(user, response);
            return new ResponseEntity<>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
        	String msg = e.getMessage();
        	
        	if ("이미 이 이메일로 가입된 계정이 있습니다.".equals(msg)) {
                try {
                    if (bearerToken != null && !bearerToken.isBlank()) {
                        naverOauthService.unlinkNaverAccountByToken(bearerToken);
                    }
                } catch (Exception unlinkEx) {
                    // 언링크 실패해도 로그인 실패 응답은 그대로
                    unlinkEx.printStackTrace();
                }
            }
        	
        	return handleSocialLoginException(e);
        } catch (URISyntaxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("네이버 인증 서버 요청 오류");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("네이버 로그인 처리 중 서버 오류");
        }
    }

    public ResponseEntity<?> googleLogin(String code, HttpServletResponse response) {
    	String accessToken = null;
        try {
        	accessToken = googleOauthService.fetchAccessToken(code, googleRedirectUriLogin);
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalArgumentException("구글 토큰 발급 실패");
            }
            LinkedHashMap<String, Object> googleUser =	
            		googleOauthService.fetchGoogleUser(accessToken);
            
            String email = googleOauthService.extractEmailFromUser(googleUser);
            String profileImageUrl = googleOauthService.extractProfileImageFromUser(googleUser);
            Social social = getOrCreateGoogleSocial(googleUser);
            
            User user = getOrCreateUserForSocial(social, email, profileImageUrl);
            Map<String, String> result = issueTokensForUser(user, response);

            return new ResponseEntity<>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
        	String msg = e.getMessage();
        	
        	if ("이미 이 이메일로 가입된 계정이 있습니다.".equals(msg)) {
                try {
                    if (accessToken != null && !accessToken.isBlank()) {
                        googleOauthService.unlinkGoogleAccountByToken(accessToken);
                    }
                } catch (Exception unlinkEx) {
                    unlinkEx.printStackTrace();
                }
            }
        	
        	return handleSocialLoginException(e);
        } catch (URISyntaxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("구글 인증 서버 요청 오류");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("구글 로그인 처리 중 서버 오류");
        }
    }
    
    @Transactional
    public ResponseEntity<?> naverDelete(String header, Map<String, String> body) {
        try {
            // 1) 공통: 토큰 → 유저 조회 + NAVER 연동 여부 확인
            User user = getSocialUser(header, "NAVER");

            // 2) 네이버 전용 파라미터 검증
            String code = requireParam(body, "code", "네이버 code 값이 없습니다.");
            String state = requireParam(body, "state", "네이버 state 값이 없습니다.");

            // 3) 네이버 unlink 전체 처리 (토큰 발급 + 삭제)
            naverOauthService.unlinkNaverAccount(code, state, naverRedirectUriDelete);

            // 4) 우리 서비스 회원탈퇴 처리 (기존 로직 재사용)
            return userService.deleteUser(header);

        } catch (IllegalArgumentException e) {
            // 토큰 없음 / 유저 없음 / NAVER 계정 아님 / 필수 파라미터 없음 등
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return new ResponseEntity<>("네이버 인증 서버 요청 오류", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("네이버 회원탈퇴 처리 중 서버 오류", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    @Transactional
    public ResponseEntity<?> kakaoDelete(String header, Map<String, String> body) {
        try {
            // 1) 우리 서비스에서 KAKAO 연동 유저인지 검증 (공통 헬퍼)
            User user = getSocialUser(header, "KAKAO");

            // 2) 카카오 삭제 플로우에서 전달받은 code
            //    (state 쓰고 싶으면 requireParam 하나 더 추가)
            String code = requireParam(body, "code", "카카오 code 값이 없습니다.");

            // 3) 카카오 unlink 전체 처리 (토큰 발급 + unlink API 호출까지 KakaoOauthService 안에서 처리)
            kakaoOauthService.unlinkKakaoAccount(code, kakaoRedirectUriDelete);

            // 4) 우리 서비스 회원탈퇴 처리 (기존 deleteUser 재사용)
            return userService.deleteUser(header);

        } catch (IllegalArgumentException e) {
            // 토큰 없음, 유저 없음, KAKAO 연동이 아님, code 없음 등
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return new ResponseEntity<>("카카오 인증 서버 요청 오류", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("카카오 회원탈퇴 처리 중 서버 오류", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Transactional
    public ResponseEntity<?> googleDelete(String header, Map<String, String> body) {
        try {
            // 1) 우리 서비스에서 GOOGLE 연동 유저인지 검증
            User user = getSocialUser(header, "GOOGLE");

            // 2) 프론트에서 넘겨준 code
            String code = requireParam(body, "code", "구글 code 값이 없습니다.");

            // 3) 구글 unlink (revoke) 처리
            googleOauthService.unlinkGoogleAccount(code, googleRedirectUriDelete);

            // 4) 우리 서비스 회원탈퇴 처리 (공통 로직)
            return userService.deleteUser(header);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return new ResponseEntity<>("구글 인증 서버 요청 오류", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("구글 회원탈퇴 처리 중 서버 오류", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
//    private LinkedHashMap<String, Object> getKakaoUserByCode(String code) throws URISyntaxException {
//        String bearerToken = kakaoOauthService.fetchAccessToken(code, kakaoRedirectUriLogin);
//        if (bearerToken == null || bearerToken.isBlank()) {
//            throw new IllegalArgumentException("카카오 토큰 발급 실패");
//        }
//
//        LinkedHashMap<String, Object> kakaoUser = kakaoOauthService.fetchKakaoUser(bearerToken);
//        if (kakaoUser == null || !kakaoUser.containsKey("id")) {
//            throw new IllegalArgumentException("카카오 사용자 정보를 가져올 수 없습니다.");
//        }
//
//        return kakaoUser;
//    }

//    private LinkedHashMap<String, Object> getNaverUserByCode(String code, String state) throws URISyntaxException {
//        String bearerToken = naverOauthService.fetchAccessToken(code, state, naverRedirectUriLogin);
//        if (bearerToken == null || bearerToken.isBlank()) {
//            throw new IllegalArgumentException("네이버 토큰 발급 실패");
//        }
//
//        LinkedHashMap<String, Object> naverUserRaw = naverOauthService.fetchNaverUser(bearerToken);
//        if (naverUserRaw == null || !naverUserRaw.containsKey("response")) {
//            throw new IllegalArgumentException("네이버 사용자 정보를 가져올 수 없습니다.");
//        }
//
//        Object responseObj = naverUserRaw.get("response");
//        if (!(responseObj instanceof LinkedHashMap)) {
//            throw new IllegalArgumentException("네이버 사용자 정보 형식이 올바르지 않습니다.");
//        }
//
//        // 여기서부터가 실제 유저 정보 (id, email, nickname 등등)
//        return (LinkedHashMap<String, Object>) responseObj;
//    }

//    private LinkedHashMap<String, Object> getGoogleUserByCode(String code) throws URISyntaxException {
//        String accessToken = googleOauthService.fetchAccessToken(code, googleRedirectUriLogin);
//        if (accessToken == null || accessToken.isBlank()) {
//            throw new IllegalArgumentException("구글 토큰 발급 실패");
//        }
//
//        LinkedHashMap<String, Object> googleUser = googleOauthService.fetchGoogleUser(accessToken);
//        if (googleUser == null || !googleUser.containsKey("sub")) {
//            // sub = 구글 계정 고유 ID
//            throw new IllegalArgumentException("구글 사용자 정보를 가져올 수 없습니다.");
//        }
//
//        return googleUser;
//    }
    
    private Social getOrCreateSocial(String provider, String providerUserId) {
        return socialRepository
                .findByProviderAndProviderUserId(provider, providerUserId)
                .orElseGet(() -> {
                    Social social = new Social();
                    social.setProvider(provider);
                    social.setProviderUserId(providerUserId);
                    return social;
                });
    }

    private Social getOrCreateKakaoSocial(LinkedHashMap<String, Object> kakaoUser) {
        Long kakaoId = ((Number) kakaoUser.get("id")).longValue();
        String providerUserId = String.valueOf(kakaoId);
        return getOrCreateSocial("KAKAO", providerUserId);
    }

    private Social getOrCreateNaverSocial(LinkedHashMap<String, Object> naverUser) {
        String providerUserId = String.valueOf(naverUser.get("id"));
        return getOrCreateSocial("NAVER", providerUserId);
    }

    private Social getOrCreateGoogleSocial(LinkedHashMap<String, Object> googleUser) {
        String providerUserId = String.valueOf(googleUser.get("sub")); // 구글 고유 ID
        return getOrCreateSocial("GOOGLE", providerUserId);
    }

    
    private User getSocialUser(String header, String provider) {
        Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) {
            throw new IllegalArgumentException("토큰 정보가 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Social social = user.getSocial();
        if (social == null || !provider.equalsIgnoreCase(social.getProvider())) {
            throw new IllegalArgumentException(provider + " 연동 계정이 아닙니다.");
        }

        return user;
    }

    // body 에서 필수 파라미터 꺼내기 헬퍼
    private String requireParam(Map<String, String> body, String key, String message) {
        String value = body.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
    
    private User getOrCreateUserForSocial(Social social, String email, String profileImgageUrl) {	
    	User existing = social.getUser();
        if (existing != null) {
            return existing;
        }
        
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                "이메일 정보를 가져올 수 없습니다. 소셜 계정의 이메일 제공에 동의했는지 확인해주세요."
            );
        }
        
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 이 이메일로 가입된 계정이 있습니다.");
        }
        
        User user = new User();
        social.setUser(user);
        user.setSocial(social);
        user.setEmail(email);
        user.setAuthType(social.getProvider());
        
        user.setProfileImageUrl(profileImgageUrl);
//        user.setProfileImageKey(null);
        
        userRepository.save(user);
        return user;
    }

    // UserId 기준 토큰 발급 + Refresh 저장
    private Map<String, String> issueTokensForUser(User user, HttpServletResponse response) {
        Long userId = user.getId();
        if (userId == null) {
            throw new IllegalStateException("유저 ID가 없습니다.");
        }

        String accessToken = jwtUtil.createAccessToken(userId);
        String refreshToken = jwtUtil.createRefreshToken();

        
        refreshTokenService.saveTokenInfo(userId, refreshToken, accessToken);
       
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)    // 로컬 테스트면 false, HTTPS 배포면 true 로
                .sameSite("Lax")  // localhost:3000 ↔ 8888이면 Lax 로 충분
                .path("/")
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());
        
        Map<String, String> result = new HashMap<>();
        result.put("accessToken", accessToken);
//        result.put("refreshToken", refreshToken);

        return result;
    }
    
    // 소셜 전용 이메일 중복 확인 공통 메서드
    private ResponseEntity<?> handleSocialLoginException(IllegalArgumentException e) {
        String msg = e.getMessage();

        if ("이미 이 이메일로 가입된 계정이 있습니다.".equals(msg)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(msg);
        }
        if (msg != null && msg.startsWith("이메일 정보를 가져올 수 없습니다")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg);
    }
}

