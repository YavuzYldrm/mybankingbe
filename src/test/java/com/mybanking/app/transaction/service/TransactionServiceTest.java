package com.mybanking.app.transaction.service;

import com.mybanking.app.account.entity.Account;
import com.mybanking.app.account.repository.AccountRepository;
import com.mybanking.app.common.error.AppException;
import com.mybanking.app.common.util.FeePolicy;
import com.mybanking.app.transaction.dto.TransferResult;
import com.mybanking.app.transaction.entity.Transaction;
import com.mybanking.app.transaction.repository.TransactionRepository;
import com.mybanking.app.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository txRepo;
    @Mock AccountRepository accountRepo;
    @Mock FeePolicy feePolicy;

    @InjectMocks TransactionService service;

    private final UUID fromId = UUID.randomUUID();
    private final UUID toId = UUID.randomUUID();
    private final String toAccountNumber = "NL0000000123";

    private static Account acc(UUID id, UUID ownerId, String balance) {
        var a = new Account();
        a.setId(id);
        a.setBalance(new BigDecimal(balance));
        var user = new User();
        user.setId(ownerId);
        a.setUser(user);
        return a;
    }

    @Test
    void transfer_ok() {
        var requester = UUID.randomUUID();
        var from = acc(fromId, requester, "1000.00");
        var to   = acc(toId, UUID.randomUUID(), "500.00");

        when(accountRepo.findByAccountNumber(toAccountNumber)).thenReturn(Optional.of(to));
        when(accountRepo.findByIdForUpdate(fromId)).thenReturn(Optional.of(from));
        when(accountRepo.findByIdForUpdate(toId)).thenReturn(Optional.of(to));
        when(feePolicy.feeFor(FeePolicy.Operation.TRANSFER, from, new BigDecimal("100.00")))
                .thenReturn(new BigDecimal("1.00"));

        TransferResult r = service.transfer(fromId, toAccountNumber, new BigDecimal("100.00"), requester);

        assertThat(from.getBalance()).isEqualByComparingTo("899.00");
        assertThat(to.getBalance()).isEqualByComparingTo("600.00");
        assertThat(r.fromAccountId()).isEqualTo(fromId);
        assertThat(r.toAccountId()).isEqualTo(toId);
        assertThat(r.transferred()).isEqualByComparingTo("100.00");
        assertThat(r.feeCharged()).isEqualByComparingTo("1.00");
        assertThat(r.fromNewBalance()).isEqualByComparingTo("899.00");
        assertThat(r.toNewBalance()).isEqualByComparingTo("600.00");

        verify(accountRepo).save(from);
        verify(accountRepo).save(to);

        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(txRepo).save(captor.capture());
        var tx = captor.getValue();
        assertThat(tx.getAmount()).isEqualByComparingTo("100.00");
        assertThat(tx.getFee()).isEqualByComparingTo("1.00");
        assertThat(tx.getFromAccount().getId()).isEqualTo(fromId);
        assertThat(tx.getToAccount().getId()).isEqualTo(toId);
    }

    @Test
    void same_account_rejected() {
        var requester = UUID.randomUUID();
        var toSameAsFrom = acc(fromId, UUID.randomUUID(), "0.00");

        when(accountRepo.findByAccountNumber(toAccountNumber)).thenReturn(Optional.of(toSameAsFrom));

        assertThatThrownBy(() ->
                service.transfer(fromId, toAccountNumber, new BigDecimal("10"), requester))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(com.mybanking.app.common.error.ErrorCode.SAME_ACCOUNT));

        verify(accountRepo, never()).findByIdForUpdate(any());
        verifyNoInteractions(txRepo, feePolicy);
    }

    @Test
    void negative_amount_rejected() {
        when(accountRepo.findByAccountNumber(toAccountNumber))
                .thenReturn(Optional.of(acc(toId, UUID.randomUUID(), "0")));

        assertThatThrownBy(() ->
                service.transfer(fromId, toAccountNumber, new BigDecimal("-1"), UUID.randomUUID()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(com.mybanking.app.common.error.ErrorCode.VALIDATION_ERROR));

        verify(accountRepo, never()).findByIdForUpdate(any());
        verifyNoInteractions(txRepo, feePolicy);
    }

    @Test
    void source_not_found() {
        when(accountRepo.findByAccountNumber(toAccountNumber))
                .thenReturn(Optional.of(acc(toId, UUID.randomUUID(), "0")));
        when(accountRepo.findByIdForUpdate(fromId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.transfer(fromId, toAccountNumber, new BigDecimal("10"), UUID.randomUUID()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(com.mybanking.app.common.error.ErrorCode.ACCOUNT_NOT_FOUND));

        verify(accountRepo, never()).save(any());
        verifyNoInteractions(txRepo, feePolicy);
    }

    @Test
    void destination_not_found() {
        var requester = UUID.randomUUID();
        when(accountRepo.findByAccountNumber(toAccountNumber))
                .thenReturn(Optional.of(acc(toId, UUID.randomUUID(), "0")));
        when(accountRepo.findByIdForUpdate(fromId))
                .thenReturn(Optional.of(acc(fromId, requester, "100")));
        when(accountRepo.findByIdForUpdate(toId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.transfer(fromId, toAccountNumber, new BigDecimal("10"), requester))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(com.mybanking.app.common.error.ErrorCode.ACCOUNT_NOT_FOUND));

        verifyNoInteractions(txRepo, feePolicy);
    }

    @Test
    void not_owner_forbidden() {
        var requester = UUID.randomUUID();
        var otherOwner = UUID.randomUUID();

        when(accountRepo.findByAccountNumber(toAccountNumber))
                .thenReturn(Optional.of(acc(toId, UUID.randomUUID(), "0")));
        when(accountRepo.findByIdForUpdate(fromId))
                .thenReturn(Optional.of(acc(fromId, otherOwner, "100")));
        when(accountRepo.findByIdForUpdate(toId))
                .thenReturn(Optional.of(acc(toId, UUID.randomUUID(), "0")));

        assertThatThrownBy(() ->
                service.transfer(fromId, toAccountNumber, new BigDecimal("10"), requester))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(com.mybanking.app.common.error.ErrorCode.ACCOUNT_FORBIDDEN));

        verifyNoInteractions(txRepo, feePolicy);
    }

    @Test
    void insufficient_balance() {
        var requester = UUID.randomUUID();
        var from = acc(fromId, requester, "50.00");
        var to = acc(toId, UUID.randomUUID(), "0.00");

        when(accountRepo.findByAccountNumber(toAccountNumber)).thenReturn(Optional.of(to));
        when(accountRepo.findByIdForUpdate(fromId)).thenReturn(Optional.of(from));
        when(accountRepo.findByIdForUpdate(toId)).thenReturn(Optional.of(to));
        when(feePolicy.feeFor(eq(FeePolicy.Operation.TRANSFER), eq(from), eq(new BigDecimal("100.00"))))
                .thenReturn(new BigDecimal("1.00"));

        assertThatThrownBy(() ->
                service.transfer(fromId, toAccountNumber, new BigDecimal("100.00"), requester))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(com.mybanking.app.common.error.ErrorCode.INSUFFICIENT_BALANCE));

        verify(accountRepo, never()).save(any());
        verifyNoInteractions(txRepo);
    }
}
