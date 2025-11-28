package com.mind_mate.home.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mind_mate.home.dto.EmojiRequestDto;
import com.mind_mate.home.dto.EmojiResponseDto;
import com.mind_mate.home.entity.Board;
import com.mind_mate.home.entity.Comment;
import com.mind_mate.home.entity.Emoji;
import com.mind_mate.home.entity.User;
import com.mind_mate.home.repository.BoardRepository;
import com.mind_mate.home.repository.CommentRepository;
import com.mind_mate.home.repository.EmojiRepository;
import com.mind_mate.home.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmojiService {

    private final EmojiRepository emojiRepository;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    /** 게시글/댓글 이모지 토글 */
    @Transactional
    public EmojiResponseDto toggleEmoji(EmojiRequestDto request, Long userId) {
    	User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (request.getBoardId() != null) {
            Board board = boardRepository.findById(request.getBoardId())
                    .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
            return handleToggle(board, null, user, request);
        } else if (request.getCommentId() != null) {
            Comment comment = commentRepository.findById(request.getCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
            return handleToggle(null, comment, user, request);
        }

        throw new IllegalArgumentException("이모지를 적용할 대상을 찾을 수 없습니다.");
    }

    /** 게시글용 이모지 조회 */
    @Transactional(readOnly = true)
    public List<EmojiResponseDto> getEmojiForBoard(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        List<Emoji> emojis = emojiRepository.findByBoard(board);
        return convertToDtoList(emojis, userId);
    }

    /** 댓글용 이모지 조회 */
    @Transactional(readOnly = true)
    public List<EmojiResponseDto> getEmojiForComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));
        List<Emoji> emojis = emojiRepository.findByComment(comment);
        return convertToDtoList(emojis, userId);
    }

    /** 내부 처리: 토글 로직 */
    private EmojiResponseDto handleToggle(Board board, Comment comment, User user, EmojiRequestDto request) {
        //  같은 타입 클릭 시 취소
        Optional<Emoji> existingSameType;
        if (board != null) existingSameType = emojiRepository.findByBoardAndUserAndType(board, user, request.getType());
        else existingSameType = emojiRepository.findByCommentAndUserAndType(comment, user, request.getType());

        if (existingSameType.isPresent()) {
            emojiRepository.delete(existingSameType.get());
            return new EmojiResponseDto(request.getType(), false, request.getImageUrl(),
                    getUpdatedCount(board, comment, request.getType()));
        }

        //  다른 타입이면 기존 이모지 삭제 후 새로 저장
        Optional<Emoji> existingForUser;
        if (board != null) existingForUser = emojiRepository.findByBoardAndUser(board, user);
        else existingForUser = emojiRepository.findByCommentAndUser(comment, user);

        existingForUser.ifPresent(emojiRepository::delete);

        Emoji emoji = Emoji.builder()
                .board(board)
                .comment(comment)
                .user(user)
                .type(request.getType())
                .imageUrl(request.getImageUrl())
                .count(1)
                .build();

        emojiRepository.save(emoji);

        return new EmojiResponseDto(request.getType(), true, request.getImageUrl(),
                getUpdatedCount(board, comment, request.getType()));
    }
    /** 이모지 리스트 DTO 변환 */
    private List<EmojiResponseDto> convertToDtoList(List<Emoji> emojis, Long userId) {
        return emojis.stream()
                .collect(Collectors.groupingBy(Emoji::getType))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<Emoji> list = entry.getValue();
                    boolean selected = userId != null && list.stream().anyMatch(e -> e.getUser().getId().equals(userId));
                    Emoji sample = list.get(0);
                    return new EmojiResponseDto(entry.getKey(), selected, sample.getImageUrl(), list.size());
                })
                .collect(Collectors.toList());
    }

    /** 게시글/댓글별 이모지 개수 조회 */
    private int getUpdatedCount(Board board, Comment comment, String type) {
        if (board != null) return emojiRepository.countByBoardAndType(board, type);
        else return emojiRepository.countByCommentAndType(comment, type);
    }

//    /** userId가 없으면 기본값 1번 유저 */
//    private User getUserOrDefault(Long userId) {
//        if (userId == null) return userRepository.findById(1L).orElseThrow();
//        return userRepository.findById(userId).orElseThrow();
//    }
}
