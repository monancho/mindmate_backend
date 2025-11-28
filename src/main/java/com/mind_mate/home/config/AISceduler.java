//package com.mind_mate.home.config;
//
//import java.time.DayOfWeek;
//import java.time.LocalDate;
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import com.mind_mate.home.dto.DiaryEmotionDto;
//import com.mind_mate.home.entity.User;
//import com.mind_mate.home.repository.UserRepository;
//import com.mind_mate.home.service.AIService;
//import com.mind_mate.home.service.DiaryService;
//
//import lombok.RequiredArgsConstructor;
//
//@Component
//@RequiredArgsConstructor
//public class AISceduler {
//	
//	@Autowired
//	DiaryService diaryService;
//	
//	@Autowired
//	AIService aiService;
//	
//	@Autowired
//	UserRepository userRepository;
//	
//	@Scheduled(cron = "0 0 23 * * SUN")
//	public void generateWeeklyComment() {
//	  
//	    LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY);
//	    LocalDate end = LocalDate.now();
//
//	    List<User> users = userRepository.findAll();
//	    
//	    for (User user : users) {
//            List<DiaryEmotionDto> dtos = diaryService.getEmotionByWeek( String.valueOf(user.getId()), start, end);
//            aiService.generateWeeklyResult(String.valueOf(user.getId()),start, end, dtos);
//        }
//	}
//
//}
