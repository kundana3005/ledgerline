package com.ledgerline.backend.service;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.repository.AccountRepository;
import com.ledgerline.backend.repository.AccountRequestRepository;
import com.ledgerline.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final AccountRequestRepository requests;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AdminService(UserRepository users, AccountRepository accounts, AccountRequestRepository requests,
                        PasswordEncoder encoder, AuditService audit) {
        this.users = users; this.accounts = accounts; this.requests = requests;
        this.encoder = encoder; this.audit = audit;
    }

    // ---------- users ----------

    @Transactional(readOnly = true)
    public PageResponse<UserSummary> searchUsers(String search, Role role, String status, int page, int size) {
        List<Role> roles = role == null ? List.of(Role.values()) : List.of(role);
        List<UserStatus> statuses = List.of(UserStatus.values());
        List<Boolean> enabled = List.of(true, false);
        if (status != null && !status.isBlank()) {
            if ("DISABLED".equalsIgnoreCase(status)) {
                enabled = List.of(false);
            } else {
                try { statuses = List.of(UserStatus.valueOf(status.trim().toUpperCase())); }
                catch (IllegalArgumentException e) { throw ApiException.badRequest("Unknown status filter"); }
            }
        }
        String q = "%" + (search == null ? "" : search.trim().toLowerCase()) + "%";
        Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(users.search(q, roles, statuses, enabled, p).map(UserSummary::from));
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

    // ---------- approval workflow ----------

    @Transactional
    public UserSummary reviewRegistration(Long adminId, Long userId, boolean approved, String reason) {
        User u = users.lockById(userId).orElseThrow(() -> ApiException.resourceNotFound("User not found"));
        if (u.getRole() != Role.ACCOUNT_HOLDER || u.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw ApiException.unprocessable("This registration is not awaiting review");
        }
        String why = cleanReason(approved, reason);
        u.setStatus(approved ? UserStatus.ACTIVE : UserStatus.REJECTED);
        u.setRejectionReason(why);
        Map<String, String> details = approved ? Map.of("email", u.getEmail())
                : Map.of("email", u.getEmail(), "reason", why);
        audit.log(adminId, approved ? "USER_APPROVED" : "USER_REJECTED", "USER", String.valueOf(userId), details);
        return UserSummary.from(u);
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountRequestAdminView> listAccountRequests(AccountRequestStatus status, int page, int size) {
        Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(requests.findByStatus(status, p).map(AccountRequestAdminView::from));
    }

    @Transactional
    public AccountRequestAdminView reviewAccountRequest(Long adminId, Long requestId, boolean approved, String reason) {
        AccountRequest r = requests.lockById(requestId)
                .orElseThrow(() -> ApiException.resourceNotFound("Request not found"));
        if (r.getStatus() != AccountRequestStatus.PENDING) {
            throw ApiException.unprocessable("This request has already been reviewed");
        }
        User owner = r.getUser();
        if (owner.getStatus() != UserStatus.ACTIVE || !owner.isEnabled()) {
            throw ApiException.unprocessable("The applicant is not an active user");
        }
        String why = cleanReason(approved, reason);
        User admin = users.getReferenceById(adminId);
        if (approved) {
            Account a = accounts.save(new Account(newAccountNumber(), owner, r.getAccountType()));
            r.decide(AccountRequestStatus.APPROVED, admin, null);
            audit.log(adminId, "ACCOUNT_REQUEST_APPROVED", "ACCOUNT_REQUEST", String.valueOf(requestId),
                    Map.of("account", a.getAccountNumber(), "owner", owner.getEmail(), "type", r.getAccountType().name()));
        } else {
            r.decide(AccountRequestStatus.REJECTED, admin, why);
            audit.log(adminId, "ACCOUNT_REQUEST_REJECTED", "ACCOUNT_REQUEST", String.valueOf(requestId),
                    Map.of("owner", owner.getEmail(), "reason", why));
        }
        return AccountRequestAdminView.from(r);
    }

    private String cleanReason(boolean approved, String reason) {
        if (approved) return null;
        if (reason == null || reason.isBlank()) throw ApiException.badRequest("A rejection reason is required");
        String t = reason.trim();
        if (t.length() > 255) throw ApiException.badRequest("Reason is too long");
        return t;
    }

    // ---------- accounts ----------

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
        if (owner.getStatus() != UserStatus.ACTIVE) throw ApiException.unprocessable("The user is not approved yet");
        if (r.type() == AccountType.SYSTEM) throw ApiException.badRequest("Invalid account type");
        Account a = accounts.save(new Account(newAccountNumber(), owner, r.type()));
        audit.log(adminId, "ACCOUNT_CREATED", "ACCOUNT", a.getAccountNumber(),
                Map.of("owner", owner.getUsername(), "type", r.type().name()));
        return AdminAccountView.from(a);
    }

    @Transactional
    public AdminAccountView setAccountStatus(Long adminId, Long accountId, AccountStatus status) {
        // Same row lock that transfers use, so a status change cannot race a money movement
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