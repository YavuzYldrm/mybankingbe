package com.mybanking.app.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawResult(UUID accountId, BigDecimal withdrawn, BigDecimal feeCharged, BigDecimal newBalance) {}

