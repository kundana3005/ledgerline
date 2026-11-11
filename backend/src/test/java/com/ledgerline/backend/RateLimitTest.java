package com.ledgerline.backend;

import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(properties = {
        "app.rate-limit.enabled=true", "app.rate-limit.store=memory",
        "app.rate-limit.login-per-minute=3", "app.rate-limit.register-per-hour=2",
        "app.rate-limit.transactions-per-minute=3"})
class RateLimitTest extends IntegrationTest {

    private static String ip() {
        int h = Math.abs(UUID.randomUUID().hashCode());
        return "10." + (h % 250) + "." + ((h / 250) % 250) + "." + ((h / 62500) % 250 + 1);
    }

    private ResultActions loginFrom(String ip, String id) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .with(r -> { r.setRemoteAddr(ip); return r; }).contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + id + "\",\"password\":\"wrong\"}"));
    }

    private ResultActions registerFrom(String ip) throws Exception {
        String email = "rl" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        return mvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                .with(r -> { r.setRemoteAddr(ip); return r; }).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Rate Test\",\"email\":\"" + email + "\",\"phone\":\"+919876543210\","
                        + "\"password\":\"Str0ng!Pass\",\"confirmPassword\":\"Str0ng!Pass\"}"));
    }

    @Test void login_isLimitedPerIp_andOthersAreNotAffected() throws Exception {
        String ip = ip();
        for (int i = 0; i < 3; i++) loginFrom(ip, "nobody").andExpect(status().isUnauthorized());
        loginFrom(ip, "nobody").andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After")).andExpect(jsonPath("$.error").value("RATE_LIMITED"));
        loginFrom(ip(), "nobody").andExpect(status().isUnauthorized());   // another client is unaffected
    }

    @Test void rateLimitExceeded_isAuditedOncePerWindow() throws Exception {
        String ip = ip();
        for (int i = 0; i < 7; i++) loginFrom(ip, "nobody");
        assertEquals(1L, count("select count(*) from audit_logs where action = 'RATE_LIMIT_EXCEEDED' and ip_address = ?", ip));
    }

    @Test void registration_isLimited() throws Exception {
        String ip = ip();
        registerFrom(ip).andExpect(status().isCreated());
        registerFrom(ip).andExpect(status().isCreated());
        registerFrom(ip).andExpect(status().isTooManyRequests());
    }

    @Test void transactions_areLimitedPerUser_andRejectedOnesNeverReachTheLedger() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER), other = newUser(Role.ACCOUNT_HOLDER);
        Account a = newAccount(u, null), b = newAccount(other, null);
        for (int i = 0; i < 3; i++) post(u, DEPOSIT, key(), dep(a, "10.00")).andExpect(status().isOk());
        post(u, DEPOSIT, key(), dep(a, "10.00")).andExpect(status().isTooManyRequests());
        assertBalance("30.00", a);
        assertEquals(3L, count("select count(*) from transactions where initiated_by = ?", u.getId()));
        post(other, DEPOSIT, key(), dep(b, "10.00")).andExpect(status().isOk());   // a different user is not affected
    }
}