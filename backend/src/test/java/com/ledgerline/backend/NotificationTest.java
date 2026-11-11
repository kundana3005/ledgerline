package com.ledgerline.backend;

import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationTest extends IntegrationTest {

    private String list(User u) throws Exception {
        return get(u, "/api/notifications").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
    private static String last4(Account a) { return a.getAccountNumber().substring(a.getAccountNumber().length() - 4); }
    private ResultActions putJson(User u, String path, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put(path).header("Authorization", bearer(u)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test void deposit_notifiesWithMaskedAccount() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER); Account a = newAccount(u, null);
        post(u, DEPOSIT, key(), dep(a, "25.00")).andExpect(status().isOk());
        String body = list(u);
        assertTrue(body.contains("TRANSACTION_COMPLETED")); assertTrue(body.contains("25.00"));
        assertTrue(body.contains(last4(a))); assertFalse(body.contains(a.getAccountNumber()));
    }

    @Test void transfer_notifiesRecipient_withoutRevealingTheSender() throws Exception {
        User alice = newUser(Role.ACCOUNT_HOLDER), bob = newUser(Role.ACCOUNT_HOLDER);
        Account a = newAccount(alice, "100.00"), b = newAccount(bob, null);
        post(alice, TRANSFER, key(), xfer(a, b, "40.50")).andExpect(status().isOk());
        String bobs = list(bob);
        assertTrue(bobs.contains("MONEY_RECEIVED")); assertTrue(bobs.contains("40.50"));
        assertFalse(bobs.contains(a.getAccountNumber())); assertFalse(bobs.contains(last4(a) + "\"") && bobs.contains("from account"));
        assertFalse(list(alice).contains(b.getAccountNumber()));
    }

    @Test void failedTransaction_createsAFailureNotice() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER); Account a = newAccount(u, "10.00");
        post(u, WITHDRAW, key(), dep(a, "999.00")).andExpect(status().is(422));
        assertTrue(list(u).contains("TRANSACTION_FAILED"));
        assertBalance("10.00", a);
    }

    @Test void unreadCount_markRead_andOwnershipOfNotifications() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER), other = newUser(Role.ACCOUNT_HOLDER); Account a = newAccount(u, null);
        post(u, DEPOSIT, key(), dep(a, "5.00"));
        get(u, "/api/notifications/unread-count").andExpect(jsonPath("$.count").value(1));
        Long id = jdbc.queryForObject("select id from notifications where user_id = ? order by id desc limit 1", Long.class, u.getId());
        putJson(other, "/api/notifications/" + id + "/read", "{}").andExpect(status().isNotFound());
        get(u, "/api/notifications/unread-count").andExpect(jsonPath("$.count").value(1));
        putJson(u, "/api/notifications/" + id + "/read", "{}").andExpect(status().isOk());
        get(u, "/api/notifications/unread-count").andExpect(jsonPath("$.count").value(0));
        get(other, "/api/notifications").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test void adminApproval_notifiesTheApplicant() throws Exception {
        User admin = newUser(Role.ADMIN), applicant = newUser(Role.ACCOUNT_HOLDER);
        jdbc.update("update users set status = 'PENDING_APPROVAL' where id = ?", applicant.getId());
        putJson(admin, "/api/admin/users/" + applicant.getId() + "/approval", "{\"approved\":true}").andExpect(status().isOk());
        assertTrue(list(applicant).contains("REGISTRATION_APPROVED"));
    }

    @Test void staffCannotUseHolderNotifications() throws Exception {
        get(newUser(Role.AUDITOR), "/api/notifications").andExpect(status().isForbidden());
        get(newUser(Role.ADMIN), "/api/notifications").andExpect(status().isForbidden());
    }
}