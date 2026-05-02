package com.jobtracker.service;

import com.jobtracker.config.JwtService;
import com.jobtracker.dto.*;
import com.jobtracker.entity.RefreshToken;
import com.jobtracker.entity.Role;
import com.jobtracker.entity.User;
import com.jobtracker.exception.DuplicateResourceException;
import com.jobtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user authentication operations.
 *
 * Responsibilities:
 * - User registration with duplicate email check
 * - User login with credential verification
 * - JWT access token generation
 * - Refresh token generation and rotation
 * - Entity-to-DTO mapping (no entities exposed to controller)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    /**
     * Register a new user.
     *
     * Flow:
     * 1. Check for duplicate email
     * 2. Encode password with BCrypt
     * 3. Save user with default USER role
     * 4. Generate JWT access token
     * 5. Generate refresh token
     * 6. Return AuthResponse DTO
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Build and save user entity
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {} ({})", user.getDisplayUsername(), user.getEmail());

        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(mapToUserDto(user))
                .build();
    }

    /**
     * Authenticate an existing user.
     *
     * Flow:
     * 1. Authenticate credentials via AuthenticationManager
     * 2. Load user from database
     * 3. Generate JWT access token
     * 4. Generate (or rotate) refresh token
     * 5. Return AuthResponse DTO
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Authenticate - throws BadCredentialsException if invalid
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Load authenticated user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        log.info("User logged in: {} ({})", user.getDisplayUsername(), user.getEmail());

        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(mapToUserDto(user))
                .build();
    }

    /**
     * Refresh an access token using a valid refresh token.
     *
     * Flow:
     * 1. Look up refresh token in DB
     * 2. Verify it hasn't expired
     * 3. Generate new access token for the associated user
     * 4. Return AuthResponse with new access token (same refresh token)
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken());
        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user);

        log.info("Access token refreshed for user: {}", user.getEmail());

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .user(mapToUserDto(user))
                .build();
    }

    /**
     * Map User entity to UserDto (no password, no internal fields).
     */
    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getDisplayUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
