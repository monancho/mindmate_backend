package com.mind_mate.home.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mind_mate.home.dto.CommentRequestDto;
import com.mind_mate.home.dto.CommentResponseDto;
import com.mind_mate.home.dto.EmojiResponseDto;
import com.mind_mate.home.entity.Board;
import com.mind_mate.home.entity.Comment;
import com.mind_mate.home.entity.Emoji;
import com.mind_mate.home.entity.User;
import com.mind_mate.home.repository.BoardRepository;
import com.mind_mate.home.repository.CommentRepository;
import com.mind_mate.home.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final EmojiService emojiService; // 댓글 이모지 서비스

//    /** 댓글 생성 */
//    @Transactional
//    public CommentResponseDto createComment(CommentRequestDto request) {
//        Board board = boardRepository.findById(request.getBoardId())
//                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
//
//        User user = userRepository.findById(request.getUserId())
//                .orElseThrow(() -> new IllegalArgumentException("작성자가 존재하지 않습니다."));
//
//        Comment comment = Comment.builder()
//                .board(board)
//                .user(user)
//                .content(request.getContent())
//                .build();
//
//        Comment saved = commentRepository.save(comment);
//        return convertToDto(saved, request.getUserId());
//    }
    
    /** 댓글 생성 */
    @Transactional
    public CommentResponseDto createComment(CommentRequestDto request, Long userId) {
        Board board = boardRepository.findById(request.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("작성자가 존재하지 않습니다."));

        Comment comment = Comment.builder()
                .board(board)
                .user(user)
                .content(request.getContent())
                .build();

        Comment saved = commentRepository.save(comment);
        return convertToDto(saved, userId);
    }
    
//    /** 댓글 수정 */
//    @Transactional
//    public CommentResponseDto updateComment(Long id, CommentRequestDto request) {
//        Comment comment = commentRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));
//        comment.setContent(request.getContent());
//        return convertToDto(comment, request.getUserId());
//    }
    
    /** 댓글 수정 */
    @Transactional
    public CommentResponseDto updateComment(Long id, CommentRequestDto request, Long userId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (!comment.getUser().getId().equals(userId) && !"ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("작성자 또는 관리자만 수정가능합니다.");
        }

        comment.setContent(request.getContent());
        return convertToDto(comment, userId);
    }

//    /** 댓글 삭제 */
//    @Transactional
//    public void deleteComment(Long id) {
//        commentRepository.deleteById(id);
//    }
    
    /** 댓글 삭제 */
    @Transactional
    public void deleteComment(Long id, Long userId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (!comment.getUser().getId().equals(userId) && !"ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("작성자 또는 관리자만 삭제가능합니다.");
        }

        commentRepository.delete(comment);
    }

//    /** 게시글별 댓글 리스트 조회 (댓글 이모지 포함) */
//    @Transactional(readOnly = true)
//    public List<CommentResponseDto> getCommentsByBoardDto(Long boardId, Long userId) {
//        List<Comment> comments = commentRepository.findByBoardId(boardId);
//        return comments.stream()
//                .map(comment -> convertToDto(comment, userId))
//                .collect(Collectors.toList());
//    }
    
    /** 게시글별 댓글 리스트 조회 */
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByBoardDto(Long boardId, Long userId) {
        List<Comment> comments = commentRepository.findByBoardId(boardId);
        return comments.stream()
                .map(comment -> convertToDto(comment, userId))
                .collect(Collectors.toList());
    }
    
    /** DTO 변환 + 댓글별 이모지 포함 */
    private CommentResponseDto convertToDto(Comment comment, Long userId) {
        List<EmojiResponseDto> emojiDtos = emojiService.getEmojiForComment(comment.getId(), userId);

        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .writer(comment.getUser().getNickname())
                .writerId(comment.getUser().getId())
                .writerRole(comment.getUser().getRole()) 
                .createdate(comment.getCreatedate())
                .updatedAt(comment.getUpdatedAt())
                .emojis(emojiDtos)
                
                .build();
    }

//    /** 댓글 이모지 리스트 조회 */
//    @Transactional(readOnly = true)
//    public List<EmojiResponseDto> getCommentEmojis(Long commentId, Long userId) {
//        return emojiService.getEmojiForComment(commentId, userId);
//    }
//
//    /** 댓글 DTO 변환 (댓글 이모지 포함) */
//    private CommentResponseDto convertToDto(Comment comment, Long userId) {
//        List<EmojiResponseDto> emojiDtos = getCommentEmojis(comment.getId(), userId);
//
//        return CommentResponseDto.builder()
//                .id(comment.getId())
//                .content(comment.getContent())
//                .writer(comment.getUser().getNickname())
//                .writerId(comment.getUser().getId())
//                .createdate(comment.getCreatedate())
//                .updatedAt(comment.getUpdatedAt())
//                .emojis(emojiDtos)
//                .build();
//    }
}
