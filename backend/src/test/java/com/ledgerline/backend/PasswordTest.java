package com.ledgerline.backend;

import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.service.PasswordService;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PasswordTest extends IntegrationTest {
    private static final String GOOD = "N3w!Passw0rd";
    @Autowired PasswordService passwords;

    private ResultActions anon(String path, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }
    private int loginStatus(User u, String pw) throws Exception {
        return anon("/api/auth/login", "{\"identifier\":\"" + u.getUsername() + "\",\"password\":\"" + pw + "\"}")
                .andReturn().getResponse().getStatus();
    }
    private String change(String cur, String nw, String conf) { return "{\"currentPassword\":\"" + cur + "\",\"newPassword\":\"" + nw + "\",\"confirmPassword\":\"" + conf + "\"}"; }
    private String reset(String token, String pw, String conf) { return "{\"token\":\"" + token + "\",\"password\":\"" + pw + "\",\"confirmPassword\":\"" + conf + "\"}"; }

    @Test void changePassword_works_andIsAudited() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        post(u, "/api/auth/change-password", null, change("Test@12345", GOOD, GOOD)).andExpect(status().isOk());
        assertEquals(401, loginStatus(u, "Test@12345"));
        assertEquals(200, loginStatus(u, GOOD));
        String hash = jdbc.queryForObject("select password_hash from users where id = ?", String.class, u.getId());
        assertTrue(hash.startsWith("$2")); assertFalse(hash.contains("N3w"));
        assertEquals(1L, count("select count(*) from audit_logs where action = 'PASSWORD_CHANGED' and actor_id = ?", u.getId()));
    }

    @Test void changePassword_rejectsWrongCurrentWeakMismatchedAndSame() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        post(u, "/api/auth/change-password", null, change("wrong", GOOD, GOOD)).andExpect(status().isBadRequest());
        post(u, "/api/auth/change-password", null, change("Test@12345", "weak", "weak")).andExpect(status().isBadRequest());
        post(u, "/api/auth/change-password", null, change("Test@12345", GOOD, "Other!Passw0rd")).andExpect(status().isBadRequest());
        post(u, "/api/auth/change-password", null, change("Test@12345", "Test@12345", "Test@12345")).andExpect(status().isBadRequest());
        assertEquals(200, loginStatus(u, "Test@12345"));
        assertEquals(1L, count("select count(*) from audit_logs where action = 'PASSWORD_CHANGE_FAILED' and actor_id = ?", u.getId()));
    }

    @Test void changePassword_needsLogin() throws Exception {
        anon("/api/auth/change-password", change("a", GOOD, GOOD)).andExpect(status().isUnauthorized());
    }

    @Test void forgotPassword_looksTheSameForKnownAndUnknownEmails() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        String known = anon("/api/auth/forgot-password", "{\"email\":\"" + u.getEmail() + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String unknown = anon("/api/auth/forgot-password", "{\"email\":\"nobody@nowhere.test\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(known, unknown);
        assertEquals(1L, count("select count(*) from password_reset_tokens where user_id = ?", u.getId()));
    }

    @Test void resetToken_isSingleUse() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        String raw = passwords.issueResetToken(u);
        anon("/api/auth/reset-password", reset(raw, GOOD, GOOD)).andExpect(status().isOk());
        assertEquals(200, loginStatus(u, GOOD));
        anon("/api/auth/reset-password", reset(raw, "An0ther!Pass", "An0ther!Pass")).andExpect(status().isBadRequest());
        assertEquals(200, loginStatus(u, GOOD));
    }

    @Test void expiredToken_isRejected() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        String raw = passwords.issueResetToken(u);
        jdbc.update("update password_reset_tokens set expires_at = now() - interval '1 minute' where user_id = ?", u.getId());
        anon("/api/auth/reset-password", reset(raw, GOOD, GOOD)).andExpect(status().isBadRequest());
        assertEquals(200, loginStatus(u, "Test@12345"));
    }

    @Test void unknownToken_isRejected() throws Exception {
        anon("/api/auth/reset-password", reset("not-a-real-token", GOOD, GOOD)).andExpect(status().isBadRequest());
    }

    @Test void weakPassword_doesNotBurnTheToken() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        String raw = passwords.issueResetToken(u);
        anon("/api/auth/reset-password", reset(raw, "weak", "weak")).andExpect(status().isBadRequest());
        anon("/api/auth/reset-password", reset(raw, GOOD, GOOD)).andExpect(status().isOk());
    }

    @Test void newRequestInvalidatesTheOlderLink() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        String first = passwords.issueResetToken(u);
        String second = passwords.issueResetToken(u);
        anon("/api/auth/reset-password", reset(first, GOOD, GOOD)).andExpect(status().isBadRequest());
        anon("/api/auth/reset-password", reset(second, GOOD, GOOD)).andExpect(status().isOk());
    }

    @Test void tokenIsStoredOnlyAsAHash() {
        User u = newUser(Role.ACCOUNT_HOLDER);
        String raw = passwords.issueResetToken(u);
        String stored = jdbc.queryForObject("select token_hash from password_reset_tokens where user_id = ?", String.class, u.getId());
        assertNotEquals(raw, stored);
        assertEquals(64, stored.length());
        assertEquals(0L, count("select count(*) from password_reset_tokens where token_hash = ?", raw));
    }
}