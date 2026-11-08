package com.ledgerline.backend.service;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.repository.AccountRepository;
import com.ledgerline.backend.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AdminService(UserRepository users, AccountRepository accounts,
                        PasswordEncoder encoder, AuditService audit) {
        this.users = users; this.accounts = accounts; this.encoder = encoder; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return users.findAll(Sort.by("id")).stream().map(UserSummary::from).toList();
    }

    @Transactional
    public UserSummary createUser(Long adminId, CreateUserRequest r) {
        if (users.existsByUsernameIgnoreCase(r.username())) throw ApiException.conflict("Username already taken");
        if (users.existsByEmailIgnoreCase(r.email())) throw ApiException.conflict("Email already registered");
        User u = users.save(new User(r.username().trim(), r.email().trim().toLowerCase(),
                encoder.encode(r.password()), r.role()));
        audit.log(adminId, "USER_CREATED", "USER", String.valueOf(u.getId()),
                Map.of("username", u.getUsername(), "role", u.getRole().name()));
        return UserSummary.from(u);
    }

    @Transactional
    public UserSummary setUserEnabled(Long adminId, Long userId, boolean enabled) {
        if (adminId.equals(userId) && !enabled) throw ApiException.badRequest("You cannot disable your own account");
        User u = users.findById(userId).orElseThrow(() -> ApiException.resourceNotFound("User not found"));
        u.setEnabled(enabled);
        audit.log(adminId, enabled ? "USER_ENABLED" : "USER_DISABLED", "USER", String.valueOf(userId),
                Map.of("username", u.getUsername()));
        return UserSummary.from(u);
    }

    @Transactional(readOnly = true)
    public List<AdminAccountView> listAccounts() {
        return accounts.findAllWithOwner().stream().map(AdminAccountView::from).toList();
    }

    @Transactional
    public AdminAccountView createAccount(Long adminId, CreateAccountRequest r) {
        User owner = users.findById(r.ownerId()).orElseThrow(() -> ApiException.resourceNotFound("User not found"));
        if (owner.getRole() != Role.ACCOUNT_HOLDER) {
            throw ApiException.badRequest("Accounts can only be opened for account holders");
        }
        if (r.type() == AccountType.SYSTEM) throw ApiException.badRequest("Invalid account type");
        Account a = accounts.save(new Account(newAccountNumber(), owner, r.type()));
        audit.log(adminId, "ACCOUNT_CREATED", "ACCOUNT", a.getAccountNumber(),
                Map.of("owner", owner.getUsername(), "type", r.type().name()));
        return AdminAccountView.from(a);
    }

    @Transactional
    public AdminAccountView setAccountStatus(Long adminId, Long accountId, AccountStatus status) {
        // Take the same row lock that transfers use, so a status change cannot race a money movement
        List<Account> locked = accounts.lockAllByIdIn(List.of(accountId));
        if (locked.isEmpty()) throw ApiException.notFound();
        Account a = locked.get(0);

        if (a.getType() == AccountType.SYSTEM) throw ApiException.badRequest("The system account cannot be changed");
        if (a.getStatus() == AccountStatus.CLOSED && status != AccountStatus.CLOSED) {
            throw ApiException.unprocessable("Closed accounts cannot be reopened");
        }
        if (status == AccountStatus.CLOSED && a.getBalance().signum() != 0) {
            throw ApiException.unprocessable("Withdraw or transfer the remaining balance before closing");
        }
        AccountStatus old = a.getStatus();
        a.setStatus(status);
        audit.log(adminId, "ACCOUNT_STATUS_CHANGED", "ACCOUNT", a.getAccountNumber(),
                Map.of("from", old.name(), "to", status.name()));
        return AdminAccountView.from(a);
    }

    private String newAccountNumber() {
        for (int i = 0; i < 10; i++) {
            String n = "LL" + (1_000_000_000L + RANDOM.nextLong(9_000_000_000L));
            if (!accounts.existsByAccountNumber(n)) return n;
        }
        throw new IllegalStateException("Could not generate a unique account number");
    }
}