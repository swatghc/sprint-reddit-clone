package com.example.springredditclone.service;


import com.example.springredditclone.dto.VoteDto;
import com.example.springredditclone.exception.SpringRedditException;
import com.example.springredditclone.model.Post;
import com.example.springredditclone.model.Vote;
import com.example.springredditclone.repository.PostRepository;
import com.example.springredditclone.repository.VoteRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.example.springredditclone.model.VoteType.UPVOTE;

@Service
@AllArgsConstructor
public class VoteService {

  private final PostRepository postRepository;
  private final VoteRepository voteRepository;
  private final AuthService authService;

  public void vote(VoteDto voteDto) {
    Post post = postRepository.findById(voteDto.getPostId()).orElseThrow(() -> new SpringRedditException("Post not found with the ID " + voteDto.getPostId()));
    Optional<Vote> voteByPostAndUser = voteRepository.findTopByPostAndUserOrderByVoteIdDesc(post, authService.getCurrentUser());

    // Line 31-36: We are first checking whether the user has already performed the same Vote action or not.
    // ie. If the user has already upvoted a particular post, he/she is not allowed to upvote that particular post again.
    if (voteByPostAndUser.isPresent() && voteByPostAndUser.get().getVoteType().equals(voteDto.getVoteType())) {
      throw new SpringRedditException("You have already " + voteDto.getVoteType() + "'d for this post");
    }

    if (UPVOTE.equals(voteDto.getVoteType())) {
      post.setVoteCount(post.getVoteCount() + 1);
    } else {
      post.setVoteCount(post.getVoteCount() - 1);
    }
    voteRepository.save(mapToVote(voteDto, post));
    postRepository.save(post);
  }

  private Vote mapToVote(VoteDto voteDto, Post post) {
    return Vote.builder()
      .voteType(voteDto.getVoteType())
      .post(post)
      .user(authService.getCurrentUser())
      .build();
  }
}
