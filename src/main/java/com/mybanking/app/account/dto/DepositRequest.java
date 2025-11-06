package com.mybanking.app.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(
        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount
) {}

