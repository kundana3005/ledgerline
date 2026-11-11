package com.ledgerline.backend;

import com.jayway.jsonpath.JsonPath;
import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransactionSearchTest extends IntegrationTest {
    private User alice, bob;
    private Account a;
    private String depositRef;

    @BeforeEach void setUp() throws Exception {
        alice = newUser(Role.ACCOUNT_HOLDER); bob = newUser(Role.ACCOUNT_HOLDER);
        a = newAccount(alice, null);
        depositRef = JsonPath.read(post(alice, DEPOSIT, key(), dep(a, "10.00")).andReturn().getResponse().getContentAsString(), "$.reference");
        post(alice, DEPOSIT, key(), dep(a, "20.00"));
        post(alice, DEPOSIT, key(), dep(a, "30.00"));
        post(alice, WITHDRAW, key(), dep(a, "5.00"));
    }

    private String body(User u, String query) throws Exception {
        return get(u, "/api/transactions" + query).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
    private int total(User u, String query) throws Exception {
        return ((Number) JsonPath.read(body(u, query), "$.totalElements")).intValue();
    }

    @Test void filtersByAmountRange() throws Exception { assertEquals(1, total(alice, "?minAmount=15&maxAmount=25")); }
    @Test void filtersByType() throws Exception { assertEquals(1, total(alice, "?type=WITHDRAWAL")); assertEquals(3, total(alice, "?type=DEPOSIT")); }
    @Test void filtersByReference() throws Exception { assertEquals(1, total(alice, "?search=" + depositRef)); }

    @Test void filtersByTransactionId() throws Exception {
        List<Integer> ids = JsonPath.read(body(alice, "?size=100"), "$.content[*].id");
        assertEquals(4, ids.size());
        assertEquals(1, total(alice, "?id=" + ids.get(0)));
    }

    @Test void filtersByDateRange() throws Exception {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        assertEquals(4, total(alice, "?from=" + today + "&to=" + today));
        assertEquals(0, total(alice, "?from=" + LocalDate.now(ZoneOffset.UTC).plusDays(1)));
    }

    @Test void sortsByAmountBothWays() throws Exception {
        List<Object> asc = JsonPath.read(body(alice, "?sort=amount&dir=asc"), "$.content[*].amount");
        List<Object> desc = JsonPath.read(body(alice, "?sort=amount&dir=desc"), "$.content[*].amount");
        assertEquals(0, new BigDecimal(asc.get(0).toString()).compareTo(new BigDecimal("5")));
        assertEquals(0, new BigDecimal(desc.get(0).toString()).compareTo(new BigDecimal("30")));
    }

    @Test void paginates() throws Exception {
        String first = body(alice, "?size=3&page=0");
        assertEquals(4, ((Number) JsonPath.read(first, "$.totalElements")).intValue());
        assertEquals(2, ((Number) JsonPath.read(first, "$.totalPages")).intValue());
        assertEquals(3, ((List<?>) JsonPath.read(first, "$.content")).size());
        assertEquals(1, ((List<?>) JsonPath.read(body(alice, "?size=3&page=1"), "$.content")).size());
    }

    @Test void unknownSortField_is400() throws Exception {
        get(alice, "/api/transactions?sort=password_hash").andExpect(status().isBadRequest());
    }

    @Test void holdersNeverSeeOthersTransactions_butAuditorsDo() throws Exception {
        Account b = newAccount(bob, null);
        String ref = JsonPath.read(post(bob, DEPOSIT, key(), dep(b, "7.00")).andReturn().getResponse().getContentAsString(), "$.reference");
        Long bobTx = jdbc.queryForObject("select id from transactions where reference = ?", Long.class, ref);
        assertEquals(0, total(alice, "?id=" + bobTx));
        assertEquals(0, total(alice, "?search=" + ref));
        User auditor = newUser(Role.AUDITOR);
        get(auditor, "/api/auditor/transactions?id=" + bobTx).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }
}