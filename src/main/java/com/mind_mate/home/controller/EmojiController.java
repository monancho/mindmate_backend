package com.mind_mate.home.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mind_mate.home.dto.EmojiRequestDto;
import com.mind_mate.home.dto.EmojiResponseDto;
import com.mind_mate.home.entity.User;
import com.mind_mate.home.repository.UserRepository;
import com.mind_mate.home.service.EmojiService;
import com.mind_mate.home.util.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/emoji")
@RequiredArgsConstructor
public class EmojiController {

    private final EmojiService emojiService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    
    
//    @PostMapping("/toggle")
//    public EmojiResponseDto toggleEmoji(@RequestBody EmojiRequestDto request) {
//        return emojiService.toggleEmoji(request);
//    }
    @PostMapping("/toggle")
    public ResponseEntity<?> toggleEmoji(
            @RequestBody EmojiRequestDto request,
            @RequestHeader(AUTHORIZATION_HEADER) String header) {

        Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰 정보가 없습니다.");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("유저를 찾을 수 없습니다.");
        }

        request.setUserId(userId);
        EmojiResponseDto dto = emojiService.toggleEmoji(request, userId);
        return ResponseEntity.ok(dto);
    }
    
    @GetMapping("/{boardId}/emoji")
    public ResponseEntity<List<EmojiResponseDto>> getBoardEmojis(
            @PathVariable Long boardId,
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String header) {

        Long userId = null;
        if (header != null) {
            userId = jwtUtil.findUserIdByHeader(header);
        }

        List<EmojiResponseDto> emojis = emojiService.getEmojiForBoard(boardId, userId);
        return ResponseEntity.ok(emojis);
    }
//    @GetMapping("/board/{boardId}")
//    public List<EmojiResponseDto> getBoardEmoji(@PathVariable("boardId") Long boardId,
//                                                @RequestParam(value = "userId", required = false) Long userId) {
//        return emojiService.getEmojiForBoard(boardId, userId);
//    }
    /** 게시글 이모지 조회 */
    @GetMapping("/board/{boardId}")
    public ResponseEntity<?> getBoardEmoji(
            @PathVariable("boardId") Long boardId,
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String header) {
        Long userId = null;
        if (header != null) {
            userId = jwtUtil.findUserIdByHeader(header);
        }
        List<EmojiResponseDto> emojis = emojiService.getEmojiForBoard(boardId, userId);
        return ResponseEntity.ok(emojis);
    }

//    @GetMapping("/comment/{commentId}")
//    public List<EmojiResponseDto> getCommentEmoji(@PathVariable("commentId") Long commentId,
//                                                  @RequestParam(value = "userId", required = false) Long userId) {
//        return emojiService.getEmojiForComment(commentId, userId);
//    }
    /** 댓글 이모지 조회 */
    @GetMapping("/comment/{commentId}")
    public ResponseEntity<?> getCommentEmoji(
            @PathVariable("commentId") Long commentId,
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String header) {

        Long userId = null;
        if (header != null) {
            userId = jwtUtil.findUserIdByHeader(header);
        }

        List<EmojiResponseDto> emojis = emojiService.getEmojiForComment(commentId, userId);
        return ResponseEntity.ok(emojis);
    }
}
