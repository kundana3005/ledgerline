package com.ledgerline.backend.repository;

import com.ledgerline.backend.entity.Transaction;
import com.ledgerline.backend.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import java.math.BigDecimal;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByInitiatedBy_IdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Override
    @EntityGraph(attributePaths = {"sourceAccount", "destinationAccount", "initiatedBy"})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    long countByType(TransactionType type);

    @Query("select sum(t.amount) from Transaction t")
    BigDecimal totalVolume();
}