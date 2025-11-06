package com.mybanking.app.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybanking.app.account.dto.*;
import com.mybanking.app.account.service.AccountService;
import com.mybanking.app.security.JwtAuthFilter;
import com.mybanking.app.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.mybanking.app.common.security.SecurityUtils.currentUserId;
import static com.mybanking.app.common.security.SecurityUtils.requireAdmin;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @TestComponent
    static class Mocks {}

    @MockitoBean
    AccountService accountService;

    @MockitoBean
    JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    void listMine_ok() throws Exception {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID a1 = UUID.fromString("11111111-1111-1111-1111-111111111111");

        var summaries = List.of(
                new AccountSummary(a1, "NL01", null, new BigDecimal("100.00"))
        );
        when(accountService.listMine(userId)).thenReturn(summaries);

        try (MockedStatic<?> ignored = mockStatic(
                com.mybanking.app.common.security.SecurityUtils.class)) {
            when(currentUserId()).thenReturn(userId);

            mvc.perform(get("/api/accounts/detail"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].accountId").value(a1.toString()))
                    .andExpect(jsonPath("$[0].accountNumber").value("NL01"))
                    .andExpect(jsonPath("$[0].balance").value(100.00));
        }

        verify(accountService).listMine(userId);
    }

    @Test
    void admin_getAllBalances_ok() throws Exception {
        var rows = List.of(
                new AccountSummary(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "NL01", null, new BigDecimal("10.00"))
        );
        when(accountService.getAllBalances()).thenReturn(rows);

        try (MockedStatic<?> ignored = mockStatic(
                com.mybanking.app.common.security.SecurityUtils.class)) {
            ignored.when(() -> requireAdmin()).thenAnswer(inv -> null);

            mvc.perform(get("/api/accounts/admin/balances"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].accountNumber").value("NL01"))
                    .andExpect(jsonPath("$[0].balance").value(10.0));
        }

        verify(accountService).getAllBalances();
    }

    @Test
    void withdraw_ok() throws Exception {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID accId  = UUID.fromString("11111111-1111-1111-1111-111111111111");

        var req = new WithdrawRequest(new BigDecimal("100.00"));
        var res = new WithdrawResult(accId, new BigDecimal("100.00"),
                new BigDecimal("1.00"), new BigDecimal("399.00"));
        when(accountService.withdraw(accId, new BigDecimal("100.00"), userId))
                .thenReturn(res);

        try (MockedStatic<?> ignored = mockStatic(
                com.mybanking.app.common.security.SecurityUtils.class)) {
            when(currentUserId()).thenReturn(userId);

            mvc.perform(post("/api/accounts/{accountId}/withdraw", accId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(accId.toString()))
                    .andExpect(jsonPath("$.withdrawn").value(100.00))
                    .andExpect(jsonPath("$.feeCharged").value(1.00))
                    .andExpect(jsonPath("$.newBalance").value(399.00));
        }

        verify(accountService).withdraw(accId, new BigDecimal("100.00"), userId);
    }

    @Test
    void deposit_ok() throws Exception {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID accId  = UUID.fromString("22222222-2222-2222-2222-222222222222");

        var req = new DepositRequest(new BigDecimal("50.00"));
        var res = new DepositResult(accId, new BigDecimal("50.00"),
                new BigDecimal("1.00"), new BigDecimal("149.00"));
        when(accountService.deposit(accId, new BigDecimal("50.00"), userId))
                .thenReturn(res);

        try (MockedStatic<?> ignored = mockStatic(
                com.mybanking.app.common.security.SecurityUtils.class)) {
            when(currentUserId()).thenReturn(userId);

            mvc.perform(post("/api/accounts/{accountId}/deposit", accId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(accId.toString()))
                    .andExpect(jsonPath("$.deposited").value(50.00))
                    .andExpect(jsonPath("$.feeCharged").value(1.00))
                    .andExpect(jsonPath("$.newBalance").value(149.00));
        }

        verify(accountService).deposit(accId, new BigDecimal("50.00"), userId);
    }

    @Test
    void withdraw_validation_error_400() throws Exception {
        UUID accId  = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var bad = new WithdrawRequest(new BigDecimal("-5"));

        mvc.perform(post("/api/accounts/{accountId}/withdraw", accId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    void deposit_validation_error_400() throws Exception {
        UUID accId  = UUID.fromString("22222222-2222-2222-2222-222222222222");
        var bad = new DepositRequest(new BigDecimal("0"));

        mvc.perform(post("/api/accounts/{accountId}/deposit", accId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }
}

