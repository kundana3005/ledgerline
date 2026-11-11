package com.ledgerline.backend;

import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountStatusTest extends IntegrationTest {
    private User admin, alice, bob;
    private Account a1, b1;

    @BeforeEach void setUp() {
        admin = newUser(Role.ADMIN); alice = newUser(Role.ACCOUNT_HOLDER); bob = newUser(Role.ACCOUNT_HOLDER);
        a1 = newAccount(alice, "100.00"); b1 = newAccount(bob, "50.00");
    }

    private ResultActions setStatus(User who, Account a, String s) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put("/api/admin/accounts/" + a.getId() + "/status")
                .header("Authorization", bearer(who)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + s + "\"}"));
    }

    @Test void suspendedAccount_cannotTransact_butHistoryIsKept() throws Exception {
        long entries = count("select count(*) from ledger_entries where account_id = ?", a1.getId());
        setStatus(admin, a1, "SUSPENDED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));

        post(alice, WITHDRAW, key(), dep(a1, "10")).andExpect(status().is(422));
        post(alice, DEPOSIT, key(), dep(a1, "10")).andExpect(status().is(422));
        post(alice, TRANSFER, key(), xfer(a1, b1, "10")).andExpect(status().is(422));
        post(bob, TRANSFER, key(), xfer(b1, a1, "10")).andExpect(status().is(422));
        assertBalance("100.00", a1);
        assertBalance("50.00", b1);
        assertEquals(entries, count("select count(*) from ledger_entries where account_id = ?", a1.getId()));
        get(alice, "/api/accounts/" + a1.getId() + "/statement").andExpect(status().isOk());

        assertEquals(1L, count("select count(*) from audit_logs where action = 'ACCOUNT_STATUS_CHANGED' and entity_id = ?", a1.getAccountNumber()));
        assertEquals(1L, count("select count(*) from notifications where user_id = ? and type = 'ACCOUNT_SUSPENDED'", alice.getId()));

        setStatus(admin, a1, "ACTIVE").andExpect(status().isOk());
        post(alice, DEPOSIT, key(), dep(a1, "10")).andExpect(status().isOk());
    }

    @Test void closing_needsZeroBalance_andIsFinal() throws Exception {
        setStatus(admin, a1, "CLOSED").andExpect(status().is(422));
        post(alice, WITHDRAW, key(), dep(a1, "100.00")).andExpect(status().isOk());
        setStatus(admin, a1, "CLOSED").andExpect(status().isOk());
        post(alice, DEPOSIT, key(), dep(a1, "5")).andExpect(status().is(422));
        setStatus(admin, a1, "ACTIVE").andExpect(status().is(422));
    }

    @Test void pendingIsNotASettableTarget_andUnknownIsRejected() throws Exception {
        setStatus(admin, a1, "PENDING").andExpect(status().isBadRequest());
        setStatus(admin, a1, "FROZEN").andExpect(status().isBadRequest());
    }

    @Test void onlyAdminsMayChangeAccountStatus() throws Exception {
        setStatus(alice, a1, "SUSPENDED").andExpect(status().isForbidden());
        setStatus(newUser(Role.AUDITOR), a1, "SUSPENDED").andExpect(status().isForbidden());
        mvc.perform(MockMvcRequestBuilders.put("/api/admin/accounts/" + a1.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUSPENDED\"}")).andExpect(status().isUnauthorized());
        assertEquals("ACTIVE", jdbc.queryForObject("select status from accounts where id = ?", String.class, a1.getId()));
    }

    @Test void adminCannotMoveMoneyOrTouchTheSystemAccount() throws Exception {
        post(admin, DEPOSIT, key(), dep(a1, "5")).andExpect(status().isForbidden());
        Long system = jdbc.queryForObject("select id from accounts where account_number = 'SYSTEM-CASH'", Long.class);
        mvc.perform(MockMvcRequestBuilders.put("/api/admin/accounts/" + system + "/status")
                .header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SUSPENDED\"}")).andExpect(status().isBadRequest());
    }
}