package com.ledgerline.backend.config;

import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, AccountRepository accounts, PasswordEncoder encoder) {
        this.users = users; this.accounts = accounts; this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;
        users.save(new User("admin", "admin@ledgerline.com", encoder.encode("Admin@123"), Role.ADMIN));
        users.save(new User("auditor", "auditor@ledgerline.com", encoder.encode("Auditor@123"), Role.AUDITOR));

        User alice = users.save(new User("alice", "alice@example.com", encoder.encode("Alice@123"), Role.ACCOUNT_HOLDER));
        User bob = users.save(new User("bob", "bob@example.com", encoder.encode("Bob@12345"), Role.ACCOUNT_HOLDER));

        accounts.save(new Account("LL1000000001", alice, AccountType.CHECKING));
        accounts.save(new Account("LL1000000002", alice, AccountType.SAVINGS));
        accounts.save(new Account("LL1000000003", bob, AccountType.CHECKING));
    }
}