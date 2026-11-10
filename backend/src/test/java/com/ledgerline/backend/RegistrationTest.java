package com.ledgerline.backend;

import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegistrationTest extends IntegrationTest {
    private static final String REGISTER = "/api/auth/register";
    private static final String REQUESTS = "/api/onboarding/account-requests";
    private static final String STATUS = "/api/onboarding/status";

    private static String email() { return "reg" + UUID.randomUUID().toString().substring(0, 8) + "@test.local"; }

    private static String json(String email, String password, String confirm, String phone, String name) {
        return "{\"fullName\":\"" + name + "\",\"email\":\"" + email + "\",\"phone\":\"" + phone
                + "\",\"password\":\"" + password + "\",\"confirmPassword\":\"" + confirm + "\"}";
    }

    private static String valid(String email) { return json(email, "Str0ng!Pass", "Str0ng!Pass", "+919876543210", "Test User"); }

    private ResultActions register(String body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(REGISTER).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long usersWithEmail(String email) {
        return count("select count(*) from users where lower(email) = lower(?)", email);
    }

    private User pendingUser() {
        User u = newUser(Role.ACCOUNT_HOLDER);
        jdbc.update("update users set status = 'PENDING_APPROVAL' where id = ?", u.getId());
        return u;
    }

    @Test void registration_createsPendingAccountHolder_withHashedPassword() throws Exception {
        String e = email();
        register(valid(e)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        Map<String, Object> row = jdbc.queryForMap("select id, role, status, password_hash, full_name from users where email = ?", e);
        assertEquals("ACCOUNT_HOLDER", row.get("role"));
        assertEquals("PENDING_APPROVAL", row.get("status"));
        assertTrue(((String) row.get("password_hash")).startsWith("$2"), "BCrypt hash expected");
        assertFalse(((String) row.get("password_hash")).contains("Str0ng"), "password must not be stored in plain text");
        assertEquals(1L, count("select count(*) from audit_logs where action = 'USER_REGISTERED' and entity_id = ?",
                String.valueOf(row.get("id"))));
    }

    @Test void duplicateEmail_is409_caseInsensitive() throws Exception {
        String e = email();
        register(valid(e)).andExpect(status().isCreated());
        register(valid(e)).andExpect(status().isConflict());
        register(valid(e.toUpperCase())).andExpect(status().isConflict());
        assertEquals(1L, usersWithEmail(e));
        assertTrue(count("select count(*) from audit_logs where action = 'REGISTRATION_REJECTED'") >= 1);
    }

    @Test void roleInjection_neverCreatesAPrivilegedUser() throws Exception {
        String e = email();
        String body = valid(e).replace("{", "{\"role\":\"ADMIN\",\"status\":\"ACTIVE\",");
        register(body);   // either ignored (201) or rejected (400); both are safe
        long total = usersWithEmail(e);
        if (total == 1) {
            Map<String, Object> row = jdbc.queryForMap("select role, status from users where email = ?", e);
            assertEquals("ACCOUNT_HOLDER", row.get("role"));
            assertEquals("PENDING_APPROVAL", row.get("status"));
        }
    }

    @Test void invalidInput_is400_andCreatesNoUser() throws Exception {
        String e = email();
        register(json(e, "password", "password", "+919876543210", "Test User")).andExpect(status().isBadRequest());
        register(json(e, "Str0ng!Pass", "Different1!", "+919876543210", "Test User")).andExpect(status().isBadRequest());
        register(json(e, "Str0ng!Pass", "Str0ng!Pass", "12ab", "Test User")).andExpect(status().isBadRequest());
        register(json(e, "Str0ng!Pass", "Str0ng!Pass", "+919876543210", "")).andExpect(status().isBadRequest());
        register(json("not-an-email", "Str0ng!Pass", "Str0ng!Pass", "+919876543210", "Test User")).andExpect(status().isBadRequest());
        assertEquals(0L, usersWithEmail(e));
    }

    @Test void loginResponse_includesStatus() throws Exception {
        String e = email();
        register(valid(e)).andExpect(status().isCreated());
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + e + "\",\"password\":\"Str0ng!Pass\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test void pendingUser_seesStatus_butCannotUseMoneyEndpoints() throws Exception {
        User u = pendingUser();
        get(u, STATUS).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
        get(u, "/api/accounts").andExpect(status().isForbidden());
        get(u, "/api/transactions").andExpect(status().isForbidden());
        post(u, DEPOSIT, key(), "{\"accountNumber\":\"LL0000000000\",\"amount\":10}").andExpect(status().isForbidden());
        post(u, REQUESTS, null, "{\"type\":\"CHECKING\"}").andExpect(status().isForbidden());
    }

    @Test void activeHolder_canRequestAnAccount_onlyOnePendingAtATime() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        post(u, REQUESTS, null, "{\"type\":\"SAVINGS\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"));
        post(u, REQUESTS, null, "{\"type\":\"CHECKING\"}").andExpect(status().isConflict());
        assertEquals(1L, count("select count(*) from account_requests where user_id = ?", u.getId()));
        assertEquals(1L, count("select count(*) from audit_logs where action = 'ACCOUNT_REQUEST_SUBMITTED' and actor_id = ?", u.getId()));
    }

    @Test void invalidAccountTypes_are400() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        post(u, REQUESTS, null, "{\"type\":\"SYSTEM\"}").andExpect(status().isBadRequest());
        post(u, REQUESTS, null, "{\"type\":\"GOLD\"}").andExpect(status().isBadRequest());
        post(u, REQUESTS, null, "{}").andExpect(status().isBadRequest());
        assertEquals(0L, count("select count(*) from account_requests where user_id = ?", u.getId()));
    }

    @Test void auditorAndAdmin_cannotSubmitAccountRequests() throws Exception {
        post(newUser(Role.AUDITOR), REQUESTS, null, "{\"type\":\"CHECKING\"}").andExpect(status().isForbidden());
        post(newUser(Role.ADMIN), REQUESTS, null, "{\"type\":\"CHECKING\"}").andExpect(status().isForbidden());
    }

    @Test void status_needsLogin_andShowsOnlyOwnRequests() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(STATUS)).andExpect(status().isUnauthorized());
        User a = newUser(Role.ACCOUNT_HOLDER), b = newUser(Role.ACCOUNT_HOLDER);
        post(a, REQUESTS, null, "{\"type\":\"CHECKING\"}").andExpect(status().isCreated());
        get(a, STATUS).andExpect(jsonPath("$.requests.length()").value(1));
        get(b, STATUS).andExpect(jsonPath("$.requests.length()").value(0));
    }
}