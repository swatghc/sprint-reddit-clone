package com.example.springredditclone.service;

import com.example.springredditclone.dto.SubredditDto;
import com.example.springredditclone.exception.SpringRedditException;
import com.example.springredditclone.exception.SubredditNotFoundException;
import com.example.springredditclone.mapper.SubredditMapper;
import com.example.springredditclone.model.Subreddit;
import com.example.springredditclone.repository.SubredditRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;


@Service
@AllArgsConstructor
@Slf4j
public class SubredditService {
  private final SubredditRepository subredditRepository;
  private final AuthService authService;
  private final SubredditMapper subredditMapper;


  @Transactional(readOnly = true)
  public List<SubredditDto> getAll() {
    return subredditRepository.findAll()
      .stream()
//      .map(this::mapToDto)
      .map(subredditMapper::mapSubredditToDto)
      .collect(toList());
  }

  @Transactional
  public SubredditDto save(SubredditDto subredditDto) {
    Subreddit subreddit = subredditRepository.save(mapToSubreddit(subredditDto));
    subredditDto.setId(subreddit.getId());
    return subredditDto;
  }

//  @Transactional(readOnly = true)
//  public SubredditDto getSubreddit(Long id) {
//    Subreddit subreddit = subredditRepository.findById(id)
//      .orElseThrow(() -> new SubredditNotFoundException("Subreddit not found with id -" + id));
//    return mapToDto(subreddit);
//  }

  public SubredditDto getSubreddit(Long id) {
    Subreddit subreddit = subredditRepository.findById(id)
      .orElseThrow(() -> new SpringRedditException("No subreddit found with ID - " + id));
    return subredditMapper.mapSubredditToDto(subreddit);
  }

  private SubredditDto mapToDto(Subreddit subreddit) {
    return SubredditDto.builder().name(subreddit.getName())
      .id(subreddit.getId())
      .numberOfPosts(subreddit.getPosts().size())
      .build();
  }

  private Subreddit mapToSubreddit(SubredditDto subredditDto) {
    return Subreddit.builder().name("/r/" + subredditDto.getName())
      .description(subredditDto.getDescription())
      .user(authService.getCurrentUser())
      .createdDate(now()).build();
  }
}
