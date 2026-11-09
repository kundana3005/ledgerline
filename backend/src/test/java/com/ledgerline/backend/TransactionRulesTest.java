package com.ledgerline.backend;

import com.jayway.jsonpath.JsonPath;
import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransactionRulesTest extends IntegrationTest {
    private User alice, bob;
    private Account a1, b1;

    @BeforeEach
    void setUp() {
        alice = newUser(Role.ACCOUNT_HOLDER);
        bob = newUser(Role.ACCOUNT_HOLDER);
        a1 = newAccount(alice, "100.00");
        b1 = newAccount(bob, "50.00");
    }

    private static String ref(String body) { return JsonPath.read(body, "$.reference"); }

    @Test void deposit_writesBalancedDoubleEntry() throws Exception {
        String body = post(alice, DEPOSIT, key(), dep(a1, "250.00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();
        assertBalance("350.00", a1);

        String ref = ref(body);
        assertEquals(2L, count("select count(*) from ledger_entries e join transactions t on t.id = e.transaction_id where t.reference = ?", ref));
        BigDecimal net = jdbc.queryForObject(
                "select coalesce(sum(case when e.direction='DEBIT' then e.amount else -e.amount end),0) " +
                "from ledger_entries e join transactions t on t.id = e.transaction_id where t.reference = ?",
                BigDecimal.class, ref);
        assertEquals(0, net.compareTo(BigDecimal.ZERO), "debits must equal credits");
    }

    @Test void transfer_movesExactAmount_andAuditsIt() throws Exception {
        String body = post(alice, TRANSFER, key(), xfer(a1, b1, "40.50"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertBalance("59.50", a1);
        assertBalance("90.50", b1);
        assertEquals(1L, count("select count(*) from audit_logs where action='TRANSFER_COMPLETED' and entity_id=?", ref(body)));
    }

    @Test void withdrawal_reducesBalance() throws Exception {
        post(alice, WITHDRAW, key(), dep(a1, "30.00")).andExpect(status().isOk());
        assertBalance("70.00", a1);
    }

    @Test void duplicateRequest_sameKey_isProcessedOnce() throws Exception {
        String k = key();
        String first = post(alice, DEPOSIT, k, dep(a1, "40.00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(false))
                .andReturn().getResponse().getContentAsString();
        String second = post(alice, DEPOSIT, k, dep(a1, "40.00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true))
                .andReturn().getResponse().getContentAsString();
        assertEquals(ref(first), ref(second));
        assertBalance("140.00", a1);
        assertEquals(1L, count("select count(*) from transactions where idempotency_key = ?", k));
    }

    @Test void sameKey_differentAmount_is409() throws Exception {
        String k = key();
        post(alice, DEPOSIT, k, dep(a1, "40.00")).andExpect(status().isOk());
        post(alice, DEPOSIT, k, dep(a1, "999.00")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IDEMPOTENCY_CONFLICT"));
        assertBalance("140.00", a1);
    }

    @Test void missingOrMalformedIdempotencyKey_is400() throws Exception {
        post(alice, DEPOSIT, null, dep(a1, "10.00")).andExpect(status().isBadRequest());
        post(alice, DEPOSIT, "short", dep(a1, "10.00")).andExpect(status().isBadRequest());
        assertBalance("100.00", a1);
    }

    @Test void negativeZeroAndFractionalAmounts_are400() throws Exception {
        post(alice, DEPOSIT, key(), dep(a1, "-5")).andExpect(status().isBadRequest());
        post(alice, WITHDRAW, key(), dep(a1, "-5")).andExpect(status().isBadRequest());
        post(alice, TRANSFER, key(), xfer(a1, b1, "-5")).andExpect(status().isBadRequest());
        post(alice, DEPOSIT, key(), dep(a1, "0")).andExpect(status().isBadRequest());
        post(alice, DEPOSIT, key(), dep(a1, "10.005")).andExpect(status().isBadRequest());
        post(alice, DEPOSIT, key(), "{\"accountNumber\":\"" + a1.getAccountNumber() + "\"}").andExpect(status().isBadRequest());
        assertBalance("100.00", a1);
    }

    @Test void insufficientFunds_is422_andNothingChanges_butFailureIsAudited() throws Exception {
        long txBefore = count("select count(*) from transactions where initiated_by = ?", alice.getId());
        post(alice, WITHDRAW, key(), dep(a1, "999.00")).andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));
        post(alice, TRANSFER, key(), xfer(a1, b1, "100.01")).andExpect(status().is(422));

        assertBalance("100.00", a1);
        assertBalance("50.00", b1);
        assertEquals(txBefore, count("select count(*) from transactions where initiated_by = ?", alice.getId()));
        assertEquals(1L, count("select count(*) from audit_logs where actor_id = ? and action = 'WITHDRAWAL_FAILED'", alice.getId()));
    }

    @Test void transferFromSomeoneElsesAccount_is404_andNothingChanges() throws Exception {
        post(alice, TRANSFER, key(), xfer(b1, a1, "10.00")).andExpect(status().isNotFound());
        assertBalance("100.00", a1);
        assertBalance("50.00", b1);
    }

    @Test void depositIntoOrWithdrawFromSomeoneElsesAccount_is404() throws Exception {
        post(alice, DEPOSIT, key(), dep(b1, "10.00")).andExpect(status().isNotFound());
        post(alice, WITHDRAW, key(), dep(b1, "10.00")).andExpect(status().isNotFound());
        assertBalance("50.00", b1);
    }

    @Test void cannotTouchTheSystemAccount() throws Exception {
        post(alice, WITHDRAW, key(), "{\"accountNumber\":\"SYSTEM-CASH\",\"amount\":1}").andExpect(status().isNotFound());
        post(alice, TRANSFER, key(),
                "{\"sourceAccount\":\"" + a1.getAccountNumber() + "\",\"destinationAccount\":\"SYSTEM-CASH\",\"amount\":1}")
                .andExpect(status().isNotFound());
        assertBalance("100.00", a1);
    }

    @Test void transferToSelf_is400_andUnknownAccount_is404() throws Exception {
        post(alice, TRANSFER, key(), xfer(a1, a1, "5.00")).andExpect(status().isBadRequest());
        post(alice, TRANSFER, key(),
                "{\"sourceAccount\":\"" + a1.getAccountNumber() + "\",\"destinationAccount\":\"LL0000000000\",\"amount\":5}")
                .andExpect(status().isNotFound());
        assertBalance("100.00", a1);
    }

    @Test void frozenAccount_cannotTransact() throws Exception {
        jdbc.update("update accounts set status = 'FROZEN' where account_number = ?", a1.getAccountNumber());
        post(alice, WITHDRAW, key(), dep(a1, "10.00")).andExpect(status().is(422));
        post(alice, DEPOSIT, key(), dep(a1, "10.00")).andExpect(status().is(422));
        post(bob, TRANSFER, key(), xfer(b1, a1, "10.00")).andExpect(status().is(422));
        assertBalance("100.00", a1);
        assertBalance("50.00", b1);
    }

    @Test void statementOfSomeoneElsesAccount_is404() throws Exception {
        get(alice, "/api/accounts/" + b1.getId() + "/statement").andExpect(status().isNotFound());
        get(alice, "/api/accounts/" + a1.getId() + "/statement").andExpect(status().isOk());
    }

    @Test void history_neverShowsOtherCustomersTransactions() throws Exception {
        String bobsDeposit = ref(post(bob, DEPOSIT, key(), dep(b1, "5.00"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        get(alice, "/api/transactions?size=100")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.reference=='" + bobsDeposit + "')]").isEmpty());
        get(bob, "/api/transactions?size=100")
                .andExpect(jsonPath("$.content[?(@.reference=='" + bobsDeposit + "')]").isNotEmpty());
    }
}