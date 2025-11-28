package com.mind_mate.home;

import org.junit.jupiter.api.Test;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class GeminiTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String apikey= System.getenv("GEMINI_API_KEY");
		if (apikey == null || apikey.isEmpty()) {
			apikey = "abcdccdcdc";
		}
		try {
			Client geminiClient = Client.builder().apiKey(apikey).build();
			
			String prompt = "테스트 : 오늘 온도는 몇도야?";
			GenerateContentResponse response = geminiClient.models.generateContent("gemini-2.5-flash", prompt, null);
			System.out.println("연결 성공");
			System.out.println("응답 : " + response.text());
		} catch (Exception e) {
			// TODO: handle exception
			System.err.println("연결 실패");
			e.printStackTrace();
		}
	}

}
