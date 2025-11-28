package com.mind_mate.home.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.google.genai.Client;

@Configuration
public class AIConfig {

	@Autowired
	Environment env;
	@Value("${gemini.api.key}")
	private String apikey;
	
	public String getApiKey() {
		return env.getProperty("GEMINI_API_KEY");
	}
	
	@Bean
	public Client geminiClient() {
		if (apikey == null || apikey.isEmpty()) {
			throw new IllegalStateException("GEMINI_API_KEY 환경 변수가 설정되어 있지 않습니다");
		}
		return new Client.Builder().apiKey(apikey).build();
	}
}
