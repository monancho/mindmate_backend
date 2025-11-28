package com.mind_mate.home.controller;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mind_mate.home.dto.AICharacterDto;
import com.mind_mate.home.dto.DiaryCsvDto;
import com.mind_mate.home.dto.DiaryDto;
import com.mind_mate.home.dto.DiaryEmojiDto;
import com.mind_mate.home.dto.DiaryEmotionDto;
import com.mind_mate.home.dto.EmojiDto;
import com.mind_mate.home.entity.AIResult;
import com.mind_mate.home.entity.EmojiList;
import com.mind_mate.home.entity.User;
import com.mind_mate.home.repository.EmojiListRepository;
import com.mind_mate.home.service.AICharacterService;
import com.mind_mate.home.service.AIService;
import com.mind_mate.home.service.DiaryService;
import com.mind_mate.home.util.jwt.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;



@RestController
@RequestMapping("/api/diary")
//@CrossOrigin(origins = "http://localhost:3000")
public class DiaryController {

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private AIService aiService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    EmojiListRepository emojiListRepository;
    
    @Autowired
    AICharacterService aiCharacterService;
    // -----------------------------
    // 1. 새 일기 작성
    // (Service가 DiaryDto를 반환하므로, Controller도 DiaryDto를 반환하도록 수정)
    // -----------------------------
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDiaryWithImage(
            @RequestPart("data") @Valid String diaryJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestHeader("Authorization") String authHeader) throws IOException {

        User user = jwtUtil.findUserEntityByHeader(authHeader)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        DiaryDto diaryDto = objectMapper.readValue(diaryJson, DiaryDto.class);
        diaryDto.setUserId(user.getId());
        diaryDto.setNickname(user.getNickname());

        DiaryDto createdDiary = diaryService.createDiaryWithImage(user.getId(), diaryDto, imageFile);

        Map<String, Object> response = Map.of("diary", createdDiary);
        return ResponseEntity.ok(response);
    }
    // -----------------------------
    // 1. 특정 날짜 일기 수정 (이미지 포함)
    // -----------------------------
    @PutMapping("/edit")
    public ResponseEntity<DiaryDto> updateDiaryWithImage(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestPart("data") @Valid String diaryJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestHeader("Authorization") String authHeader) throws IOException {

        // JWT에서 사용자 엔티티 조회
        User user = jwtUtil.findUserEntityByHeader(authHeader)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 서비스에 JSON + 이미지 전달
        DiaryDto updatedDiary = diaryService.updateDiaryWithImageFromJson(user, date, diaryJson, imageFile);

        return ResponseEntity.ok(updatedDiary);
    }


    // -----------------------------
    // 3. 특정 날짜 일기 조회 (DTO 반환)
    // -----------------------------
    @GetMapping("/date")
    public ResponseEntity<DiaryDto> getDiaryByDate(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestHeader("Authorization") String authHeader) {

        User user = jwtUtil.findUserEntityByHeader(authHeader)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return diaryService.getDiaryDtoByDate(user.getId(), date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // -----------------------------
    // 4. 월별 일기 조회 (캘린더용, 날짜와 이모지 정보만 반환)
    // (Service가 DiaryDto 리스트를 반환하므로, 이를 EmojiDto 리스트로 변환하여 반환)
    // -----------------------------
    @GetMapping("/month")
    public ResponseEntity<List<EmojiDto>> getDiariesForMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @RequestHeader("Authorization") String authHeader) {

        User user = jwtUtil.findUserEntityByHeader(authHeader)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // DiaryDto 리스트를 가져옴
        List<DiaryDto> diaryDtos = diaryService.getDiariesByYearAndMonth(user.getId(), year, month);
        
        // 캘린더 표시를 위해 날짜와 이모지 정보만 포함된 EmojiDto 리스트로 변환
        List<EmojiDto> dtoList = diaryDtos.stream()
                //.filter(dto -> dto.getEmoji() != null) // 이모지 정보가 있는 일기만 필터링
                .map(dto -> {
                	 EmojiDto emojiDto = dto.getEmoji() != null ? dto.getEmoji() : new EmojiDto();
                    emojiDto.setDate(dto.getDate()); // 이모지 DTO에 날짜 추가
                    return emojiDto;
                })
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(dtoList);
    }

    // -----------------------------
    // 5. 모든 일기 조회 (주석 처리)
    // -----------------------------
//    @GetMapping
//    public ResponseEntity<List<Diary>> getAllDiary(@RequestHeader("Authorization") String authHeader) {
//        User user = jwtUtil.findUserEntityByHeader(authHeader)
//                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
//        List<Diary> diaries = diaryService.getDiaryList(user.getId());
//        return ResponseEntity.ok(diaries);
//    }

    // -----------------------------
    // 6. 특정 날짜 일기 삭제
    // -----------------------------
    @DeleteMapping("/date/{date}")
    public ResponseEntity<Void> deleteDiary(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader("Authorization") String authHeader) {

        User user = jwtUtil.findUserEntityByHeader(authHeader)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        diaryService.deleteByDate(user.getId(), date);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------
    // 7. CSV 내보내기
    // -----------------------------
    @GetMapping("/week/csv")
    public List<DiaryCsvDto> getWeeklyDiaryCsv(
            @RequestHeader("Authorization") String header,
            @RequestParam(value="start", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value="end", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        LocalDate today = LocalDate.now();
        if (start == null) start = today.with(DayOfWeek.MONDAY);
        if (end == null) end = today.with(DayOfWeek.SUNDAY);

        return diaryService.getDiaryCsvData(header, start, end);
    }
    // -----------------------------
    // 8. 주별 감정 통계
    // -----------------------------
//    @GetMapping("/week")
//    public ResponseEntity<Map<String, Object>> getWeekEmotion(
//            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
//            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
//            @RequestHeader("Authorization") String authHeader) {
//
//        User user = jwtUtil.findUserEntityByHeader(authHeader)
//                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
//
//        // 시작일/종료일이 명시되지 않으면 현재 주의 월요일/일요일로 설정
//        LocalDate now = LocalDate.now();
//        start = (start != null) ? start : now.with(DayOfWeek.MONDAY);
//        end = (end != null) ? end : now.with(DayOfWeek.SUNDAY);
//
//        List<DiaryEmotionDto> dtos = diaryService.getEmotionByWeek(user.getId(), start, end);
//
//        AIResult aiResult;
//        try {
//            // 주별 감정 통계를 바탕으로 AI 결과 생성
//            aiResult = aiService.generateWeeklyResult(start, end, dtos);
//        } catch (Exception e) {
//            aiResult = new AIResult();
//            aiResult.setAicomment("AI 코멘트 생성 실패");
//        }
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("weeklyCounts", dtos);
//        response.put("aiComment", aiResult.getAicomment());
//
//        return ResponseEntity.ok(response);
//    }
//    
//@GetMapping("/week")
//public ResponseEntity<?> getWeekEmotion(@RequestParam(value="start", required=false) LocalDate start,
//        @RequestParam(value="end", required=false) LocalDate end) {
//	start =(start != null) ? start : LocalDate.now().with(DayOfWeek.MONDAY);
//	end =(end != null) ? end : LocalDate.now().with(DayOfWeek.SUNDAY);
//	
//    List<DiaryEmotionDto> dtos = diaryService.getEmotionByWeek(start, end);
//    
//    AIResult aiResult;
//    try {
//        aiResult = aiService.generateWeeklyResult(start, end, dtos);
//    } catch (Exception e) {
//        aiResult = new AIResult();
//        aiResult.setAicomment("AI 코멘트 생성 실패");
//    }
//
//    Map<String, Object> response = new HashMap<>();
//    response.put("weeklyCounts", dtos);
//    response.put("aiComment", aiResult.getAicomment());
//
//    return ResponseEntity.ok(response);
//}
@GetMapping("/week/test")
public Map<String, Object> getWeeklyMood(
		@RequestHeader("Authorization") String header,
		@RequestParam(value="start", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam(value="end", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
	LocalDate today = LocalDate.now();
	if (start == null) start = today.with(DayOfWeek.MONDAY);
	if (end == null) end = today.with(DayOfWeek.SUNDAY);
    // 1️⃣ 날짜별 감정 데이터
    List<DiaryEmojiDto> dailyEmotions = diaryService.getEmotionByDate(header, start, end);

    // 2️⃣ AI 주간 코멘트
    List<DiaryEmotionDto> weeklyCounts = diaryService.getEmotionByWeek(header,start, end); 
    AIResult aiResult = aiService.generateWeeklyResult(header,start, end, weeklyCounts);

    // 3️⃣ 응답
    Map<String, Object> response = new HashMap<>();
    response.put("dailyEmotions", dailyEmotions);
    response.put("weeklyCounts", weeklyCounts);
    response.put("aiComment", aiResult.getAicomment());

    return response;
}

}
	
