package com.mybanking.app.card.repository;

import com.mybanking.app.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

}
