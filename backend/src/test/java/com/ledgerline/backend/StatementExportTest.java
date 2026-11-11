package com.ledgerline.backend;

import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StatementExportTest extends IntegrationTest {
    private User alice, bob;
    private Account a, b;

    @BeforeEach void setUp() throws Exception {
        alice = newUser(Role.ACCOUNT_HOLDER); bob = newUser(Role.ACCOUNT_HOLDER);
        a = newAccount(alice, "100.00"); b = newAccount(bob, "50.00");
        post(alice, WITHDRAW, key(), dep(a, "30.00"));
    }

    private String url(Account acc, String format) { return "/api/accounts/" + acc.getId() + "/statement/export?format=" + format; }

    @Test void csv_hasBalancesReferencesAndBothDirections() throws Exception {
        var res = get(alice, url(a, "csv")).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment"))).andReturn().getResponse();
        assertTrue(res.getContentType().startsWith("text/csv"));
        String csv = res.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(csv.contains("Opening balance")); assertTrue(csv.contains("Closing balance"));
        assertTrue(csv.contains(a.getAccountNumber()));
        assertTrue(csv.contains("DEBIT")); assertTrue(csv.contains("CREDIT"));
        assertTrue(csv.contains("70.00"));
        assertFalse(csv.contains(b.getAccountNumber()), "must contain only the selected account");
    }

    @Test void pdf_isAValidPdfDocument() throws Exception {
        var res = get(alice, url(a, "pdf")).andExpect(status().isOk()).andReturn().getResponse();
        assertEquals("application/pdf", res.getContentType());
        byte[] bytes = res.getContentAsByteArray();
        assertTrue(bytes.length > 500);
        assertEquals("%PDF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test void otherUsersAccount_is404() throws Exception {
        get(alice, url(b, "csv")).andExpect(status().isNotFound());
        get(alice, url(b, "pdf")).andExpect(status().isNotFound());
    }

    @Test void needsLogin_andHolderRole() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(url(a, "csv"))).andExpect(status().isUnauthorized());
        get(newUser(Role.AUDITOR), url(a, "csv")).andExpect(status().isForbidden());
        get(newUser(Role.ADMIN), url(a, "csv")).andExpect(status().isForbidden());
    }

    @Test void unknownFormat_is400() throws Exception { get(alice, url(a, "xlsx")).andExpect(status().isBadRequest()); }

    @Test void downloadsAreAudited() throws Exception {
        get(alice, url(a, "csv")).andExpect(status().isOk());
        assertEquals(1L, count("select count(*) from audit_logs where action = 'STATEMENT_DOWNLOADED' and actor_id = ?", alice.getId()));
    }
}