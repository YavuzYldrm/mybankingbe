package com.mybanking.app.common.util;

import com.mybanking.app.account.entity.Account;
import java.math.BigDecimal;
import java.math.RoundingMode;

public interface FeePolicy {

    enum Operation { WITHDRAW, TRANSFER, DEPOSIT }

    BigDecimal feeFor(Operation op, Account primaryAccount, BigDecimal amount);

    class CreditCardOnePercent implements FeePolicy {
        private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

        @Override
        public BigDecimal feeFor(Operation op, Account primaryAccount, BigDecimal amount) {
            if (primaryAccount == null || amount == null) return BigDecimal.ZERO;

            boolean isCredit = primaryAccount.getCard() != null
                    && primaryAccount.getCard().getCardType() == CardType.CREDIT;

            if (!isCredit) return BigDecimal.ZERO;

            return amount
                    .multiply(new BigDecimal("1"))
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        }
    }
}
