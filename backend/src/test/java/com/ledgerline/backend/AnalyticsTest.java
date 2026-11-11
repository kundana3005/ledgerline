package com.ledgerline.backend;

import com.jayway.jsonpath.JsonPath;
import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AnalyticsTest extends IntegrationTest {
    private static BigDecimal bd(Object o) { return new BigDecimal(o.toString()); }

    @Test void holderAnalytics_matchTheLedger() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER); Account a = newAccount(u, null);
        post(u, DEPOSIT, key(), dep(a, "100.00")); post(u, WITHDRAW, key(), dep(a, "30.00"));
        String body = get(u, "/api/analytics/me").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(0, bd(JsonPath.read(body, "$.currentBalance")).compareTo(new BigDecimal("70")));
        assertEquals(0, bd(JsonPath.read(body, "$.totalReceived")).compareTo(new BigDecimal("100")));
        assertEquals(0, bd(JsonPath.read(body, "$.totalSent")).compareTo(new BigDecimal("30")));
        assertFalse(((List<?>) JsonPath.read(body, "$.monthly")).isEmpty());
        List<Object> counts = JsonPath.read(body, "$.typeDistribution[*].transactions");
        assertEquals(2, counts.stream().mapToInt(c -> ((Number) c).intValue()).sum());
    }

    @Test void holdersOnlySeeTheirOwnNumbers() throws Exception {
        User a = newUser(Role.ACCOUNT_HOLDER), b = newUser(Role.ACCOUNT_HOLDER);
        newAccount(a, "500.00");
        String body = get(b, "/api/analytics/me").andReturn().getResponse().getContentAsString();
        assertEquals(0, bd(JsonPath.read(body, "$.totalReceived")).compareTo(BigDecimal.ZERO));
    }

    @Test void adminAnalytics_matchTheDatabase() throws Exception {
        User admin = newUser(Role.ADMIN); newAccount(newUser(Role.ACCOUNT_HOLDER), "40.00");
        String body = get(admin, "/api/admin/analytics?days=7").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Object> perType = JsonPath.read(body, "$.byType[*].transactions");
        assertEquals(count("select count(*) from transactions"), perType.stream().mapToLong(c -> ((Number) c).longValue()).sum());
        assertNotNull(JsonPath.read(body, "$.daily")); assertNotNull(JsonPath.read(body, "$.accountStatus"));
    }

    @Test void analyticsAreRoleProtected() throws Exception {
        User holder = newUser(Role.ACCOUNT_HOLDER), auditor = newUser(Role.AUDITOR), admin = newUser(Role.ADMIN);
        get(holder, "/api/admin/analytics").andExpect(status().isForbidden());
        get(auditor, "/api/admin/analytics").andExpect(status().isForbidden());
        get(auditor, "/api/analytics/me").andExpect(status().isForbidden());
        get(admin, "/api/analytics/me").andExpect(status().isForbidden());
    }
}