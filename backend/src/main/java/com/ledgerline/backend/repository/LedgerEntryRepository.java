package com.ledgerline.backend.repository;

import com.ledgerline.backend.entity.EntryDirection;
import com.ledgerline.backend.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LedgerEntryRepository extends Repository<LedgerEntry, Long> {

    LedgerEntry save(LedgerEntry entry);

    List<LedgerEntry> findByTransactionIdOrderByIdAsc(Long transactionId);

    Optional<LedgerEntry> findFirstByAccount_IdAndCreatedAtBeforeOrderByIdDesc(Long accountId, Instant before);

    @Query("select e from LedgerEntry e join fetch e.transaction " +
           "where e.account.id = :accountId and e.createdAt >= :fromTs and e.createdAt < :toTs " +
           "order by e.id asc")
    List<LedgerEntry> statementEntries(@Param("accountId") Long accountId,
                                       @Param("fromTs") Instant fromTs, @Param("toTs") Instant toTs);

    @Query("select sum(e.amount) from LedgerEntry e where e.direction = :direction")
    BigDecimal totalByDirection(@Param("direction") EntryDirection direction);

    @EntityGraph(attributePaths = {"transaction", "account"})
    @Query(value = "select e from LedgerEntry e " +
                   "where lower(e.account.accountNumber) like :q or lower(e.transaction.reference) like :q",
           countQuery = "select count(e) from LedgerEntry e " +
                   "where lower(e.account.accountNumber) like :q or lower(e.transaction.reference) like :q")
    Page<LedgerEntry> search(@Param("q") String q, Pageable pageable);
}