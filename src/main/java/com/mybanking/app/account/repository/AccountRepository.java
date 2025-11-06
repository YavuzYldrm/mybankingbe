package com.mybanking.app.account.repository;

import com.mybanking.app.account.entity.Account;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findAllByUser_Id(UUID userId);

    Optional<Account> findByAccountNumber(String accountNumber);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);
}
