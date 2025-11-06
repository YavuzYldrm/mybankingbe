package com.mybanking.app.account.service;

import com.mybanking.app.account.dto.AccountSummary;
import com.mybanking.app.account.dto.DepositResult;
import com.mybanking.app.account.dto.WithdrawResult;
import com.mybanking.app.account.entity.Account;
import com.mybanking.app.account.repository.AccountRepository;
import com.mybanking.app.common.error.AppException;
import com.mybanking.app.common.util.FeePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.mybanking.app.common.error.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final FeePolicy feePolicy;

    @Transactional(readOnly = true)
    public List<AccountSummary> listMine(UUID callerUserId) {
        log.info("listMine done userId={}", callerUserId);
        return accountRepository.findAllByUser_Id(callerUserId)
                .stream()
                .map(a -> new AccountSummary(
                        a.getId(),
                        a.getAccountNumber(),
                        a.getCard() != null ? a.getCard().getCardType() : null,
                        a.getBalance()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountSummary> getAllBalances() {
        log.debug("getAllBalances start");
        return accountRepository.findAll().stream()
                .map(a -> new AccountSummary(
                        a.getId(),
                        a.getAccountNumber(),
                        a.getCard() != null ? a.getCard().getCardType() : null,
                        a.getBalance()))
                .toList();
    }

    @Transactional
    public WithdrawResult withdraw(UUID accountId, BigDecimal amount, UUID callerUserId) {
        log.info("withdraw start accountId={} userId={} amount={}", accountId, callerUserId, amount);
        validateAmount(amount);

        Account acc = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> AppException.notFound(ACCOUNT_NOT_FOUND, "Account not found"));

        ensureOwnerOrThrow(acc, callerUserId);

        BigDecimal fee = feePolicy.feeFor(FeePolicy.Operation.WITHDRAW, acc, amount);

        acc.withdrawWithFeeOrThrow(amount, fee,
                () -> AppException.conflict(INSUFFICIENT_BALANCE, "Insufficient balance"));

        accountRepository.saveAndFlush(acc);
        return new WithdrawResult(acc.getId(), amount, fee, acc.getBalance());
    }

    @Transactional
    public DepositResult deposit(UUID accountId, BigDecimal amount, UUID callerUserId) {
        log.info("deposit start accountId={} userId={} amount={}", accountId, callerUserId, amount);
        validateAmount(amount);

        Account acc = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> AppException.notFound(ACCOUNT_NOT_FOUND, "Account not found"));

        ensureOwnerOrThrow(acc, callerUserId);

        BigDecimal fee = feePolicy.feeFor(FeePolicy.Operation.DEPOSIT, acc, amount);
        BigDecimal net = amount.subtract(fee);
        if (net.signum() <= 0)
            throw AppException.badRequest(VALIDATION_ERROR, "Net amount must be > 0");

        acc.depositCore(net);
        accountRepository.saveAndFlush(acc);
        return new DepositResult(acc.getId(), amount, fee, acc.getBalance());
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0)
            throw AppException.badRequest(VALIDATION_ERROR, "Amount must be > 0");
    }

    private static void ensureOwnerOrThrow(Account acc, UUID requesterId) {
        if (!acc.getUser().getId().equals(requesterId)) {
            log.warn("forbidden accountId={} requesterId={}", acc.getId(), requesterId);
            throw AppException.forbidden(ACCOUNT_FORBIDDEN, "Not owner of the account");
        }
    }
}
