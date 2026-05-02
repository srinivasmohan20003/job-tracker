package com.jobtracker.service;

import com.jobtracker.entity.RefreshToken;
import com.jobtracker.entity.User;
import com.jobtracker.exception.TokenRefreshException;
import com.jobtracker.repository.RefreshTokenRepository;
import com.jobtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing refresh tokens.
 *
 * Design decisions:
 * - One refresh token per user (old token deleted on new login)
 * - Token rotation: issuing a new refresh token invalidates the old one
 * - Tokens stored in DB for revocation support
 * - UUID-based tokens (not JWT) since they're looked up server-side
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * Create a new refresh token for the given user.
     * Deletes any existing token for the user first (one active token per user).
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Delete existing refresh token for this user
        refreshTokenRepository.findByUser(user)
                .ifPresent(existing -> refreshTokenRepository.delete(existing));

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        refreshToken = refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for user: {}", user.getEmail());
        return refreshToken;
    }

    /**
     * Find a refresh token by its token string.
     */
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException(token, "Refresh token not found"));
    }

    /**
     * Verify that a refresh token has not expired.
     * If expired, delete it and throw an exception.
     */
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            log.warn("Refresh token expired for user: {}", token.getUser().getEmail());
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token has expired. Please log in again.");
        }
        return token;
    }

    /**
     * Delete all refresh tokens for a given user (e.g., on logout).
     */
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
        log.info("Deleted refresh tokens for user: {}", user.getEmail());
    }
}
