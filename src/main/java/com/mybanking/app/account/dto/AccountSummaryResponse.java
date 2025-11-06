package com.mybanking.app.account.dto;

import com.mybanking.app.common.util.CardType;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountSummaryResponse(
        UUID accountId,
        String accountNumber,
        CardType cardType,
        BigDecimal balance
) {}
