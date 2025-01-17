package com.example.springredditclone.service;

import com.example.springredditclone.dto.CommentsDto;
import com.example.springredditclone.exception.SpringRedditException;
import com.example.springredditclone.mapper.CommentMapper;
import com.example.springredditclone.model.Comment;
import com.example.springredditclone.model.NotificationEmail;
import com.example.springredditclone.model.Post;
import com.example.springredditclone.model.User;
import com.example.springredditclone.repository.CommentRepository;
import com.example.springredditclone.repository.PostRepository;
import com.example.springredditclone.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import static java.util.stream.Collectors.toList;


@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class CommentService {
  private static final String POST_URL = "";
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final AuthService authService;
  private final CommentMapper commentMapper;
  private final CommentRepository commentRepository;
  private final MailContentBuilder mailContentBuilder;
  private final MailService mailService;

  public void save(CommentsDto commentsDto) {
    Post post = postRepository.findById(commentsDto.getPostId())
      .orElseThrow(() -> new SpringRedditException(commentsDto.getPostId().toString()));
    Comment comment = commentMapper.map(commentsDto, post, authService.getCurrentUser());
    commentRepository.save(comment);

    String message = mailContentBuilder.build(post.getUser().getUsername() + " posted a comment on your post." + POST_URL);
    sendCommentNotification(message, post.getUser());
  }

  private void sendCommentNotification(String message, User user) {
    mailService.sendMail(new NotificationEmail(user.getUsername() + " Commented on your post", user.getEmail(), message));
  }

  public List<CommentsDto> getAllCommentsForPost(Long postId) {
    Post post = postRepository.findById(postId).orElseThrow(() -> new SpringRedditException(postId.toString()));
    return commentRepository.findByPost(post)
      .stream()
      .map(commentMapper::mapToDto).collect(toList());
  }

  public List<CommentsDto> getAllCommentsForUser(String userName) {
    User user = userRepository.findByUsername(userName)
      .orElseThrow(() -> new SpringRedditException(userName));
    return commentRepository.findAllByUser(user)
      .stream()
      .map(commentMapper::mapToDto)
      .collect(toList());
  }
}
