package com.ledgerline.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
    @Column(nullable = false, length = 40, updatable = false)
    private String type;
    @Column(nullable = false, length = 500, updatable = false)
    private String message;
    @Column(name = "is_read", nullable = false)
    private boolean seen;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {}
    public Notification(Long userId, String type, String message) {
        this.userId = userId; this.type = type;
        this.message = message.length() > 500 ? message.substring(0, 500) : message;
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public boolean isSeen() { return seen; }
    public Instant getCreatedAt() { return createdAt; }
}