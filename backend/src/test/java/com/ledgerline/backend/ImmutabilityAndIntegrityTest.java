package com.ledgerline.backend;

import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.repository.AuditLogRepository;
import com.ledgerline.backend.repository.LedgerEntryRepository;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.repository.CrudRepository;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImmutabilityAndIntegrityTest extends IntegrationTest {
    private User alice;
    private Account acc;

    @BeforeEach
    void setUp() throws Exception {
        alice = newUser(Role.ACCOUNT_HOLDER);
        acc = newAccount(alice, "100.00");
        post(alice, DEPOSIT, key(), dep(acc, "5.00"));   // makes sure ledger and audit tables have rows
    }

    private void assertAppendOnly(Runnable r) {
        DataAccessException e = assertThrows(DataAccessException.class, r::run);
        assertTrue(e.getMessage().contains("append-only"), "database trigger should have blocked it: " + e.getMessage());
    }

    @Test void ledgerEntries_cannotBeUpdated() {
        assertAppendOnly(() -> jdbc.update("update ledger_entries set amount = amount + 1 where account_id = ?", acc.getId()));
    }

    @Test void ledgerEntries_cannotBeDeleted() {
        assertAppendOnly(() -> jdbc.update("delete from ledger_entries where account_id = ?", acc.getId()));
    }

    @Test void ledgerEntries_cannotBeTruncated() {
        assertAppendOnly(() -> jdbc.execute("truncate table ledger_entries"));
    }

    @Test void auditLogs_cannotBeUpdatedDeletedOrTruncated() {
        assertAppendOnly(() -> jdbc.update("update audit_logs set action = 'TAMPERED' where actor_id = ?", alice.getId()));
        assertAppendOnly(() -> jdbc.update("delete from audit_logs where actor_id = ?", alice.getId()));
        assertAppendOnly(() -> jdbc.execute("truncate table audit_logs"));
    }

    @Test void databaseItselfRejectsNegativeBalance() {
        assertThrows(DataAccessException.class,
                () -> jdbc.update("update accounts set balance = -1 where account_number = ?", acc.getAccountNumber()));
        assertBalance("105.00", acc);
    }

    @Test void ledgerAndAuditRepositories_haveNoUpdateOrDeleteMethods() {
        for (Class<?> repo : List.of(LedgerEntryRepository.class, AuditLogRepository.class)) {
            assertFalse(CrudRepository.class.isAssignableFrom(repo), repo.getSimpleName() + " must not extend CrudRepository");
            for (Method m : repo.getMethods()) {
                String n = m.getName().toLowerCase();
                assertFalse(n.startsWith("delete") || n.startsWith("remove") || n.startsWith("update"),
                        repo.getSimpleName() + " exposes " + m.getName());
            }
        }
    }

    @Test void totalDebits_equalTotalCredits() {
        BigDecimal debits = jdbc.queryForObject("select coalesce(sum(amount),0) from ledger_entries where direction='DEBIT'", BigDecimal.class);
        BigDecimal credits = jdbc.queryForObject("select coalesce(sum(amount),0) from ledger_entries where direction='CREDIT'", BigDecimal.class);
        assertEquals(0, debits.compareTo(credits), "debits " + debits + " vs credits " + credits);
    }

    @Test void everyTransactionHasExactlyTwoBalancedEntries() {
        assertEquals(0L, count("select count(*) from transactions t where " +
                "(select count(*) from ledger_entries e where e.transaction_id = t.id) <> 2"));
        assertEquals(0L, count("select count(*) from (select transaction_id from ledger_entries group by transaction_id " +
                "having sum(case when direction='DEBIT' then amount else -amount end) <> 0) x"));
    }

    @Test void noMoneyIsCreatedOrLost_andBalancesMatchTheLedger() {
        BigDecimal total = jdbc.queryForObject("select coalesce(sum(balance),0) from accounts", BigDecimal.class);
        assertEquals(0, total.compareTo(BigDecimal.ZERO), "sum of all balances (including SYSTEM-CASH) must be 0");
        assertEquals(0L, count("select count(*) from accounts a where a.balance <> coalesce(" +
                "(select e.balance_after from ledger_entries e where e.account_id = a.id order by e.id desc limit 1), 0)"),
                "every account balance must equal its latest ledger balance_after");
        assertEquals(0L, count("select count(*) from accounts where type <> 'SYSTEM' and balance < 0"));
    }
}