package com.mind_mate.home.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class NaverOauthService {
	@Value("${sns.naver.client.id}")
    private String clientId;

    @Value("${sns.naver.client.secret}")
    private String clientSecret;

    @Value("${sns.naver.token.url}")
    private String naverTokenUrl;

    @Value("${sns.naver.me.url}")
    private String naverMeUrl;
    
    public void unlinkNaverAccount(String code, String state, String redirectUri) throws URISyntaxException {
        // 1) 먼저 기존 로직 그대로 access token 발급 (Bearer xxxxx 형식)
        String bearerToken = fetchAccessToken(code, state, redirectUri);
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalArgumentException("네이버 토큰 발급 실패");
        }

        // 2) "Bearer " 제거해서 순수 access_token만 추출
        String accessToken = bearerToken;
        if (accessToken.toLowerCase().startsWith("bearer ")) {
            accessToken = accessToken.substring("bearer ".length()).trim();
        }

        // 3) URL 인코딩
        String encodedAccessToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        String encodedClientId = URLEncoder.encode(clientId, StandardCharsets.UTF_8);
        String encodedClientSecret = URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

        // 4) 네이버 토큰 삭제 API 호출
        //    naverTokenUrl 예: https://nid.naver.com/oauth2.0/token
        String url = naverTokenUrl
                + "?grant_type=delete"
                + "&client_id=" + encodedClientId
                + "&client_secret=" + encodedClientSecret
                + "&access_token=" + encodedAccessToken
                + "&service_provider=NAVER";

        RestTemplate restTemplate = new RestTemplate();
        URI uri = new URI(url);

        ResponseEntity<LinkedHashMap> response =
                restTemplate.getForEntity(uri, LinkedHashMap.class);

        LinkedHashMap body = response.getBody();
        Object result = (body != null) ? body.get("result") : null;

        // 5) 실패 시 예외 던지기 → SocialAuthService에서 catch 처리
        if (!"success".equals(result)) {
            throw new IllegalStateException("네이버 토큰 삭제 실패: result=" + result);
        }
    }
    
    public void unlinkNaverAccountByToken(String bearerToken) throws URISyntaxException {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalArgumentException("네이버 토큰이 없습니다.");
        }

        // "Bearer " 제거해서 순수 access_token만 추출
        String accessToken = bearerToken;
        if (accessToken.toLowerCase().startsWith("bearer ")) {
            accessToken = accessToken.substring("bearer ".length()).trim();
        }

        String encodedAccessToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        String encodedClientId = URLEncoder.encode(clientId, StandardCharsets.UTF_8);
        String encodedClientSecret = URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

        String url = naverTokenUrl
                + "?grant_type=delete"
                + "&client_id=" + encodedClientId
                + "&client_secret=" + encodedClientSecret
                + "&access_token=" + encodedAccessToken
                + "&service_provider=NAVER";

        RestTemplate restTemplate = new RestTemplate();
        URI uri = new URI(url);

        ResponseEntity<LinkedHashMap> response =
                restTemplate.getForEntity(uri, LinkedHashMap.class);

        LinkedHashMap body = response.getBody();
        Object result = (body != null) ? body.get("result") : null;

        if (!"success".equals(result)) {
            throw new IllegalStateException("네이버 토큰 삭제 실패: result=" + result);
        }
    }
    
    public String fetchAccessToken(String code, String state, String redirectUri) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code"); // 고정
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("state", state);                      // ⭐ 네이버는 state 필수
        params.add("redirect_uri", redirectUri);

        HttpEntity<?> http = new HttpEntity<>(params, headers);
        URI uri = new URI(naverTokenUrl);

        ResponseEntity<LinkedHashMap> response =
                restTemplate.exchange(uri, HttpMethod.POST, http, LinkedHashMap.class);

        Object token = response.getBody().get("access_token");
        return token != null ? "Bearer " + token.toString() : null;
    }

    public LinkedHashMap<String, Object> fetchNaverUser(String bearerToken) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearerToken);

        HttpEntity<?> http = new HttpEntity<>(headers);
        URI uri = new URI(naverMeUrl);

        ResponseEntity<LinkedHashMap> response =
                restTemplate.exchange(uri, HttpMethod.GET, http, LinkedHashMap.class);
        
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> body =
                (LinkedHashMap<String, Object>) response.getBody();

        if (body == null || !body.containsKey("response")) {
            throw new IllegalArgumentException("네이버 사용자 정보를 가져올 수 없습니다.");
        }

        Object responseObj = body.get("response");
        if (!(responseObj instanceof LinkedHashMap)) {
            throw new IllegalArgumentException("네이버 사용자 정보 형식이 올바르지 않습니다.");
        }
        
        return (LinkedHashMap<String, Object>) responseObj;
    }
    
    public String extractEmailFromUser(LinkedHashMap<String, Object> naverUser) {
        if (naverUser == null) {
            return null;
        }
        Object emailObj = naverUser.get("email");  // response 안의 email
        if (emailObj == null) {
            return null;
        }
        String email = emailObj.toString();
        return email.isBlank() ? null : email;
    }
    
    public String extractProfileImageFromUser(LinkedHashMap<String, Object> naverUser) {
        if (naverUser == null) {
            return null;
        }
        Object imageObj = naverUser.get("profile_image");
        if (imageObj == null) {
            return null;
        }
        String url = imageObj.toString();
        return url.isBlank() ? null : url;
    }
}
