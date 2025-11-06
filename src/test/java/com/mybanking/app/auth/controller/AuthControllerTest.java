package com.mybanking.app.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybanking.app.auth.dto.LoginRequest;
import com.mybanking.app.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockitoBean AuthService authService;
    @MockitoBean com.mybanking.app.security.JwtAuthFilter jwtAuthFilter;

    @Test
    void login_ok() throws Exception {
        var req = new LoginRequest("alice@example.com", "S3cret!");
        var serviceRes = new AuthService.LoginResult("jwt-token-123", 1_700_000_000L, Set.of("USER"));
        when(authService.loginWithEmail(eq("alice@example.com"), eq("S3cret!"))).thenReturn(serviceRes);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.expiresAt").value(1_700_000_000L))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }
}
