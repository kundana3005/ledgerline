package com.ledgerline.backend;

import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.service.PasswordService;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityMonitoringTest extends IntegrationTest {
    @Autowired PasswordService passwords;

    private static String ip() {
        int h = Math.abs(UUID.randomUUID().hashCode());
        return "172." + (16 + h % 15) + "." + ((h / 15) % 250) + "." + ((h / 3750) % 250 + 1);
    }
    private void badLogin(String ip, String id, String pw) throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"" + id + "\",\"password\":\"" + pw + "\"}"));
    }

    @Test void repeatedFailedLogins_areFlaggedOnce_andAuditStaysAppendOnly() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER); String ip = ip();
        for (int i = 0; i < 4; i++) badLogin(ip, u.getUsername(), "wrong");
        assertEquals(0L, count("select count(*) from audit_logs where action = 'LOGIN_BURST_DETECTED' and ip_address = ?", ip));
        badLogin(ip, u.getUsername(), "wrong");
        badLogin(ip, u.getUsername(), "wrong");
        assertEquals(1L, count("select count(*) from audit_logs where action = 'LOGIN_BURST_DETECTED' and ip_address = ?", ip));
        DataAccessException e = assertThrows(DataAccessException.class,
                () -> jdbc.update("update audit_logs set action = 'X' where action = 'LOGIN_BURST_DETECTED' and ip_address = ?", ip));
        assertTrue(e.getMessage().contains("append-only"));
    }

    @Test void auditLog_neverContainsPasswordsOrTheFullLoginName() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER); String ip = ip();
        badLogin(ip, u.getUsername(), "Wr0ng!Secret-xyz");
        assertEquals(0L, count("select count(*) from audit_logs where details like '%Wr0ng!Secret-xyz%'"));
        String details = jdbc.queryForObject("select details from audit_logs where action = 'LOGIN_FAILED' and ip_address = ?", String.class, ip);
        assertFalse(details.contains(u.getUsername()));
    }

    @Test void probingSomeoneElsesAccount_isFlaggedAsSuspicious() throws Exception {
        User alice = newUser(Role.ACCOUNT_HOLDER), bob = newUser(Role.ACCOUNT_HOLDER);
        Account a = newAccount(alice, "10.00"), b = newAccount(bob, "10.00");
        post(alice, TRANSFER, key(), xfer(b, a, "1.00"));
        assertEquals(1L, count("select count(*) from audit_logs where action = 'SUSPICIOUS_ACTIVITY' and actor_id = ?", alice.getId()));
        assertEquals(1L, count("select count(*) from audit_logs where action = 'TRANSFER_FAILED' and actor_id = ?", alice.getId()));
    }

    @Test void repeatedFailedTransactions_areFlaggedOnce() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER); Account a = newAccount(u, "10.00");
        for (int i = 0; i < 6; i++) post(u, WITHDRAW, key(), dep(a, "999.00"));
        assertEquals(1L, count("select count(*) from audit_logs where action = 'REPEATED_FAILED_TRANSACTIONS' and actor_id = ?", u.getId()));
    }

    @Test void resetTokensNeverAppearInTheAuditTrail() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        String raw = passwords.issueResetToken(u);
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + u.getEmail() + "\"}"));
        assertEquals(0L, count("select count(*) from audit_logs where details like ?", "%" + raw + "%"));
        assertTrue(count("select count(*) from audit_logs where action = 'PASSWORD_RESET_REQUESTED' and actor_id = ?", u.getId()) >= 1);
    }
}