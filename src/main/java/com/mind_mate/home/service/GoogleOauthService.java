package com.mind_mate.home.service;

import java.net.URI;
import java.net.URISyntaxException;
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
public class GoogleOauthService {

    @Value("${sns.google.client.id}")
    private String clientId;

    @Value("${sns.google.client.secret}")
    private String clientSecret;

    @Value("${sns.google.token.url}")
    private String googleTokenUrl;

    @Value("${sns.google.me.url}")
    private String googleMeUrl;

    
    public String fetchAccessToken(String code, String redirectUri) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", redirectUri);

        HttpEntity<?> http = new HttpEntity<>(params, headers);
        URI uri = new URI(googleTokenUrl);

        ResponseEntity<LinkedHashMap> response =
                restTemplate.exchange(uri, HttpMethod.POST, http, LinkedHashMap.class);

        Object token = response.getBody().get("access_token");
        return token != null ? token.toString() : null;
    }

   
    public LinkedHashMap<String, Object> fetchGoogleUser(String accessToken) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> http = new HttpEntity<>(headers);
        URI uri = new URI(googleMeUrl);

        ResponseEntity<LinkedHashMap> response =
                restTemplate.exchange(uri, HttpMethod.GET, http, LinkedHashMap.class);

        return response.getBody();
    }
    public void unlinkGoogleAccount(String code, String redirectUri) throws URISyntaxException {
        // 1) code → access_token
        String accessToken = fetchAccessToken(code, redirectUri);
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("구글 토큰 발급 실패");
        }

        // 2) revoke 호출
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", accessToken);

        HttpEntity<?> http = new HttpEntity<>(params, headers);
        URI uri = new URI("https://oauth2.googleapis.com/revoke");

        ResponseEntity<String> response =
                restTemplate.postForEntity(uri, http, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("구글 unlink 실패: " + response.getStatusCode());
        }
    }
    
    public void unlinkGoogleAccountByToken(String accessToken) throws URISyntaxException {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("구글 토큰이 없습니다.");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", accessToken);   // ❗ Bearer 없이 순수 토큰

        HttpEntity<?> http = new HttpEntity<>(params, headers);
        URI uri = new URI("https://oauth2.googleapis.com/revoke");

        ResponseEntity<String> response =
                restTemplate.postForEntity(uri, http, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("구글 unlink 실패: " + response.getStatusCode());
        }
    }
    
    public String extractEmailFromUser(LinkedHashMap<String, Object> googleUser) {
        if (googleUser == null) {
            return null;
        }
        Object emailObj = googleUser.get("email"); // 구글 userinfo 응답 구조: sub, email, name ...
        if (emailObj == null) {
            return null;
        }
        String email = emailObj.toString();
        return email.isBlank() ? null : email;
    }
    
    public String extractProfileImageFromUser(LinkedHashMap<String, Object> googleUser) {
        if (googleUser == null) {
            return null;
        }
        Object pictureObj = googleUser.get("picture");
        if (pictureObj == null) {
            return null;
        }
        String url = pictureObj.toString();
        return url.isBlank() ? null : url;
    }
}
