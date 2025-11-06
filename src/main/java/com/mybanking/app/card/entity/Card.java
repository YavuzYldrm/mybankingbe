package com.mybanking.app.card.entity;

import com.mybanking.app.account.entity.Account;
import com.mybanking.app.common.util.CardType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(
        name = "cards",
        indexes = {
                @Index(name = "ix_card_number", columnList = "cardNumber", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Card {

    @Id
    @GeneratedValue
    @UuidGenerator
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, unique = true)
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CardType cardType;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    @ToString.Exclude
    private Account account;
}
