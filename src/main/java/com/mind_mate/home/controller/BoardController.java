package com.mind_mate.home.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mind_mate.home.dto.*;
import com.mind_mate.home.entity.Board;
import com.mind_mate.home.entity.Emoji;
import com.mind_mate.home.entity.User;
import com.mind_mate.home.repository.BoardRepository;
import com.mind_mate.home.repository.UserRepository;
import com.mind_mate.home.service.BoardService;
import com.mind_mate.home.service.CommentService;
import com.mind_mate.home.service.EmojiService;
import com.mind_mate.home.util.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

	private final BoardRepository boardRepository;
    private final BoardService boardService;
    private final CommentService commentService;
    private final EmojiService emojiService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** 게시글 리스트 조회 (로그인 없어도 가능) */
    @GetMapping
    public ResponseEntity<?> getBoards(
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String header,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "field", required = false) String field,
            @RequestParam(name = "keyword", required = false) String keyword) {

        final Long userId = (header != null) ? jwtUtil.findUserIdByHeader(header) : null;

        Pageable pageable = PageRequest.of(page, size);
        Page<Board> boards = boardService.getBoards(pageable, field, keyword);

        Page<BoardResponseDto> dtoPage = boards.map(board -> convertToDto(board, userId));
        return ResponseEntity.ok(dtoPage);
    }

    /** 게시글 상세 조회 (댓글 포함, 로그인 없어도 가능) */
    @GetMapping("/{boardId}")
    public ResponseEntity<?> getBoard(
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String header,
            @PathVariable("boardId") Long boardId) {

        final Long userId = (header != null) ? jwtUtil.findUserIdByHeader(header) : null;
        Board board = boardService.getBoard(boardId);

        return ResponseEntity.ok(convertToDto(board, userId));
    }

    /** 게시글 생성 (로그인 필요) */
    @PostMapping
    public ResponseEntity<?> createBoard(
            @RequestHeader(AUTHORIZATION_HEADER) String header,
            @RequestBody BoardRequestDto request) {

        final Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");

        Optional<User> _user = userRepository.findById(userId);
        if (_user.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("유저를 찾을 수 없습니다.");

        User user = _user.get();
        Board board = Board.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .user(user)
                .build();

        Board saved = boardService.save(board, userId);
        return ResponseEntity.ok(convertToDto(saved, userId));
    }

    /** 게시글 수정 (작성자만 가능) */
    @PutMapping("/{boardId}")
    public ResponseEntity<?> updateBoard(
            @RequestHeader(AUTHORIZATION_HEADER) String header,
            @PathVariable("boardId") Long boardId,
            @RequestBody BoardRequestDto request) {

        final Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");

        try {
        Board updated = boardService.update(boardId,
                Board.builder()
                        .title(request.getTitle())
                        .content(request.getContent())
                        .build(),
                userId);

        return ResponseEntity.ok(convertToDto(updated, userId));
    } catch (IllegalArgumentException e) {
    	return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }
		// TODO: handle exception
	}
    
    /** 게시글 삭제 (작성자만 가능) */
    @DeleteMapping("/{boardId}")
    public ResponseEntity<?> deleteBoard(
            @RequestHeader(AUTHORIZATION_HEADER) String header,
            @PathVariable("boardId") Long boardId) {

        final Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");

        try {
        boardService.delete(boardId, userId);
        return ResponseEntity.noContent().build();
    }catch (IllegalArgumentException e) {
    	return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }
		// TODO: handle exception
	}

    /** 게시글 이모지 토글 (로그인 필요) */
    @PostMapping("/{boardId}/emoji")
    public ResponseEntity<?> toggleBoardEmoji(
            @RequestHeader(AUTHORIZATION_HEADER) String header,
            @PathVariable("boardId") Long boardId,
            @RequestBody EmojiRequestDto request) {

        Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        request.setUserId(userId);
        request.setBoardId(boardId);

        EmojiResponseDto updatedEmoji = emojiService.toggleEmoji(request, userId);

        return ResponseEntity.ok(updatedEmoji);
    }

    /** 게시글 DTO 변환 */
    private BoardResponseDto convertToDto(Board board, Long userId) {
        List<CommentResponseDto> commentDtos = commentService.getCommentsByBoardDto(board.getId(), null); // 댓글 조회는 로그인 없어도
        int commentCount = commentDtos.size();

        List<EmojiResponseDto> boardEmojis = Optional.ofNullable(board.getEmojis())
                .orElse(List.of())
                .stream()
                .collect(Collectors.groupingBy(Emoji::getType))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<Emoji> emojis = entry.getValue();
                    boolean selected = userId != null && emojis.stream().anyMatch(e -> e.getUser().getId().equals(userId));
                    Emoji sample = emojis.get(0);
                    return new EmojiResponseDto(entry.getKey(), selected, sample.getImageUrl(), emojis.size());
                })
                .toList();

        String hashtags = board.getHashtags() != null ? board.getHashtags().trim() : "";
        List<String> tagList = new ArrayList<>();
            
        if (!hashtags.isEmpty()) {
            // 공백 단위로 나누고, '#'로 시작하는 것만 필터
            for (String t : hashtags.split("\\s+")) {
                t = t.trim();
                if (!t.isEmpty() && t.startsWith("#")) tagList.add(t);
            }
        }
        String hashtagsForDto = String.join(" ", tagList);
        
        return BoardResponseDto.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .writer(board.getUser() != null ? board.getUser().getNickname() : "익명")
                .writerId(board.getUser() != null ? board.getUser().getId() : null)
                .isPinned(board.isPinned())
                .writerRole(board.getUser() != null ? board.getUser().getRole() : "USER") 
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .viewCount(board.getViewCount())
                .comments(commentDtos)
                .commentCount(commentCount)
                .emojis(boardEmojis)
                .hashtags(hashtagsForDto)
                .build();
    }
 // 내 글 모아보기
    @GetMapping("/my-boards")
    public ResponseEntity<?> getMyBoards(
            @RequestHeader(AUTHORIZATION_HEADER) String header,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Long userId = jwtUtil.findUserIdByHeader(header);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Board> boards = boardService.getBoardsByUser(userId, pageable);

        Page<BoardResponseDto> dtoPage = boards.map(board -> convertToDto(board, userId));
        return ResponseEntity.ok(dtoPage);
    }
    
    // 해시태그 글 모아보기
    @GetMapping("/hashtag/{tag}")
    public ResponseEntity<List<BoardResponseDto>> getBoardsByHashtag(
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String header,
            @PathVariable("tag") String tag) {

        Long userId = (header != null) ? jwtUtil.findUserIdByHeader(header) : null;

        List<Board> boards = boardRepository.findByHashtagIgnoreCase(tag);
        List<BoardResponseDto> result = boards.stream()
            .map(board -> convertToDto(board, userId)) // 기존 convertToDto 그대로 사용
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
    @GetMapping("/hashtags")
    public ResponseEntity<?> getRecommendedHashtags( @RequestParam(name = "days", defaultValue = "7") int days,
            @RequestParam(name = "topN", defaultValue = "5") int topN ) {
    	List<Board> boards = boardRepository.findAll();
    	List<String> tags = boardService.getHashtags(boards,days, topN);
    	return ResponseEntity.ok(tags);
    }
}
