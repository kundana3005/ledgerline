package com.ledgerline.backend;

import com.jayway.jsonpath.JsonPath;
import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminApprovalTest extends IntegrationTest {
    private static final String REQUESTS = "/api/onboarding/account-requests";
    private User admin;

    @BeforeEach void setUp() { admin = newUser(Role.ADMIN); }

    private User pendingUser() {
        User u = newUser(Role.ACCOUNT_HOLDER);
        jdbc.update("update users set status = 'PENDING_APPROVAL' where id = ?", u.getId());
        return u;
    }

    private ResultActions putJson(User u, String path, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put(path).header("Authorization", bearer(u))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String userStatus(User u) {
        return jdbc.queryForObject("select status from users where id = ?", String.class, u.getId());
    }

    private String approval(User u) { return "/api/admin/users/" + u.getId() + "/approval"; }

    private Long requestFor(User holder, String type) throws Exception {
        post(holder, REQUESTS, null, "{\"type\":\"" + type + "\"}").andExpect(status().isCreated());
        return jdbc.queryForObject("select id from account_requests where user_id = ?", Long.class, holder.getId());
    }

    private String decision(Long id) { return "/api/admin/account-requests/" + id + "/decision"; }

    @Test void approveRegistration_activatesUser_andIsAudited() throws Exception {
        User u = pendingUser();
        putJson(admin, approval(u), "{\"approved\":true}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        assertEquals("ACTIVE", userStatus(u));
        assertEquals(1L, count("select count(*) from audit_logs where action = 'USER_APPROVED' and entity_id = ? and actor_id = ?",
                String.valueOf(u.getId()), admin.getId()));
        get(u, "/api/accounts").andExpect(status().isOk());
    }

    @Test void rejectRegistration_needsReason_andStoresIt() throws Exception {
        User u = pendingUser();
        putJson(admin, approval(u), "{\"approved\":false}").andExpect(status().isBadRequest());
        putJson(admin, approval(u), "{\"approved\":false,\"reason\":\"   \"}").andExpect(status().isBadRequest());
        assertEquals("PENDING_APPROVAL", userStatus(u));

        putJson(admin, approval(u), "{\"approved\":false,\"reason\":\"Phone number could not be verified\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        assertEquals("REJECTED", userStatus(u));
        assertEquals("Phone number could not be verified",
                jdbc.queryForObject("select rejection_reason from users where id = ?", String.class, u.getId()));
        assertEquals(1L, count("select count(*) from audit_logs where action = 'USER_REJECTED' and entity_id = ?",
                String.valueOf(u.getId())));
        get(u, "/api/accounts").andExpect(status().isForbidden());
    }

    @Test void registrationReviewedTwice_is422() throws Exception {
        User u = pendingUser();
        putJson(admin, approval(u), "{\"approved\":true}").andExpect(status().isOk());
        putJson(admin, approval(u), "{\"approved\":true}").andExpect(status().is(422));
        putJson(admin, approval(u), "{\"approved\":false,\"reason\":\"changed my mind\"}").andExpect(status().is(422));
        assertEquals("ACTIVE", userStatus(u));
    }

    @Test void reviewEndpoints_areAdminOnly() throws Exception {
        User target = pendingUser();
        User holder = newUser(Role.ACCOUNT_HOLDER), auditor = newUser(Role.AUDITOR), applicant = pendingUser();
        for (User who : new User[]{holder, auditor, applicant}) {
            putJson(who, approval(target), "{\"approved\":true}").andExpect(status().isForbidden());
            putJson(who, decision(1L), "{\"approved\":true}").andExpect(status().isForbidden());
            get(who, "/api/admin/account-requests").andExpect(status().isForbidden());
        }
        mvc.perform(MockMvcRequestBuilders.put(approval(target)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"approved\":true}")).andExpect(status().isUnauthorized());
        assertEquals("PENDING_APPROVAL", userStatus(target));
    }

    @Test void approveAccountRequest_createsEmptyActiveAccount() throws Exception {
        User holder = newUser(Role.ACCOUNT_HOLDER);
        Long id = requestFor(holder, "SAVINGS");
        putJson(admin, decision(id), "{\"approved\":true}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));

        assertEquals(1L, count("select count(*) from accounts where owner_id = ?", holder.getId()));
        var row = jdbc.queryForMap("select type, status, balance from accounts where owner_id = ?", holder.getId());
        assertEquals("SAVINGS", row.get("type"));
        assertEquals("ACTIVE", row.get("status"));
        assertEquals(0, ((BigDecimal) row.get("balance")).compareTo(BigDecimal.ZERO));
        assertEquals(1L, count("select count(*) from audit_logs where action = 'ACCOUNT_REQUEST_APPROVED' and entity_id = ?",
                String.valueOf(id)));
        get(holder, "/api/accounts").andExpect(jsonPath("$.length()").value(1));
    }

    @Test void rejectAccountRequest_needsReason_andCreatesNothing() throws Exception {
        User holder = newUser(Role.ACCOUNT_HOLDER);
        Long id = requestFor(holder, "CHECKING");
        putJson(admin, decision(id), "{\"approved\":false}").andExpect(status().isBadRequest());
        putJson(admin, decision(id), "{\"approved\":false,\"reason\":\"Incomplete documents\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        assertEquals(0L, count("select count(*) from accounts where owner_id = ?", holder.getId()));
        assertEquals(1L, count("select count(*) from audit_logs where action = 'ACCOUNT_REQUEST_REJECTED' and entity_id = ?",
                String.valueOf(id)));
        post(holder, REQUESTS, null, "{\"type\":\"CHECKING\"}").andExpect(status().isCreated());   // may apply again
    }

    @Test void accountRequestDecidedTwice_is422_andCreatesOneAccount() throws Exception {
        User holder = newUser(Role.ACCOUNT_HOLDER);
        Long id = requestFor(holder, "CHECKING");
        putJson(admin, decision(id), "{\"approved\":true}").andExpect(status().isOk());
        putJson(admin, decision(id), "{\"approved\":true}").andExpect(status().is(422));
        assertEquals(1L, count("select count(*) from accounts where owner_id = ?", holder.getId()));
    }

    @Test void userSearch_filtersAndPages() throws Exception {
        User p = pendingUser(), h = newUser(Role.ACCOUNT_HOLDER), a = newUser(Role.AUDITOR);
        User disabled = newUser(Role.ACCOUNT_HOLDER);
        jdbc.update("update users set enabled = false where id = ?", disabled.getId());

        get(admin, "/api/admin/users?search=" + p.getUsername()).andExpect(jsonPath("$.totalElements").value(1));
        get(admin, "/api/admin/users?status=PENDING_APPROVAL&size=100")
                .andExpect(jsonPath("$.content[?(@.id==" + p.getId() + ")]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.id==" + h.getId() + ")]").isEmpty());
        get(admin, "/api/admin/users?role=AUDITOR&size=100")
                .andExpect(jsonPath("$.content[?(@.id==" + a.getId() + ")]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.id==" + h.getId() + ")]").isEmpty());
        get(admin, "/api/admin/users?status=DISABLED&size=100")
                .andExpect(jsonPath("$.content[?(@.id==" + disabled.getId() + ")]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.id==" + h.getId() + ")]").isEmpty());
        get(admin, "/api/admin/users?size=1").andExpect(jsonPath("$.content.length()").value(1));
        get(admin, "/api/admin/users?status=NONSENSE").andExpect(status().isBadRequest());
    }

    @Test void stats_includePendingCounts() throws Exception {
        pendingUser();
        String body = get(admin, "/api/admin/stats").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(((Number) JsonPath.read(body, "$.pendingRegistrations")).longValue() >= 1);
        assertNotNull(JsonPath.read(body, "$.pendingAccountRequests"));
        assertEquals(Boolean.TRUE, JsonPath.read(body, "$.ledgerBalanced"));
    }

    @Test void auditor_ledgerFilters() throws Exception {
        User h = newUser(Role.ACCOUNT_HOLDER);
        Account acc = newAccount(h, "100.00");
        User auditor = newUser(Role.AUDITOR);
        String n = acc.getAccountNumber();
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        String tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();

        get(auditor, "/api/auditor/ledger?account=" + n).andExpect(jsonPath("$.totalElements").value(1));
        get(auditor, "/api/auditor/ledger?account=" + n + "&type=DEPOSIT").andExpect(jsonPath("$.totalElements").value(1));
        get(auditor, "/api/auditor/ledger?account=" + n + "&type=TRANSFER").andExpect(jsonPath("$.totalElements").value(0));
        get(auditor, "/api/auditor/ledger?account=" + n + "&from=2000-01-01&to=" + today)
                .andExpect(jsonPath("$.totalElements").value(1));
        get(auditor, "/api/auditor/ledger?account=" + n + "&from=" + tomorrow).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test void auditor_transactionDetail_readOnly() throws Exception {
        User h = newUser(Role.ACCOUNT_HOLDER);
        Account acc = newAccount(h, "100.00");
        String ref = JsonPath.read(post(h, DEPOSIT, key(), dep(acc, "5.00")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.reference");
        User auditor = newUser(Role.AUDITOR);

        get(auditor, "/api/auditor/transactions/" + ref).andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.reference").value(ref))
                .andExpect(jsonPath("$.entries.length()").value(2));
        get(admin, "/api/auditor/transactions/" + ref).andExpect(status().isOk());
        get(h, "/api/auditor/transactions/" + ref).andExpect(status().isForbidden());
        get(auditor, "/api/auditor/transactions/00000000-0000-0000-0000-000000000000").andExpect(status().isNotFound());
        putJson(auditor, "/api/auditor/transactions/" + ref, "{}").andExpect(status().isForbidden());
    }
}