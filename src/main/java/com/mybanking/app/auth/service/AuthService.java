package com.mybanking.app.auth.service;

import com.mybanking.app.common.error.AppException;
import com.mybanking.app.common.error.ErrorCode;
import com.mybanking.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.mybanking.app.security.JwtTokenProvider jwtTokenProvider;

    public LoginResult loginWithEmail(String email, String rawPassword) {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, BAD_REQUEST, "Email and password are required");
        }

        final String normEmail = email.trim().toLowerCase();
        log.info("login attempt email={}", normEmail);

        var user = userRepository.findByEmail(normEmail)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS, UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS, UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtTokenProvider.createToken(user.getId().toString(), user.getRoles());
        Set<String> roles = user.getRoles().stream().map(Object::toString).collect(Collectors.toSet());
        long expiresAt = jwtTokenProvider.getExpiryEpochSeconds();

        log.info("login success userId={} roles={}", user.getId(), roles);
        return new LoginResult(token, expiresAt, roles);
    }

    public record LoginResult(String token, long expiresAt, Set<String> roles) {}
}
