package com.mybanking.app.e2e;

import com.mybanking.app.account.entity.Account;
import com.mybanking.app.account.repository.AccountRepository;
import com.mybanking.app.account.service.AccountService;
import com.mybanking.app.transaction.service.TransactionService;
import com.mybanking.app.user.entity.User;
import com.mybanking.app.user.repository.UserRepository;
import com.mybanking.app.user.dto.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback
class FundsLifecycleFlowIT {

    @Autowired UserRepository userRepo;
    @Autowired AccountRepository accountRepo;
    @Autowired AccountService accountService;
    @Autowired TransactionService txService;

    @Test
    void user_creates_account_deposits_transfers_and_withdraws() {
        // Arrange: two users and two accounts
        var u1 = user("user@mybanking.com", "Yavuz", "Yildirim");
        var u2 = user("user2@mybanking.com", "Michael", "Jackson");
        userRepo.save(u1);
        userRepo.save(u2);

        var a1 = account(u1, "NL" + UUID.randomUUID().toString().substring(0, 10), "0.00");
        var a2 = account(u2, "NL" + UUID.randomUUID().toString().substring(0, 10), "0.00");
        accountRepo.save(a1);
        accountRepo.save(a2);

        // 1) Deposit: 1,000.00
        accountService.deposit(a1.getId(), bd("1000.00"), u1.getId());

        // 2) Transfer: 200.00 from a1 to a2 (use accountNumber for destination)
        txService.transfer(a1.getId(), a2.getAccountNumber(), bd("200.00"), u1.getId());

        // 3) Withdraw: 50.00 from a1
        accountService.withdraw(a1.getId(), bd("50.00"), u1.getId());

        // Assert balances (assuming zero fees without a card)
        var reloadedA1 = accountRepo.findById(a1.getId()).orElseThrow();
        var reloadedA2 = accountRepo.findById(a2.getId()).orElseThrow();

        assertThat(reloadedA1.getBalance()).isEqualByComparingTo("750.00"); // 0 +1000 -200 -50
        assertThat(reloadedA2.getBalance()).isEqualByComparingTo("200.00"); // 0 +200
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    private static User user(String email, String name, String surname) {
        var u = new User();
        u.setEmail(email);
        u.setName(name);
        u.setSurname(surname);
        u.setPassword("{noop}test");
        u.setCustomerNumber("CUST-" + UUID.randomUUID().toString().substring(0,8));
        u.setRoles(Set.of(Role.USER));
        return u;
    }

    private static Account account(User owner, String number, String balance) {
        var a = new Account();
        a.setUser(owner);
        a.setAccountNumber(number);
        a.setBalance(new BigDecimal(balance));
        return a;
    }
}
