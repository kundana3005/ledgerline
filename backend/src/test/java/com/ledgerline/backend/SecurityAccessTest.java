package com.ledgerline.backend;

import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityAccessTest extends IntegrationTest {
    private User holder, auditor, admin;

    @BeforeEach
    void setUp() {
        holder = newUser(Role.ACCOUNT_HOLDER);
        auditor = newUser(Role.AUDITOR);
        admin = newUser(Role.ADMIN);
    }

    private String loginJson(String identifier, String password) {
        return "{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}";
    }

    @Test void noToken_is401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(MockMvcRequestBuilders.post(DEPOSIT).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void garbageToken_is401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/accounts").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test void tamperedToken_is401() throws Exception {
        String t = tokens.issue(holder);
        mvc.perform(MockMvcRequestBuilders.get("/api/accounts")
                .header("Authorization", "Bearer " + t.substring(0, t.length() - 3) + "abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test void holderCannotUseAdminApi() throws Exception {
        get(holder, "/api/admin/users").andExpect(status().isForbidden());
        get(holder, "/api/admin/stats").andExpect(status().isForbidden());
        post(holder, "/api/admin/users", null,
                "{\"username\":\"evil\",\"email\":\"e@e.com\",\"password\":\"Passw0rd!!\",\"role\":\"ADMIN\"}")
                .andExpect(status().isForbidden());
    }

    @Test void holderCannotUseAuditorApi() throws Exception {
        get(holder, "/api/auditor/ledger").andExpect(status().isForbidden());
        get(holder, "/api/auditor/audit-logs").andExpect(status().isForbidden());
    }

    @Test void auditorCanRead() throws Exception {
        get(auditor, "/api/auditor/ledger").andExpect(status().isOk());
        get(auditor, "/api/auditor/audit-logs").andExpect(status().isOk());
        get(auditor, "/api/auditor/summary").andExpect(status().isOk());
    }

    @Test void auditorIsReadOnly_everywhere() throws Exception {
        post(auditor, DEPOSIT, key(), "{}").andExpect(status().isForbidden());
        post(auditor, WITHDRAW, key(), "{}").andExpect(status().isForbidden());
        post(auditor, TRANSFER, key(), "{}").andExpect(status().isForbidden());
        post(auditor, "/api/admin/users", null, "{}").andExpect(status().isForbidden());
        post(auditor, "/api/auditor/ledger", null, "{}").andExpect(status().isForbidden());
        mvc.perform(put("/api/auditor/ledger").header("Authorization", bearer(auditor))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/auditor/ledger").header("Authorization", bearer(auditor)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/auditor/audit-logs").header("Authorization", bearer(auditor)))
                .andExpect(status().isForbidden());
        get(auditor, "/api/accounts").andExpect(status().isForbidden());
    }

    @Test void adminCannotMoveMoney() throws Exception {
        post(admin, DEPOSIT, key(), "{}").andExpect(status().isForbidden());
        post(admin, TRANSFER, key(), "{}").andExpect(status().isForbidden());
    }

    @Test void ledgerAndAuditHaveNoWriteEndpoints() throws Exception {
        mvc.perform(delete("/api/auditor/ledger").header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/auditor/audit-logs").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test void loginWithWrongPassword_is401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(holder.getUsername(), "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test void loginWithUnknownUser_looksTheSame() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("nobody-here", "x")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test void loginWithCorrectPassword_works() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(holder.getUsername(), "Test@12345")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test void disabledUserCannotLogIn() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        u.setEnabled(false);
        users.save(u);
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(u.getUsername(), "Test@12345")))
                .andExpect(status().isUnauthorized());
    }
}