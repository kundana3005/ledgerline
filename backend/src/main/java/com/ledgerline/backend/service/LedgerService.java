package com.ledgerline.backend.service;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LedgerService {
    public static final String SYSTEM_ACCOUNT = "SYSTEM-CASH";
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000.00");

    private final AccountRepository accounts;
    private final TransactionRepository txs;
    private final LedgerEntryRepository ledger;
    private final UserRepository users;
    private final AuditService audit;

    public LedgerService(AccountRepository accounts, TransactionRepository txs,
                         LedgerEntryRepository ledger, UserRepository users, AuditService audit) {
        this.accounts = accounts; this.txs = txs; this.ledger = ledger;
        this.users = users; this.audit = audit;
    }

    // Deposit: DEBIT system cash, CREDIT the customer account
    @Transactional
    public TransactionResponse deposit(Long actorId, String key, DepositRequest r) {
        return execute(actorId, key, TransactionType.DEPOSIT, SYSTEM_ACCOUNT, r.accountNumber(), r.amount());
    }

    // Withdrawal: DEBIT the customer account, CREDIT system cash
    @Transactional
    public TransactionResponse withdraw(Long actorId, String key, WithdrawRequest r) {
        return execute(actorId, key, TransactionType.WITHDRAWAL, r.accountNumber(), SYSTEM_ACCOUNT, r.amount());
    }

    // Transfer: DEBIT source, CREDIT destination
    @Transactional
    public TransactionResponse transfer(Long actorId, String key, TransferRequest r) {
        return execute(actorId, key, TransactionType.TRANSFER, r.sourceAccount(), r.destinationAccount(), r.amount());
    }

    @Transactional(readOnly = true)
    public Optional<TransactionResponse> findByKey(Long actorId, String key) {
        return txs.findByInitiatedBy_IdAndIdempotencyKey(actorId, key)
                .map(t -> TransactionResponse.from(t, true));
    }

    private TransactionResponse execute(Long actorId, String key, TransactionType type,
                                        String srcNumber, String dstNumber, BigDecimal rawAmount) {
        BigDecimal amount = normalize(rawAmount);
        String hash = sha256(type + "|" + srcNumber + "|" + dstNumber + "|" + amount.toPlainString());

        // 1. Idempotency: same key + same request => return the original result, do nothing
        Optional<Transaction> existing = txs.findByInitiatedBy_IdAndIdempotencyKey(actorId, key);
        if (existing.isPresent()) {
            Transaction t = existing.get();
            if (!t.getRequestHash().equals(hash)) throw ApiException.idempotencyConflict();
            return TransactionResponse.from(t, true);
        }

        // 2. Resolve ids first (no entity loaded yet, so no stale balances)
        Long srcId = accounts.findIdByAccountNumber(srcNumber).orElseThrow(ApiException::notFound);
        Long dstId = accounts.findIdByAccountNumber(dstNumber).orElseThrow(ApiException::notFound);
        if (srcId.equals(dstId)) throw ApiException.badRequest("Source and destination must be different accounts");

        // 3. Lock both rows in id order (blocks simultaneous withdrawals, prevents deadlocks)
        Map<Long, Account> locked = accounts.lockAllByIdIn(List.of(srcId, dstId)).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        Account src = locked.get(srcId);
        Account dst = locked.get(dstId);
        if (src == null || dst == null) throw ApiException.notFound();

        // 4. Ownership and status checks on the fresh, locked rows
        switch (type) {
            case DEPOSIT -> requireOwner(dst, actorId);
            case WITHDRAWAL -> requireOwner(src, actorId);
            case TRANSFER -> {
                requireOwner(src, actorId);
                if (dst.getType() == AccountType.SYSTEM) throw ApiException.notFound();
            }
        }
        if (src.getStatus() != AccountStatus.ACTIVE || dst.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.accountNotActive();
        }
        if (src.getType() != AccountType.SYSTEM && src.getBalance().compareTo(amount) < 0) {
            throw ApiException.insufficientFunds();
        }

        // 5. Move the money
        src.setBalance(src.getBalance().subtract(amount));
        dst.setBalance(dst.getBalance().add(amount));

        // 6. Record the transaction (unique key violation here = a concurrent duplicate) and both ledger entries
        User actor = users.getReferenceById(actorId);
        Transaction tx = txs.saveAndFlush(Transaction.completed(type, amount, src, dst, actor, key, hash));
        ledger.save(new LedgerEntry(tx, src, EntryDirection.DEBIT, amount, src.getBalance()));
        ledger.save(new LedgerEntry(tx, dst, EntryDirection.CREDIT, amount, dst.getBalance()));

        // 7. Audit record joins this transaction: all or nothing
        audit.log(actorId, type + "_COMPLETED", "TRANSACTION", tx.getReference(),
                Map.of("amount", amount.toPlainString(), "from", srcNumber, "to", dstNumber));

        return TransactionResponse.from(tx, false);
    }

    private void requireOwner(Account a, Long actorId) {
        // 404 (not 403) so users cannot discover which account numbers exist
        if (a.getOwner() == null || !a.getOwner().getId().equals(actorId)) throw ApiException.notFound();
    }

    private BigDecimal normalize(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw ApiException.badRequest("Amount must be greater than zero");
        if (amount.stripTrailingZeros().scale() > 2) throw ApiException.badRequest("Amount can have at most 2 decimal places");
        if (amount.compareTo(MAX_AMOUNT) > 0) throw ApiException.badRequest("Amount exceeds the maximum allowed");
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}