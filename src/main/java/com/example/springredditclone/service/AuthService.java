package com.example.springredditclone.service;

import com.example.springredditclone.dto.RefreshTokenRequest;
import com.example.springredditclone.exception.UsernameNotFoundException;
import com.example.springredditclone.security.JwtProvider;


import com.example.springredditclone.dto.AuthenticationResponse;
import com.example.springredditclone.dto.LoginRequest;
import com.example.springredditclone.dto.RegisterRequest;
import com.example.springredditclone.exception.SpringRedditException;
import com.example.springredditclone.model.NotificationEmail;
import com.example.springredditclone.model.User;
import com.example.springredditclone.model.VerificationToken;
import com.example.springredditclone.repository.UserRepository;
import com.example.springredditclone.repository.VerificationTokenRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.example.springredditclone.util.Constants.ACTIVATION_EMAIL;
import static java.time.Instant.now;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtProvider jwtProvider;
  private final VerificationTokenRepository verificationTokenRepository;
  private final MailContentBuilder mailContentBuilder;
  private final MailService mailService;
  private final RefreshTokenService refreshTokenService;

  @Transactional
  public void signup(RegisterRequest registerRequest) {
    User user = new User();
    user.setUsername(registerRequest.getUsername());
    user.setEmail(registerRequest.getEmail());
    user.setPassword(encodePassword(registerRequest.getPassword()));
    user.setCreated(now());
    user.setEnabled(false);

    userRepository.save(user);

    String token = generateVerificationToken(user);

    String message = mailContentBuilder.build("Thank you for signing up to Spring Reddit, please click on the below url to activate your account : "
      + ACTIVATION_EMAIL + "/" + token);

    mailService.sendMail(new NotificationEmail("Please Activate your account", user.getEmail(), message));

  }

  private String generateVerificationToken(User user) {
    String token = UUID.randomUUID().toString();
    VerificationToken verificationToken = new VerificationToken();
    verificationToken.setToken(token);
    verificationToken.setUser(user);
    verificationTokenRepository.save(verificationToken);
    return token;

  }

  private String encodePassword(String password) {
    return this.passwordEncoder.encode(password);
  }

  public AuthenticationResponse login(LoginRequest loginRequest) {
    Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
      loginRequest.getPassword()));

    // Hold security context, determine who is the current user, whether it is verified and what access it has. By default
    // use Threadlocal strategy to store credentials. When user logout, clean the user credentials on current thread.
    SecurityContextHolder.getContext().setAuthentication(authenticate);
    String token = jwtProvider.generateToken(authenticate);
    return AuthenticationResponse.builder()
      .authenticationToken(token)
      .refreshToken(refreshTokenService.generateRefreshToken().getToken())
      .expiresAt(Instant.now().plusMillis(jwtProvider.getJwtExpirationInMillis()))
      .username(loginRequest.getUsername())
      .build();
  }

  public AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
    refreshTokenService.validateRefreshToken(refreshTokenRequest.getRefreshToken());
    String token = jwtProvider.generateTokenWithUserName(refreshTokenRequest.getUsername());
    return AuthenticationResponse.builder()
      .authenticationToken(token)
      .refreshToken(refreshTokenRequest.getRefreshToken())
      .expiresAt(Instant.now().plusMillis(jwtProvider.getJwtExpirationInMillis()))
      .username(refreshTokenRequest.getUsername())
      .build();
  }

  public boolean isLoggedIn() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated();
  }

  public void verifyAccount(String token) {
    Optional<VerificationToken> verificationTokenOptional = verificationTokenRepository.findByToken(token);
    verificationTokenOptional.orElseThrow(() -> new SpringRedditException("Invalid Token"));
    fetchUserAndEnable(verificationTokenOptional.get());
  }

  @Transactional
  private void fetchUserAndEnable(VerificationToken verificationToken) {
    String username = verificationToken.getUser().getUsername();
    User user = userRepository.findByUsername(username).orElseThrow(() -> new SpringRedditException("User Not Found with id - " + username));
    user.setEnabled(true);
    userRepository.save(user);
  }

  @Transactional(readOnly = true)
  User getCurrentUser() {
    org.springframework.security.core.userdetails.User principal =
      (org.springframework.security.core.userdetails.User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return userRepository.findByUsername(principal.getUsername())
      .orElseThrow(() -> new UsernameNotFoundException("User name not found - " + principal.getUsername()));
  }
}
