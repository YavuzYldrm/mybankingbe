package com.mybanking.app.transaction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(
        UUID fromAccountId,
        UUID toAccountId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00") BigDecimal transferred,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00") BigDecimal feeCharged,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00") BigDecimal fromNewBalance,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00") BigDecimal toNewBalance
) {}

