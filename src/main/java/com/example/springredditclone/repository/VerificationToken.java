package com.example.springredditclone.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationToken extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
}
