package com.mybanking.app.account.service;

import com.mybanking.app.account.dto.AccountSummary;
import com.mybanking.app.account.entity.Account;
import com.mybanking.app.account.repository.AccountRepository;
import com.mybanking.app.common.error.AppException;
import com.mybanking.app.common.error.ErrorCode;
import com.mybanking.app.common.util.FeePolicy;
import com.mybanking.app.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock FeePolicy feePolicy;

    @InjectMocks AccountService service;

    private final UUID owner1Id = UUID.randomUUID();
    private final UUID owner2Id = UUID.randomUUID();
    private final UUID acc1Id = UUID.randomUUID();
    private final UUID acc2Id = UUID.randomUUID();
    private User owner1;
    private User owner2;
    private Account acc1_owner1;
    private Account acc2_owner2;

    @BeforeEach
    void setUp() {
        owner1 = new User();
        owner1.setId(owner1Id);

        owner2 = new User();
        owner2.setId(owner2Id);

        acc1_owner1 = Account.builder()
                .id(acc1Id)
                .accountNumber("NL01")
                .balance(new BigDecimal("0.00"))
                .user(owner1)
                .build();

        acc2_owner2 = Account.builder()
                .id(acc2Id)
                .accountNumber("NL02")
                .balance(new BigDecimal("0.00"))
                .user(owner2)
                .build();
    }

    @Test
    void listMine_maps_entities_to_summary() {
        acc1_owner1.setBalance(new BigDecimal("100.00"));
        var acc1b = Account.builder()
                .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .accountNumber("NL02")
                .balance(new BigDecimal("200.00"))
                .user(owner1)
                .build();
        when(accountRepository.findAllByUser_Id(owner1Id)).thenReturn(List.of(acc1_owner1, acc1b));

        List<AccountSummary> out = service.listMine(owner1Id);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).accountNumber()).isEqualTo("NL01");
        assertThat(out.get(1).balance()).isEqualByComparingTo("200.00");
    }

    @Test
    void withdraw_ok_applies_fee_and_saves() {
        acc1_owner1.setBalance(new BigDecimal("500.00"));
        when(accountRepository.findByIdForUpdate(acc1Id)).thenReturn(Optional.of(acc1_owner1));
        when(feePolicy.feeFor(eq(FeePolicy.Operation.WITHDRAW), eq(acc1_owner1), eq(new BigDecimal("100.00"))))
                .thenReturn(new BigDecimal("1.00"));

        var r = service.withdraw(acc1Id, new BigDecimal("100.00"), owner1Id);

        assertThat(acc1_owner1.getBalance()).isEqualByComparingTo("399.00");
        assertThat(r.accountId()).isEqualTo(acc1Id);
        assertThat(r.withdrawn()).isEqualByComparingTo("100.00");
        assertThat(r.feeCharged()).isEqualByComparingTo("1.00");
        assertThat(r.newBalance()).isEqualByComparingTo("399.00");
        verify(accountRepository).saveAndFlush(acc1_owner1);
    }

    @Test
    void withdraw_rejects_negative_amount() {
        assertThatThrownBy(() ->
                service.withdraw(acc1Id, new BigDecimal("-1"), owner1Id))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verifyNoInteractions(accountRepository, feePolicy);
    }

    @Test
    void withdraw_not_owner_forbidden() {
        acc1_owner1.setBalance(new BigDecimal("50.00"));
        when(accountRepository.findByIdForUpdate(acc1Id)).thenReturn(Optional.of(acc1_owner1));

        assertThatThrownBy(() ->
                service.withdraw(acc1Id, new BigDecimal("10.00"), owner2Id))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_FORBIDDEN));
    }

    @Test
    void withdraw_insufficient_balance() {
        acc1_owner1.setBalance(new BigDecimal("50.00"));
        when(accountRepository.findByIdForUpdate(acc1Id)).thenReturn(Optional.of(acc1_owner1));
        when(feePolicy.feeFor(eq(FeePolicy.Operation.WITHDRAW), eq(acc1_owner1), eq(new BigDecimal("100.00"))))
                .thenReturn(new BigDecimal("1.00"));

        assertThatThrownBy(() ->
                service.withdraw(acc1Id, new BigDecimal("100.00"), owner1Id))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));

        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void deposit_ok_applies_fee_and_saves() {
        acc1_owner1.setBalance(new BigDecimal("100.00"));
        when(accountRepository.findByIdForUpdate(acc1Id)).thenReturn(Optional.of(acc1_owner1));
        when(feePolicy.feeFor(eq(FeePolicy.Operation.DEPOSIT), eq(acc1_owner1), eq(new BigDecimal("50.00"))))
                .thenReturn(new BigDecimal("1.00"));

        var r = service.deposit(acc1Id, new BigDecimal("50.00"), owner1Id);

        assertThat(acc1_owner1.getBalance()).isEqualByComparingTo("149.00");
        assertThat(r.deposited()).isEqualByComparingTo("50.00");
        assertThat(r.feeCharged()).isEqualByComparingTo("1.00");
        assertThat(r.newBalance()).isEqualByComparingTo("149.00");
        verify(accountRepository).saveAndFlush(acc1_owner1);
    }

    @Test
    void deposit_too_small_after_fee_rejected() {
        acc1_owner1.setBalance(new BigDecimal("100.00"));
        when(accountRepository.findByIdForUpdate(acc1Id)).thenReturn(Optional.of(acc1_owner1));
        when(feePolicy.feeFor(eq(FeePolicy.Operation.DEPOSIT), eq(acc1_owner1), eq(new BigDecimal("1.00"))))
                .thenReturn(new BigDecimal("1.00"));

        assertThatThrownBy(() ->
                service.deposit(acc1Id, new BigDecimal("1.00"), owner1Id))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void getAllBalances_maps() {
        acc1_owner1.setBalance(new BigDecimal("10.00"));
        when(accountRepository.findAll()).thenReturn(List.of(acc1_owner1));

        var out = service.getAllBalances();

        assertThat(out).hasSize(1);
        assertThat(out.get(0).accountNumber()).isEqualTo("NL01");
    }
}
