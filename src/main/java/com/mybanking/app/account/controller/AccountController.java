package com.mybanking.app.account.controller;

import com.mybanking.app.account.dto.AccountSummary;
import com.mybanking.app.account.dto.AccountSummaryResponse;
import com.mybanking.app.account.dto.DepositRequest;
import com.mybanking.app.account.dto.DepositResponse;
import com.mybanking.app.account.dto.WithdrawRequest;
import com.mybanking.app.account.dto.WithdrawResponse;
import com.mybanking.app.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.mybanking.app.common.security.SecurityUtils.currentUserId;
import static com.mybanking.app.common.security.SecurityUtils.requireAdmin;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/admin/balances")
    public List<AccountSummary> getAllBalancesForAdmin() {
        requireAdmin();
        log.info("admin balances requested");
        return accountService.getAllBalances();
    }

    @GetMapping("/detail")
    public ResponseEntity<List<AccountSummaryResponse>> listMine() {
        var userId = currentUserId();
        log.info("listMine start userId={}", userId);
        var list = accountService.listMine(userId)
                .stream()
                .map(a -> new AccountSummaryResponse(
                        a.accountId(),
                        a.accountNumber(),
                        a.cardType(),
                        a.balance()
                ))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<WithdrawResponse> withdraw(@PathVariable UUID accountId,
                                                     @Valid @RequestBody WithdrawRequest body) {
        var userId = currentUserId();
        log.info("withdraw start accountId={} userId={} amount={}", accountId, userId, body.amount());
        var r = accountService.withdraw(accountId, body.amount(), userId);
        return ResponseEntity.ok(new WithdrawResponse(r.accountId(), r.withdrawn(), r.feeCharged(), r.newBalance()));
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<DepositResponse> deposit(@PathVariable UUID accountId,
                                                   @Valid @RequestBody DepositRequest body) {
        var userId = currentUserId();
        log.info("deposit start accountId={} userId={} amount={}", accountId, userId, body.amount());
        var r = accountService.deposit(accountId, body.amount(), userId);
        return ResponseEntity.ok(new DepositResponse(r.accountId(), r.deposited(), r.feeCharged(), r.newBalance()));
    }
}
