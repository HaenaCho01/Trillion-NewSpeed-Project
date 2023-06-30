package com.sparta.trillionnewspeedproject.service;

import com.sparta.trillionnewspeedproject.dto.CommentRequestDto;
import com.sparta.trillionnewspeedproject.dto.CommentResponseDto;
import com.sparta.trillionnewspeedproject.entity.Comment;
import com.sparta.trillionnewspeedproject.entity.Post;
import com.sparta.trillionnewspeedproject.entity.User;
import com.sparta.trillionnewspeedproject.repository.CommentRepository;
import com.sparta.trillionnewspeedproject.repository.PostRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;

	// 선택한 게시글에 대한 댓글 전체 조회
	public List<CommentResponseDto> getCommentsByPostId(Long postid) {
		return commentRepository.findAllByPost_idOrderByCreatedAtDesc(postid).stream().map(CommentResponseDto::new).toList();
	}

	// 댓글 작성
	public CommentResponseDto createComment(Long postid, CommentRequestDto requestDto, User user) {
		Post post = postRepository.getById(postid);
		Comment comment = new Comment(post, requestDto, user);
		Comment saveComment = commentRepository.save(comment);
		CommentResponseDto commentResponseDto = new CommentResponseDto(saveComment);
		return commentResponseDto;
	}

	// 선택한 댓글 수정
	@Transactional
	public CommentResponseDto updateComment(Long postid, Long commentid, CommentRequestDto requestDto, User user, HttpServletResponse response) {
		if (postid != findComment(commentid).getPost().getId()) {
			response.setStatus(404);
			return null;
		} else if (!checkUser(commentid, user)) {
			response.setStatus(400);
			return null;
		} else {
			findComment(commentid).update(requestDto);
			CommentResponseDto commentResponseDto = new CommentResponseDto(findComment(commentid));
			return commentResponseDto;
		}
	}

	// 선택한 댓글 삭제
	public void deleteComment(Long postid, Long commentid, @AuthenticationPrincipal User user, HttpServletResponse response) {
		if (postid != findComment(commentid).getPost().getId()) {
			response.setStatus(404);
		} else if (!checkUser(commentid, user)) {
			response.setStatus(400);
		} else {
			commentRepository.delete(findComment(commentid));
		}
	}

	// 선택한 댓글 좋아요 기능 추가
	public CommentResponseDto commentLike(Long postid, Long commentid, User user, HttpServletResponse response) {
		if (postid != findComment(commentid).getPost().getId()) {
			response.setStatus(404);
			return null;
		} else if (checkUser(commentid, user)) {
			response.setStatus(400);
			return null;
		} else {
			Comment comment = findComment(commentid);
			comment.updateLikes();
			Comment savedComment = commentRepository.save(comment);
			CommentResponseDto commentResponseDto = new CommentResponseDto(savedComment);
			return commentResponseDto;
		}
	}

	// id에 따른 댓글 찾기
	private Comment findComment(Long commentid) {
		return commentRepository.findById(commentid).orElseThrow(() ->
				new IllegalArgumentException("선택한 게시글은 존재하지 않습니다.")
		);
	}

	// id에 따른 게시글 찾기
	private Post findPost(Long id) {
		return postRepository.findById(id).orElseThrow(() ->
				new IllegalArgumentException("선택한 게시글은 존재하지 않습니다.")
		);
	}

	// 선택한 댓글의 사용자가 맞는지 혹은 관리자인지 확인하기
	private boolean checkUser(Long selectId, User user) {
		Comment comment = findComment(selectId);
		if (comment.getUser().getUsername().equals(user.getUsername()) || user.getRole().getAuthority().equals("ROLE_ADMIN")) {
			return true;
		} else {
			return false;
		}
	}
}