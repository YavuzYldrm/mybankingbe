package com.mybanking.app.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositResponse(UUID accountId, BigDecimal deposited, BigDecimal feeCharged, BigDecimal newBalance) {}
