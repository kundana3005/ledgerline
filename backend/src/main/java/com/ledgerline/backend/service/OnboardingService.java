package com.ledgerline.backend.service;

import com.ledgerline.backend.dto.AccountRequestView;
import com.ledgerline.backend.dto.OnboardingStatus;
import com.ledgerline.backend.entity.*;
import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.repository.AccountRequestRepository;
import com.ledgerline.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class OnboardingService {
    private final UserRepository users;
    private final AccountRequestRepository requests;
    private final AuditService audit;

    public OnboardingService(UserRepository users, AccountRequestRepository requests, AuditService audit) {
        this.users = users; this.requests = requests; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public OnboardingStatus status(Long userId) {
        User u = load(userId);
        return new OnboardingStatus(u.getFullName(), u.getEmail(), u.getStatus().name(), u.getRejectionReason(),
                requests.findByUser_IdOrderByIdDesc(userId).stream().map(AccountRequestView::from).toList());
    }

    @Transactional
    public AccountRequestView submit(Long userId, AccountType type) {
        User u = load(userId);
        if (u.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.unprocessable("Your registration must be approved before you can request an account");
        }
        if (type == AccountType.SYSTEM) throw ApiException.badRequest("Invalid account type");
        boolean hasPending = requests.findByUser_IdOrderByIdDesc(userId).stream()
                .anyMatch(r -> r.getStatus() == AccountRequestStatus.PENDING);
        if (hasPending) throw ApiException.conflict("You already have a pending account request");

        // The partial unique index also blocks two simultaneous submissions (409 via the handler)
        AccountRequest saved = requests.saveAndFlush(new AccountRequest(u, type));
        audit.log(userId, "ACCOUNT_REQUEST_SUBMITTED", "ACCOUNT_REQUEST", String.valueOf(saved.getId()),
                Map.of("type", type.name()));
        return AccountRequestView.from(saved);
    }

    private User load(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.resourceNotFound("User not found"));
    }
}