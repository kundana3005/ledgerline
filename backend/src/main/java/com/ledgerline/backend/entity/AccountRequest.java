package com.ledgerline.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "account_requests")
public class AccountRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20, updatable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountRequestStatus status = AccountRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected AccountRequest() {}

    public AccountRequest(User user, AccountType accountType) {
        this.user = user;
        this.accountType = accountType;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public AccountType getAccountType() { return accountType; }
    public AccountRequestStatus getStatus() { return status; }
    public User getReviewedBy() { return reviewedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void decide(AccountRequestStatus newStatus, User reviewer, String reason) {
        this.status = newStatus;
        this.reviewedBy = reviewer;
        this.rejectionReason = reason;
        this.reviewedAt = Instant.now();
    }
}