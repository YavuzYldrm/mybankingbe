package com.mybanking.app.transaction.repository;

import com.mybanking.app.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

}
