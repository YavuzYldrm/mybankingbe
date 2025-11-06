package com.mybanking.app.auth.service;

import com.mybanking.app.common.error.AppException;
import com.mybanking.app.common.error.ErrorCode;
import com.mybanking.app.user.entity.User;
import com.mybanking.app.user.repository.UserRepository;
import com.mybanking.app.user.dto.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock com.mybanking.app.security.JwtTokenProvider jwtTokenProvider;

    @InjectMocks AuthService authService;

    private static User user(UUID id, String email, String hash, Set<Role> roles) {
        var u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPassword(hash);
        u.setRoles(roles);
        return u;
    }

    @Test
    void login_ok() {
        var id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var u  = user(id, "jonathanjackson@mybanking.nl", "{bcrypt}hash", Set.of(Role.USER));

        when(userRepository.findByEmail("jonathanjackson@mybanking.nl")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("S3cret!", "{bcrypt}hash")).thenReturn(true);
        when(jwtTokenProvider.createToken(id.toString(), u.getRoles())).thenReturn("jwt-token-123");
        when(jwtTokenProvider.getExpiryEpochSeconds()).thenReturn(1_700_000_000L);

        AuthService.LoginResult res = authService.loginWithEmail("jonathanjackson@mybanking.nl", "S3cret!");

        assertThat(res.token()).isEqualTo("jwt-token-123");
        assertThat(res.expiresAt()).isEqualTo(1_700_000_000L);

        verify(jwtTokenProvider).createToken(id.toString(), u.getRoles());
    }

    @Test
    void login_email_not_found() {
        when(userRepository.findByEmail("jonathanjackson@mybanking.nl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginWithEmail("jonathanjackson@mybanking.nl", "x"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS))
                .hasMessageContaining("Invalid credentials");

        verifyNoInteractions(passwordEncoder, jwtTokenProvider);
    }

    @Test
    void login_wrong_password() {
        var u = user(UUID.randomUUID(), "jonathanjackson@mybanking.nl", "{bcrypt}hash", Set.of(Role.USER));
        when(userRepository.findByEmail("jonathanjackson@mybanking.nl")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("bad", "{bcrypt}hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.loginWithEmail("jonathanjackson@mybanking.nl", "bad"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS))
                .hasMessageContaining("Invalid credentials");

        verify(jwtTokenProvider, never()).createToken(any(), any());
    }
}

