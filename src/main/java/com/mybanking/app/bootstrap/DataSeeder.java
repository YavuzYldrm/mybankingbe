package com.mybanking.app.bootstrap;

import com.mybanking.app.account.entity.Account;
import com.mybanking.app.account.repository.AccountRepository;
import com.mybanking.app.card.repository.CardRepository;
import com.mybanking.app.card.entity.Card;
import com.mybanking.app.common.util.CardType;
import com.mybanking.app.user.dto.Role;
import com.mybanking.app.user.entity.User;
import com.mybanking.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final AccountRepository accountRepo;
    private final CardRepository cardRepo;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        var user = userRepo.save(User.builder()
                .customerNumber("NL0000000123")
                .name("Yavuz")
                .surname("Yildirim")
                .email("user@mybanking.nl")
                .password(encoder.encode("Pass123!"))
                .roles(Set.of(Role.ADMIN, Role.USER))
                .build());

        var debit = accountRepo.save(Account.builder()
                .user(user).accountNumber("NL00MB00020003000")
                .balance(new BigDecimal("750.00")).build());

        cardRepo.save(Card.builder()
                .account(debit).cardType(CardType.DEBIT)
                .cardNumber("4111 1111 1111 1111").build());

        var credit = accountRepo.save(Account.builder()
                .user(user).accountNumber("NL00MB00020003001")
                .balance(new BigDecimal("1200.00")).build());

        cardRepo.save(Card.builder()
                .account(credit).cardType(CardType.CREDIT)
                .cardNumber("5555 5555 5555 4444").build());

        //Second User
        var user2 = userRepo.save(User.builder()
                .customerNumber("NL0000000456")
                .name("Michael")
                .surname("Jackson")
                .email("user2@mybanking.nl")
                .password(encoder.encode("Pass123!"))
                .roles(Set.of(Role.USER))
                .build());

        var debit2 = accountRepo.save(Account.builder()
                .user(user2).accountNumber("NL00MB00020003010")
                .balance(new BigDecimal("500.00")).build());

        cardRepo.save(Card.builder()
                .account(debit2).cardType(CardType.DEBIT)
                .cardNumber("4000 0000 0000 0002").build());

        var credit2 = accountRepo.save(Account.builder()
                .user(user2).accountNumber("NL00MB00020003011")
                .balance(new BigDecimal("300.00")).build());

        cardRepo.save(Card.builder()
                .account(credit2).cardType(CardType.CREDIT)
                .cardNumber("5105 1051 0510 5102").build());
    }
}
