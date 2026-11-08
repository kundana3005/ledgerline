package com.ledgerline.backend.service;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {
    private static final ZoneOffset UTC = ZoneOffset.UTC;

    private final TransactionRepository txs;
    private final LedgerEntryRepository ledgerRepo;
    private final AuditLogRepository auditRepo;
    private final AccountRepository accounts;
    private final UserRepository users;

    public ReportService(TransactionRepository txs, LedgerEntryRepository ledgerRepo,
                         AuditLogRepository auditRepo, AccountRepository accounts, UserRepository users) {
        this.txs = txs; this.ledgerRepo = ledgerRepo; this.auditRepo = auditRepo;
        this.accounts = accounts; this.users = users;
    }

    /** ownerId != null: only transactions touching that user's accounts. ownerId == null: all (auditor/admin). */
    @Transactional(readOnly = true)
    public PageResponse<TransactionSummary> searchTransactions(Long ownerId, TransactionFilter f, int page, int size) {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            Join<Transaction, Account> src = root.join("sourceAccount", JoinType.LEFT);
            Join<Transaction, Account> dst = root.join("destinationAccount", JoinType.LEFT);

            if (ownerId != null) {
                Join<Account, User> srcOwner = src.join("owner", JoinType.LEFT);
                Join<Account, User> dstOwner = dst.join("owner", JoinType.LEFT);
                where.add(cb.or(cb.equal(srcOwner.get("id"), ownerId), cb.equal(dstOwner.get("id"), ownerId)));
            }
            if (f.type() != null) where.add(cb.equal(root.get("type"), f.type()));
            if (f.status() != null) where.add(cb.equal(root.get("status"), f.status()));
            if (f.search() != null && !f.search().isBlank()) {
                String like = like(f.search());
                where.add(cb.or(
                        cb.like(cb.lower(root.get("reference")), like),
                        cb.like(cb.lower(src.get("accountNumber")), like),
                        cb.like(cb.lower(dst.get("accountNumber")), like)));
            }
            if (f.from() != null) {
                where.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), f.from().atStartOfDay(UTC).toInstant()));
            }
            if (f.to() != null) {
                where.add(cb.lessThan(root.<Instant>get("createdAt"), f.to().plusDays(1).atStartOfDay(UTC).toInstant()));
            }
            return cb.and(where.toArray(new Predicate[0]));
        };

        boolean includeInitiator = ownerId == null;   // do not reveal other customers' usernames to account holders
        Page<Transaction> result = txs.findAll(spec, pageable(page, size));
        return PageResponse.of(result.map(t -> toSummary(t, includeInitiator)));
    }

    @Transactional(readOnly = true)
    public StatementResponse statement(Long ownerId, Long accountId, LocalDate from, LocalDate to) {
        Account acc = accounts.findByIdAndOwner_Id(accountId, ownerId).orElseThrow(ApiException::notFound);

        LocalDate end = to != null ? to : LocalDate.now(UTC);
        LocalDate start = from != null ? from : end.minusDays(30);
        if (start.isAfter(end)) throw ApiException.badRequest("'from' must not be after 'to'");

        Instant fromTs = start.atStartOfDay(UTC).toInstant();
        Instant toTs = end.plusDays(1).atStartOfDay(UTC).toInstant();

        BigDecimal opening = ledgerRepo.findFirstByAccount_IdAndCreatedAtBeforeOrderByIdDesc(accountId, fromTs)
                .map(LedgerEntry::getBalanceAfter).orElse(BigDecimal.ZERO);
        List<LedgerEntry> entries = ledgerRepo.statementEntries(accountId, fromTs, toTs);
        BigDecimal closing = entries.isEmpty() ? opening : entries.get(entries.size() - 1).getBalanceAfter();

        List<StatementLine> lines = entries.stream()
                .map(e -> new StatementLine(e.getCreatedAt(), e.getTransaction().getReference(),
                        e.getTransaction().getType().name(), e.getDirection().name(),
                        e.getAmount(), e.getBalanceAfter()))
                .toList();

        return new StatementResponse(acc.getAccountNumber(), acc.getType().name(), start, end, opening, closing,
                sum(entries, EntryDirection.CREDIT), sum(entries, EntryDirection.DEBIT), lines);
    }

    @Transactional(readOnly = true)
    public PageResponse<LedgerEntryView> ledgerEntries(String search, int page, int size) {
        return PageResponse.of(ledgerRepo.search(like(search), pageable(page, size)).map(LedgerEntryView::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogView> auditLogs(String search, int page, int size) {
        return PageResponse.of(auditRepo.search(like(search), pageable(page, size)).map(AuditLogView::from));
    }

    /** REPEATABLE_READ = one consistent snapshot, so the debit/credit check never shows a false alarm. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AdminStats stats() {
        BigDecimal debits = nz(ledgerRepo.totalByDirection(EntryDirection.DEBIT));
        BigDecimal credits = nz(ledgerRepo.totalByDirection(EntryDirection.CREDIT));
        return new AdminStats(
                users.count(), users.countByRole(Role.ADMIN), users.countByRole(Role.ACCOUNT_HOLDER),
                users.countByRole(Role.AUDITOR),
                accounts.countByTypeNot(AccountType.SYSTEM), nz(accounts.totalBalanceExcluding(AccountType.SYSTEM)),
                txs.count(), txs.countByType(TransactionType.DEPOSIT), txs.countByType(TransactionType.WITHDRAWAL),
                txs.countByType(TransactionType.TRANSFER), nz(txs.totalVolume()),
                debits, credits, debits.compareTo(credits) == 0);
    }

    private TransactionSummary toSummary(Transaction t, boolean includeInitiator) {
        return new TransactionSummary(t.getId(), t.getReference(), t.getType().name(), t.getStatus().name(),
                t.getAmount(),
                t.getSourceAccount() == null ? null : t.getSourceAccount().getAccountNumber(),
                t.getDestinationAccount() == null ? null : t.getDestinationAccount().getAccountNumber(),
                includeInitiator ? t.getInitiatedBy().getUsername() : null,
                t.getCreatedAt());
    }

    private BigDecimal sum(List<LedgerEntry> entries, EntryDirection d) {
        return entries.stream().filter(e -> e.getDirection() == d)
                .map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private String like(String s) { return "%" + (s == null ? "" : s.trim().toLowerCase()) + "%"; }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));
    }
}