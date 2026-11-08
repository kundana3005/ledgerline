package com.ledgerline.backend.repository;

import com.ledgerline.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByInitiatedBy_IdAndIdempotencyKey(Long userId, String idempotencyKey);
}