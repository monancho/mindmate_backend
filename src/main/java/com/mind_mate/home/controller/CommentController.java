package com.mind_mate.home.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mind_mate.home.dto.CommentRequestDto;
import com.mind_mate.home.dto.CommentResponseDto;
import com.mind_mate.home.dto.EmojiRequestDto;
import com.mind_mate.home.dto.EmojiResponseDto;
import com.mind_mate.home.service.CommentService;
import com.mind_mate.home.service.EmojiService;
import com.mind_mate.home.util.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final EmojiService emojiService;
    private final JwtUtil jwtUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** 게시글별 댓글 조회 (누구나 가능) */
    @GetMapping("/boards/{boardId}")
    public ResponseEntity<List<CommentResponseDto>> getCommentsByBoard(
            @PathVariable("boardId") Long boardId,
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String header) {

        Long userId = null;
        if (header != null) {
            userId = jwtUtil.findUserIdByHeader(header);
        }

        List<CommentResponseDto> comments = commentService.getCommentsByBoardDto(boardId, userId);
        return ResponseEntity.ok(comments);
    }
    /** 댓글 작성 (로그인 필요) */
    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(
            @RequestBody CommentRequestDto request,
            @RequestHeader(value = "Authorization", required = false) String header) {

        if (header == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        CommentResponseDto saved = commentService.createComment(request, userId);
        return ResponseEntity.ok(saved);
    }

    /** 댓글 수정 (작성자만 가능) */
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @RequestHeader(AUTHORIZATION_HEADER) String header,
            @PathVariable("commentId") Long commentId,
            @RequestBody CommentRequestDto request) {

        final Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            CommentResponseDto updated = commentService.updateComment(commentId, request, userId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /** 댓글 삭제 (작성자만 가능) */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @RequestHeader(AUTHORIZATION_HEADER) String header,
            @PathVariable("commentId") Long commentId) {

        final Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            commentService.deleteComment(commentId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /** 댓글 이모지 토글 (로그인 필요) */
    @PostMapping("/{commentId}/emoji")
    public ResponseEntity<?> toggleCommentEmoji(
            @RequestHeader(AUTHORIZATION_HEADER) String header,
            @PathVariable("commentId") Long commentId,
            @RequestBody EmojiRequestDto request) {

        // 로그인 체크
        Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // request DTO에 userId와 commentId 세팅
        request.setUserId(userId);
        request.setCommentId(commentId);

        // Service 호출
        EmojiResponseDto updatedEmoji = emojiService.toggleEmoji(request, userId);

        return ResponseEntity.ok(updatedEmoji);
    }
}
