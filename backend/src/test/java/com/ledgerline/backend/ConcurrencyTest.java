package com.ledgerline.backend;

import com.ledgerline.backend.dto.TransferRequest;
import com.ledgerline.backend.dto.WithdrawRequest;
import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConcurrencyTest extends IntegrationTest {

    /** Starts all tasks at the same instant. A task returns true on success; any unexpected exception fails the test. */
    private List<Boolean> race(int n, IntFunction<Callable<Boolean>> task) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(n);

    try {
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            Callable<Boolean> c = task.apply(i);

            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return c.call();
            }));
        }

        ready.await();
        go.countDown();

        List<Boolean> results = new ArrayList<>();

        for (Future<Boolean> f : futures) {
            results.add(f.get(60, TimeUnit.SECONDS));
        }

        return results;

    } finally {
        pool.shutdownNow();
    }
}

    @Test void tenSimultaneousWithdrawals_cannotOverdraw() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        Account acc = newAccount(u, "100.00");

        List<Boolean> results = race(10, i -> () -> {
            try {
                ledger.withdraw(u.getId(), key(), new WithdrawRequest(acc.getAccountNumber(), new BigDecimal("30.00")));
                return true;
            } catch (ApiException e) {
                if ("INSUFFICIENT_FUNDS".equals(e.getCode())) return false;
                throw e;
            }
        });

        assertEquals(3, results.stream().filter(b -> b).count(), "exactly 3 x 30 fits into 100");
        assertBalance("10.00", acc);
        assertEquals(3L, count("select count(*) from ledger_entries where account_id = ? and direction = 'DEBIT'", acc.getId()));
    }

    @Test void oppositeTransfersAtOnce_noDeadlock_andTotalsPreserved() throws Exception {
        User ua = newUser(Role.ACCOUNT_HOLDER), ub = newUser(Role.ACCOUNT_HOLDER);
        Account a = newAccount(ua, "1000.00"), b = newAccount(ub, "1000.00");

        List<Boolean> results = race(20, i -> () -> {
            if (i < 12) ledger.transfer(ua.getId(), key(), new TransferRequest(a.getAccountNumber(), b.getAccountNumber(), new BigDecimal("10.00")));
            else        ledger.transfer(ub.getId(), key(), new TransferRequest(b.getAccountNumber(), a.getAccountNumber(), new BigDecimal("10.00")));
            return true;
        });

        assertEquals(20, results.size());
        assertBalance("960.00", a);    // 1000 - 12*10 + 8*10
        assertBalance("1040.00", b);   // 1000 + 12*10 - 8*10
        assertEquals(0, balance(a).add(balance(b)).compareTo(new BigDecimal("2000")), "money was neither created nor lost");
    }

    @Test void sameRequestSentEightTimesAtOnce_paysOnce() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        Account acc = newAccount(u, null);
        String k = key();

        List<Boolean> results = race(8, i -> () ->
                post(u, DEPOSIT, k, dep(acc, "50.00")).andReturn().getResponse().getStatus() == 200);

        assertEquals(8, results.stream().filter(b -> b).count(), "every duplicate gets the original result");
        assertBalance("50.00", acc);
        assertEquals(1L, count("select count(*) from transactions where idempotency_key = ?", k));
    }

    @Test void simultaneousOverdrawAttemptsThroughTheApi_leaveBalanceNonNegative() throws Exception {
        User u = newUser(Role.ACCOUNT_HOLDER);
        Account acc = newAccount(u, "100.00");

        List<Boolean> results = race(10, i -> () ->
                post(u, WITHDRAW, key(), dep(acc, "60.00")).andReturn().getResponse().getStatus() == 200);

        assertEquals(1, results.stream().filter(b -> b).count());
        assertBalance("40.00", acc);
    }
}