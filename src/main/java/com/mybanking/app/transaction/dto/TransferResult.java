package com.mybanking.app.transaction.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResult(
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal transferred,
        BigDecimal feeCharged,
        BigDecimal fromNewBalance,
        BigDecimal toNewBalance
) {}
