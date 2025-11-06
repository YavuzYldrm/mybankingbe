package com.mybanking.app.transaction.controller;

import com.mybanking.app.transaction.dto.TransferRequest;
import com.mybanking.app.transaction.dto.TransferResponse;
import com.mybanking.app.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.mybanking.app.common.security.SecurityUtils.currentUserId;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest body) {
        var userId = currentUserId();
        log.info("transfer start from={} to={} amount={}", body.fromAccountId(), body.toAccountNumber(), body.amount());

        var r = transactionService.transfer(body.fromAccountId(), body.toAccountNumber(), body.amount(), userId);

        return ResponseEntity.ok(new TransferResponse(
                r.fromAccountId(),
                r.toAccountId(),
                r.transferred(),
                r.feeCharged(),
                r.fromNewBalance(),
                r.toNewBalance()
        ));
    }
}
