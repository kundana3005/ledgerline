package com.ledgerline.backend.repository;

import com.ledgerline.backend.entity.LedgerEntry;
import org.springframework.data.repository.Repository;
import java.util.List;

public interface LedgerEntryRepository extends Repository<LedgerEntry, Long> {
    LedgerEntry save(LedgerEntry entry);
    List<LedgerEntry> findByTransactionIdOrderByIdAsc(Long transactionId);
}