package com.mind_mate.home.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import org.springframework.beans.factory.annotation.Value;



@Service
public class KakaoOauthService {
	
	 @Value("${sns.kakao.client.id}")
	    private String clientId;

	    @Value("${sns.kakao.client.secret}")
	    private String clientSecret;

	    @Value("${sns.kakao.token.url}")
	    private String kakaoTokenUrl;

	    @Value("${sns.kakao.me.url}")
	    private String kakaoMeUrl;
    
    
    public String fetchAccessToken(String code, String redirectUri) throws URISyntaxException {
    	RestTemplate restTemplate = new RestTemplate();
    	
    	HttpHeaders headers = new HttpHeaders();
    	headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    	
    	MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    	params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri); // 프론트에서 카카오가 리다이렉트한 주소
        params.add("code", code);
        params.add("client_secret", clientSecret);
        
        HttpEntity<?> http = new HttpEntity<>(params, headers);
        URI uri = new URI(kakaoTokenUrl);
        
        ResponseEntity<LinkedHashMap> response =
        		restTemplate.exchange(uri, HttpMethod.POST, http, LinkedHashMap.class);
        
        Object token = response.getBody().get("access_token");
        return token != null ? "Bearer " + token.toString() : null;
    }
    
    public LinkedHashMap<String, Object> fetchKakaoUser(String bearerToken) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearerToken);

        HttpEntity<?> http = new HttpEntity<>(headers);
        URI uri = new URI(kakaoMeUrl);

        ResponseEntity<LinkedHashMap> response =
                restTemplate.exchange(uri, HttpMethod.GET, http, LinkedHashMap.class);

        return response.getBody();
    }
    
    public void unlinkKakaoAccount(String code, String redirectUri) throws URISyntaxException {
        // 1) code + redirectUri로 access token 발급
        String bearerToken = fetchAccessToken(code, redirectUri);
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalArgumentException("카카오 토큰 발급 실패");
        }

        // 2) 카카오 unlink API 호출
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearerToken); 
        HttpEntity<?> http = new HttpEntity<>(headers);

        URI uri = new URI("https://kapi.kakao.com/v1/user/unlink");

        ResponseEntity<LinkedHashMap> response =
                restTemplate.postForEntity(uri, http, LinkedHashMap.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("카카오 unlink 실패: " + response.getStatusCode());
        }
    }
    
    public void unlinkKakaoAccountByToken(String bearerToken) throws URISyntaxException {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalArgumentException("카카오 토큰이 없습니다.");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearerToken); 
        HttpEntity<?> http = new HttpEntity<>(headers);

        URI uri = new URI("https://kapi.kakao.com/v1/user/unlink");

        ResponseEntity<LinkedHashMap> response =
                restTemplate.postForEntity(uri, http, LinkedHashMap.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("카카오 unlink 실패: " + response.getStatusCode());
        }
    }
    @SuppressWarnings("unchecked")
    public String extractEmailFromUser(LinkedHashMap<String, Object> kakaoUser) {
        Object accountObj = kakaoUser.get("kakao_account");
        if (!(accountObj instanceof LinkedHashMap)) {
            // kakao_account 없으면 이메일 동의 안 했거나 구조 바뀐 경우
            return null;
        }

        LinkedHashMap<String, Object> account = (LinkedHashMap<String, Object>) accountObj;
        Object emailObj = account.get("email");
        return emailObj != null ? emailObj.toString() : null;
    }
    
    @SuppressWarnings("unchecked")
    public String extractProfileImageFromUser(LinkedHashMap<String, Object> kakaoUser) {
        if (kakaoUser == null) {
            return null;
        }

        Object accountObj = kakaoUser.get("kakao_account");
        if (!(accountObj instanceof LinkedHashMap)) {
            return null;
        }

        LinkedHashMap<String, Object> account = (LinkedHashMap<String, Object>) accountObj;
        Object profileObj = account.get("profile");
        if (!(profileObj instanceof LinkedHashMap)) {
            return null;
        }

        LinkedHashMap<String, Object> profile = (LinkedHashMap<String, Object>) profileObj;
        Object urlObj = profile.get("profile_image_url"); // thumbnail_image_url 써도 OK

        if (urlObj == null) {
            return null;
        }
        String url = urlObj.toString();
        return url.isBlank() ? null : url;
    }
}
