package com.ledgerline.backend.support;

import com.ledgerline.backend.dto.DepositRequest;
import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.repository.*;
import com.ledgerline.backend.service.LedgerService;
import com.ledgerline.backend.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    public static final String DEPOSIT = "/api/transactions/deposit";
    public static final String WITHDRAW = "/api/transactions/withdraw";
    public static final String TRANSFER = "/api/transactions/transfer";

    @Autowired protected MockMvc mvc;
    @Autowired protected UserRepository users;
    @Autowired protected AccountRepository accounts;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected TokenService tokens;
    @Autowired protected LedgerService ledger;
    @Autowired protected JdbcTemplate jdbc;

    /** Every test creates its own users and accounts (ledger rows cannot be deleted, by design). */
    protected User newUser(Role role) {
        String name = role.name().substring(0, 3).toLowerCase() + UUID.randomUUID().toString().substring(0, 8);
        return users.save(new User(name, name + "@test.local", encoder.encode("Test@12345"), role));
    }

    /** funds == null gives an empty account; otherwise funds arrive through a real deposit (so the ledger stays valid). */
    protected Account newAccount(User owner, String funds) {
        String number = "TST" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Account a = accounts.save(new Account(number, owner, AccountType.CHECKING));
        if (funds != null) {
            ledger.deposit(owner.getId(), key(), new DepositRequest(number, new BigDecimal(funds)));
        }
        return a;
    }

    protected String bearer(User u) { return "Bearer " + tokens.issue(u); }
    protected static String key() { return UUID.randomUUID().toString(); }

    protected BigDecimal balance(Account a) {
        return accounts.findByAccountNumber(a.getAccountNumber()).orElseThrow().getBalance();
    }

    protected void assertBalance(String expected, Account a) {
        BigDecimal actual = balance(a);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected balance " + expected + " but was " + actual);
    }

    protected long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    protected ResultActions post(User u, String path, String idemKey, String json) throws Exception {
        MockHttpServletRequestBuilder b = MockMvcRequestBuilders.post(path)
                .header("Authorization", bearer(u))
                .contentType(MediaType.APPLICATION_JSON).content(json);
        if (idemKey != null) b.header("Idempotency-Key", idemKey);
        return mvc.perform(b);
    }

    protected ResultActions get(User u, String path) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(path).header("Authorization", bearer(u)));
    }

    protected static String dep(Account a, String amount) {
        return "{\"accountNumber\":\"" + a.getAccountNumber() + "\",\"amount\":" + amount + "}";
    }

    protected static String xfer(Account from, Account to, String amount) {
        return "{\"sourceAccount\":\"" + from.getAccountNumber() + "\",\"destinationAccount\":\""
                + to.getAccountNumber() + "\",\"amount\":" + amount + "}";
    }
}