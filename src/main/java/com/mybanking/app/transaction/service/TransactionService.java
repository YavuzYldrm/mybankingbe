package com.mybanking.app.transaction.service;

import com.mybanking.app.account.entity.Account;
import com.mybanking.app.account.repository.AccountRepository;
import com.mybanking.app.common.error.AppException;
import com.mybanking.app.common.util.FeePolicy;
import com.mybanking.app.common.util.TransactionType;
import com.mybanking.app.transaction.dto.TransferResult;
import com.mybanking.app.transaction.entity.Transaction;
import com.mybanking.app.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.mybanking.app.common.error.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository txRepo;
    private final AccountRepository accountRepo;
    private final FeePolicy feePolicy;

    @Transactional
    public TransferResult transfer(UUID fromAccountId,
                                   String toAccountNumber,
                                   BigDecimal amount,
                                   UUID requesterId) {
        log.info("transfer.start from={} to={} amt={} req={}",
                shortId(fromAccountId), toAccountNumber, safeAmt(amount), shortId(requesterId));

        var toAccount = accountRepo.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> AppException.notFound(ACCOUNT_NOT_FOUND, "Destination account not found"));

        if (fromAccountId == null || toAccount.getId() == null) {
            throw AppException.badRequest(VALIDATION_ERROR, "Account ids are required");
        }
        if (fromAccountId.equals(toAccount.getId())) {
            throw AppException.conflict(SAME_ACCOUNT, "Source and destination accounts cannot be the same");
        }
        validateAmount(amount);

        Account from = accountRepo.findByIdForUpdate(fromAccountId)
                .orElseThrow(() -> AppException.notFound(ACCOUNT_NOT_FOUND, "Source account not found"));

        Account to = accountRepo.findByIdForUpdate(toAccount.getId())
                .orElseThrow(() -> AppException.notFound(ACCOUNT_NOT_FOUND, "Destination account not found"));

        ensureOwnerOrThrow(from, requesterId);

        BigDecimal fee = feePolicy.feeFor(FeePolicy.Operation.TRANSFER, from, amount);

        from.withdrawWithFeeOrThrow(amount, fee,
                () -> AppException.conflict(INSUFFICIENT_BALANCE, "Insufficient balance"));

        to.setBalance(to.getBalance().add(amount));

        accountRepo.save(from);
        accountRepo.save(to);

        Transaction tx = Transaction.builder()
                .type(TransactionType.TRANSFER)
                .amount(amount)
                .fee(fee)
                .occurredAt(Instant.now())
                .fromAccount(from)
                .toAccount(to)
                .build();
        txRepo.save(tx);

        return new TransferResult(
                from.getId(), to.getId(),
                amount, fee,
                from.getBalance(), to.getBalance()
        );
    }

    private static void validateAmount(BigDecimal v) {
        if (v == null || v.signum() <= 0) {
            throw AppException.badRequest(VALIDATION_ERROR, "Amount must be > 0");
        }
    }

    private static void ensureOwnerOrThrow(Account acc, UUID requesterId) {
        if (acc == null || acc.getUser() == null || acc.getUser().getId() == null) {
            throw AppException.notFound(ACCOUNT_NOT_FOUND, "Account has no owner");
        }
        if (!acc.getUser().getId().equals(requesterId)) {
            throw AppException.forbidden(ACCOUNT_FORBIDDEN, "You are not allowed to operate on this account");
        }
    }

    private static String shortId(UUID id) {
        return id == null ? "null" : id.toString().substring(0, 8);
    }
    private static String safeAmt(BigDecimal v) {
        return v == null ? "null" : v.stripTrailingZeros().toPlainString();
    }
}
