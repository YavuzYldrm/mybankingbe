package com.mybanking.app.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record WithdrawRequest(
        @NotNull
        @Positive
        @DecimalMin(value = "0.01")
        BigDecimal amount) {}
