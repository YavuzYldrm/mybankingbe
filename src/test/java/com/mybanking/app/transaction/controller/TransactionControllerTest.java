package com.mybanking.app.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybanking.app.security.JwtTokenProvider;
import com.mybanking.app.transaction.dto.TransferRequest;
import com.mybanking.app.transaction.dto.TransferResult;
import com.mybanking.app.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static com.mybanking.app.common.security.SecurityUtils.currentUserId;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean TransactionService transactionService;

    @Test
    void transfer_ok() throws Exception {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID from   = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID to     = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String toAccountNumber = "NL0000000123";

        var req = new TransferRequest(from, toAccountNumber, new BigDecimal("100.00"));
        var result = new TransferResult(
                from, to,
                new BigDecimal("100.00"),
                new BigDecimal("1.00"),
                new BigDecimal("899.00"),
                new BigDecimal("600.00")
        );
        when(transactionService.transfer(from, toAccountNumber, new BigDecimal("100.00"), userId))
                .thenReturn(result);

        try (MockedStatic<?> ignored = mockStatic(
                com.mybanking.app.common.security.SecurityUtils.class)) {
            when(currentUserId()).thenReturn(userId);

            mvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.fromAccountId").value(from.toString()))
                    .andExpect(jsonPath("$.toAccountId").value(to.toString()))
                    .andExpect(jsonPath("$.transferred").value("100.00"))
                    .andExpect(jsonPath("$.feeCharged").value("1.00"))
                    .andExpect(jsonPath("$.fromNewBalance").value("899.00"))
                    .andExpect(jsonPath("$.toNewBalance").value("600.00"));
        }

        verify(transactionService)
                .transfer(from, toAccountNumber, new BigDecimal("100.00"), userId);
    }

    @Test
    void transfer_validation_error() throws Exception {
        var bad = new TransferRequest(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "NL0000000123",
                new BigDecimal("-5")
        );

        mvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }
}
