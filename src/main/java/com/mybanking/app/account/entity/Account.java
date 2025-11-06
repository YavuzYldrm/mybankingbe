package com.mybanking.app.account.entity;

import com.mybanking.app.card.entity.Card;
import com.mybanking.app.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "ix_account_number", columnList = "accountNumber", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 32, unique = true)
    private String accountNumber;

    @Column(nullable = false,precision = 19, scale = 2)
    private BigDecimal balance;

    @Version
    private Long version;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Card card;

    public void setBalance(BigDecimal v) {
        this.balance = (v == null) ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    public void depositCore(BigDecimal amount) {
        requirePositive(amount, "Amount");
        balance = balance.add(amount);
    }

    private static void requirePositive(BigDecimal v, String amount) {
        if (v == null || v.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(amount + " must be positive");
        }
    }

    public void withdrawWithFeeOrThrow(BigDecimal amount, BigDecimal fee, java.util.function.Supplier<RuntimeException> onInsufficient) {
        BigDecimal total = amount.add(fee);
        if (this.getBalance().compareTo(total) < 0) throw onInsufficient.get();
        this.setBalance(this.getBalance().subtract(total));
    }
}
