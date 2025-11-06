package com.mybanking.app.user.entity;

import com.mybanking.app.account.entity.Account;
import com.mybanking.app.user.dto.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.*;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable=false, unique=true, length=32)
    private String customerNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Version
    private Long version;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Set<Role> roles = new HashSet<>();

}

