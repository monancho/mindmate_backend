package com.mind_mate.home.service;


import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mind_mate.home.controller.EmojiController;
import com.mind_mate.home.dto.DiaryCsvDto;
import com.mind_mate.home.dto.DiaryDto;
import com.mind_mate.home.dto.DiaryEmojiDto;
import com.mind_mate.home.dto.DiaryEmotionDto;
import com.mind_mate.home.dto.EmojiDto;
import com.mind_mate.home.dto.UserResponseDto;
import com.mind_mate.home.entity.AIResult;
import com.mind_mate.home.entity.Diary;
import com.mind_mate.home.entity.User;
import com.mind_mate.home.repository.DiaryRepository;
import com.mind_mate.home.repository.EmojiListRepository;
import com.mind_mate.home.repository.UserRepository;
import com.mind_mate.home.util.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DiaryService {
//	@Value("${file.upload-dir}")
//    private String uploadDir; // application.properties에서 읽음
	
    private final EmojiController emojiController;
    private static final Logger logger = LoggerFactory.getLogger(DiaryService.class);

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final EmojiListRepository emojiListRepository;
    private final AIService aiService;
    private final JwtUtil jwtUtil;
    private final S3Service s3Service;
    
    @Transactional(readOnly = true)
    private DiaryDto convertToDto(Diary diary) {
        DiaryDto dto = new DiaryDto();
        dto.setTitle(diary.getTitle());
        dto.setContent(diary.getContent());
        dto.setDate(diary.getDate());

        User user = diary.getUser();
        if (user != null) {
            dto.setUserId(user.getId());
            
            dto.setNickname(user.getNickname());
        }

        if (diary.getEmojiList() != null) {
            EmojiDto emojiDto = new EmojiDto();
            emojiDto.setId(diary.getEmojiList().getId());
            emojiDto.setType(diary.getEmojiList().getType());
            emojiDto.setImageUrl(diary.getEmojiList().getImageUrl());
            dto.setEmoji(emojiDto);
        }

        if (diary.getAiresult() != null) {
            dto.setAiComment(diary.getAiresult().getAicomment());
        }
        // ✅ 여기에 Diary 엔티티의 imageUrl도 DTO에 세팅
        dto.setImageUrl(diary.getImageUrl());

        return dto;
    }

    // -----------------------------
    // 유효한 사용자 가져오기
    // -----------------------------
    private User getUserOrThrow(Long userId) {
        if (userId == null) throw new IllegalArgumentException("UserId must not be null");
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
    private String extractEnglishWord(String aiText) {
        Pattern pattern = Pattern.compile("영어단어:\\s*(\\w+)");
        Matcher matcher = pattern.matcher(aiText);
        if (matcher.find()) {
            return matcher.group(1); // 여기서 "relax"가 나옴
        }
        return null; // 영어단어가 없으면 null
    }
    // -----------------------------
    // AI 코멘트 포함 일기 저장
    // -----------------------------
    private Diary saveDiaryWithAIComment(Diary diary) {
        try {
            AIResult aiResult = aiService.generatResult("diary", diary.getContent());
            diary.setAiresult(aiResult);
            String englishWord = extractEnglishWord(aiResult.getAicomment());
            if (englishWord != null) {
                emojiListRepository.findByType(englishWord)
                    .ifPresent(diary::setEmojiList); // DB에만 연결
            } else {
                diary.setEmojiList(null);
            }
        } catch (Exception e) {
            logger.error("AI 코멘트 생성 실패 (일기 저장은 계속 진행): " + e.getMessage());
            diary.setAiresult(null);
        }
        return diaryRepository.save(diary);
    }

    // -----------------------------
    // 새 일기 작성
    // -----------------------------
    @Transactional
    public DiaryDto createDiaryWithImage(Long userId, DiaryDto diaryDto, MultipartFile imageFile) throws IOException {
        User user = getUserOrThrow(userId);

        Diary diary = new Diary();
        diary.setUser(user);
        diary.setDate(diaryDto.getDate());
        diary.setTitle(diaryDto.getTitle());
        diary.setContent(diaryDto.getContent());
//
//        if (diaryDto.getEmoji() != null && diaryDto.getEmoji().getId() != null) {
//            emojiListRepository.findById(diaryDto.getEmoji().getId())
//                    .ifPresent(diary::setEmojiList);
//        }

        // -----------------------------
        // 이미지 파일 처리
        // -----------------------------
//        if (imageFile != null && !imageFile.isEmpty()) {
//            String original = imageFile.getOriginalFilename();
//            String ext = original != null && original.contains(".") ? 
//                         original.substring(original.lastIndexOf(".")) : "";
//            String filename = UUID.randomUUID() + ext;
//
//            // 절대 경로로 폴더 생성
//            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
//            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
//
//            Path filePath = uploadPath.resolve(filename);
//            imageFile.transferTo(filePath.toFile());
//
//            // DiaryDto에 URL 저장
//            diary.setImageUrl("/uploads/" + filename);
//        }
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = s3Service.uploadDiaryImage(userId, imageFile);
            diary.setImageUrl(imageUrl);
        }

        Diary saved = saveDiaryWithAIComment(diary);
        return convertToDto(saved);
    }

    
 // 일기 수정 (이미지 포함, 기존 값 유지)
    @Transactional
    public DiaryDto updateDiaryWithImageFromJson(User user, LocalDate date, String diaryJson, MultipartFile imageFile) throws IOException {
        // JSON -> DTO 변환
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        DiaryDto diaryDto = objectMapper.readValue(diaryJson, DiaryDto.class);

        diaryDto.setUserId(user.getId());
        diaryDto.setNickname(user.getNickname());

        // 기존 일기 조회
        Diary existingDiary = diaryRepository.findByUserIdAndDate(user.getId(), date)
                .orElseThrow(() -> new RuntimeException("수정할 일기가 없습니다."));
        boolean isContentChanged = diaryDto.getContent() != null 
                && !diaryDto.getContent().trim().isEmpty()
                && !diaryDto.getContent().equals(existingDiary.getContent());

        // -------------------------
        // 제목, 내용 수정 (null/빈값 체크)
        // -------------------------
        if (diaryDto.getTitle() != null && !diaryDto.getTitle().trim().isEmpty()) {
            existingDiary.setTitle(diaryDto.getTitle());
        }
        if (diaryDto.getContent() != null && !diaryDto.getContent().trim().isEmpty()) {
            existingDiary.setContent(diaryDto.getContent());
        }

        // -------------------------
        // 이모지 수정 (null이면 기존 유지)
        // -------------------------
        if (diaryDto.getEmoji() != null && diaryDto.getEmoji().getId() != null) {
            emojiListRepository.findById(diaryDto.getEmoji().getId())
                    .ifPresentOrElse(existingDiary::setEmojiList, () -> existingDiary.setEmojiList(null));
        }
        // null이면 기존 이모지 유지

     // 이미지 처리 (수정 + 기존 파일 삭제)
//        if ((imageFile != null && !imageFile.isEmpty()) || diaryDto.isDeleteImage()) {
//
//            // 기존 이미지 삭제
//            if (existingDiary.getImageUrl() != null && !existingDiary.getImageUrl().isEmpty()) {
//                Path oldFile = Paths.get(uploadDir).resolve(
//                        existingDiary.getImageUrl().replace("/uploads/", "")
//                );
//                Files.deleteIfExists(oldFile);
//                existingDiary.setImageUrl(null); // DB에서도 제거
//            }
//
//            // 새 이미지가 있으면 저장
//            if (imageFile != null && !imageFile.isEmpty()) {
//                String original = imageFile.getOriginalFilename();
//                String ext = (original != null && original.contains(".")) ?
//                        original.substring(original.lastIndexOf(".")) : "";
//                String filename = UUID.randomUUID() + ext;
//
//                Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
//                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
//
//                Path newPath = uploadPath.resolve(filename);
//                imageFile.transferTo(newPath.toFile());
//
//                existingDiary.setImageUrl("/uploads/" + filename);
//            }
//        }
        // 이미지 없으면 기존 imageUrl 유지
        
        // 이미지 처리
        if ((imageFile != null && !imageFile.isEmpty()) || diaryDto.isDeleteImage()) {

            // 기존 S3 이미지 삭제
            if (existingDiary.getImageUrl() != null && !existingDiary.getImageUrl().isEmpty()) {
                s3Service.deleteFileByUrl(existingDiary.getImageUrl());
                existingDiary.setImageUrl(null);
            }

            // 새 이미지가 있으면 S3에 업로드
            if (imageFile != null && !imageFile.isEmpty()) {
                String newImageUrl = s3Service.uploadDiaryImage(user.getId(), imageFile);
                existingDiary.setImageUrl(newImageUrl);
            }
        }
        
        // -------------------------
        // AI 코멘트 포함 저장 (기존 Diary 객체 그대로)
        // -------------------------
        if (isContentChanged) {
            try {
                AIResult aiResult = aiService.generatResult("diary", existingDiary.getContent());
                existingDiary.setAiresult(aiResult);
            } catch (Exception e) {
                logger.error("AI 코멘트 생성 실패 (일기 저장은 계속 진행): " + e.getMessage());
                existingDiary.setAiresult(null);
            }
        }


        // JPA 영속 객체이므로 save 호출 없이도 트랜잭션 commit 시 변경 반영
        return convertToDto(existingDiary);
    }
    // -----------------------------
    // 특정 날짜 일기 조회
    // -----------------------------
    @Transactional(readOnly = true)
    public Optional<DiaryDto> getDiaryDtoByDate(Long userId, LocalDate date) {
        User user = getUserOrThrow(userId);

        return diaryRepository.findByUserIdAndDate(userId, date )
                .map(this::convertToDto);
    }

    // -----------------------------
    // 월별 일기 조회
    // -----------------------------
    @Transactional(readOnly = true)
    public List<DiaryDto> getDiariesByYearAndMonth(Long userId, int year, int month) {
        User user = getUserOrThrow(userId);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return diaryRepository.findByUserIdAndDateBetween(userId,start, end).stream()
                .filter(d -> d.getUser() != null && d.getUser().getId().equals(userId))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    // -----------------------------
    // 일기 삭제
    // -----------------------------
    @Transactional
    public void deleteByDate(Long userId, LocalDate date) {
        User user = getUserOrThrow(userId);
        diaryRepository.findByUserIdAndDate(userId, date)
            .ifPresent(diary -> {
                // 이미지 파일 삭제
//                if (diary.getImageUrl() != null && !diary.getImageUrl().isEmpty()) {
//                    Path filePath = Paths.get(uploadDir).resolve(
//                            diary.getImageUrl().replace("/uploads/", "")
//                    ).toAbsolutePath();
//                    try {
//                        Files.deleteIfExists(filePath);
//                    } catch (IOException e) {
//                        logger.error("이미지 파일 삭제 실패: " + filePath, e);
//                    }
//                }
            	if (diary.getImageUrl() != null && !diary.getImageUrl().isEmpty()) {
                    s3Service.deleteFileByUrl(diary.getImageUrl());
                }

               
                // 일기 삭제
                diaryRepository.delete(diary);
            });
    }


//    // -----------------------------
//    // CSV 내보내기
//    // -----------------------------
//    @Transactional(readOnly = true)
//    public void writeDiariesToCSV(Long userId, LocalDate startDate, LocalDate endDate, Writer writer) throws IOException {
//
//        List<Diary> diaries = diaryRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
//
//        String[] headers = {"Date", "Username", "Title", "Content", "EmojiType", "AIComment"};
//
//        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers))) {
//            
//            for (Diary diary : diaries) {
//                String emojiType = diary.getEmojiList() != null ? diary.getEmojiList().getType() : "";
//                String aiComment = diary.getAiresult() != null ? diary.getAiresult().getAicomment() : "";
//
//                printer.printRecord(
//                        diary.getDate(),
//                        diary.getUser().getNickname(),
//                        diary.getTitle(),
//                        diary.getContent(),
//                        emojiType,
//                        aiComment
//                );
//            }
//            printer.flush();
//        }
//    }
    // -----------------------------
    // 주별 감정 통계
    // -----------------------------
//    @Transactional(readOnly = true)
//    public List<DiaryEmotionDto> getEmotionByWeek(Long userId, LocalDate start, LocalDate end) {
//        User user = getUserOrThrow(userId);
//
//        List<Diary> diaries = diaryRepository.findByDateBetween(start, end).stream()
//                .filter(d -> d.getUser() != null && d.getUser().getId().equals(userId))
//                .collect(Collectors.toList());
//
//        if (diaries.isEmpty()) {
//            return List.of(
//                    new DiaryEmotionDto(null, "happy", 2),
//                    new DiaryEmotionDto(null, "sad", 1),
//                    new DiaryEmotionDto(null, "anger", 1)
//            );
//        }
//
//        Map<String, Integer> counts = new HashMap<>();
//        diaries.forEach(d -> {
//            if (d.getEmojiList() != null) {
//                counts.put(d.getEmojiList().getType(),
//                        counts.getOrDefault(d.getEmojiList().getType(), 0) + 1);
//            }
//        });
//
//        return counts.entrySet().stream()
//                .map(e -> new DiaryEmotionDto(null, e.getKey(), e.getValue()))
//                .collect(Collectors.toList());
//    }
    
    //통계 테스트(주간)
    public List<DiaryEmotionDto> getEmotionByWeek(String header,LocalDate start, LocalDate end) {
    	Long userId = jwtUtil.findUserIdByHeader(header);
    	
		List<Diary> diaries = diaryRepository.findByUserIdAndDateBetween(userId, start, end);
		
		if(!diaries.isEmpty()) {
			Map<String, Integer> counts = new HashMap<>();
			diaries.forEach(d -> {
				String emo = d.getEmojiList().getType();
				counts.put(emo, counts.getOrDefault(emo, 0)+ 1);
			});
			return counts.entrySet().stream()
					.map(e -> new DiaryEmotionDto(null, e.getKey(), e.getValue())).toList();
		}
		return List.of();
		
    }
    //통계 테스트(일간)
    public List<DiaryEmojiDto> getEmotionByDate(String header, LocalDate start, LocalDate end) {
    	Long userId = jwtUtil.findUserIdByHeader(header);
    	return diaryRepository.findByUserIdAndDateBetween(userId ,start, end)
                .stream()
                .map(diary -> {
                    DiaryEmojiDto dto = new DiaryEmojiDto();
                    dto.setDate(diary.getDate());
                    dto.setEmojiId(diary.getEmojiList().getId());
                    return dto;
                })
                .collect(Collectors.toList());
    
}
    //csv내보내기
    public List<DiaryCsvDto> getDiaryCsvData(String header, LocalDate start, LocalDate end) {
        Long userId = jwtUtil.findUserIdByHeader(header);

        List<Diary> diaries = diaryRepository.findByUserIdAndDateBetween(userId, start, end);
        if (diaries.isEmpty()) return List.of();

        return diaries.stream()
            .map(d -> new DiaryCsvDto(
                    d.getDate(),
                    d.getUser().getNickname(),
                    d.getTitle(),
                    d.getContent(),
                    d.getEmojiList() != null ? d.getEmojiList().getType() : "",
                    d.getAiresult() != null ? d.getAiresult().getAicomment() : "" // 각 일기별 AI 코멘트
            ))
            .collect(Collectors.toList());
    }
    
    
}

