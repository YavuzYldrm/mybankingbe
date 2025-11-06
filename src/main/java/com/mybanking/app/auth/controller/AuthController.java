package com.mybanking.app.auth.controller;

import com.mybanking.app.auth.dto.LoginRequest;
import com.mybanking.app.auth.dto.LoginResponse;
import com.mybanking.app.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        log.info("login start email={}", req.email());
        var res = authService.loginWithEmail(req.email(), req.password());
        return ResponseEntity.ok(new LoginResponse(res.token(), res.expiresAt(), res.roles()));
    }
}