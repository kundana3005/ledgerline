package com.ledgerline.backend.service;

import com.ledgerline.backend.dto.ChangePasswordRequest;
import com.ledgerline.backend.dto.ResetPasswordRequest;
import com.ledgerline.backend.entity.PasswordResetToken;
import com.ledgerline.backend.entity.User;
import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.repository.PasswordResetTokenRepository;
import com.ledgerline.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordService {
    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final long expiryMinutes;
    private final String frontendUrl;
    private final boolean logLink;

    public PasswordService(UserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder encoder,
                           AuditService audit,
                           @Value("${app.password-reset.expiry-minutes:30}") long expiryMinutes,
                           @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
                           @Value("${app.password-reset.log-link:false}") boolean logLink) {
        this.users = users; this.tokens = tokens; this.encoder = encoder; this.audit = audit;
        this.expiryMinutes = expiryMinutes; this.frontendUrl = frontendUrl; this.logLink = logLink;
    }

    @Transactional
    public void change(Long userId, ChangePasswordRequest r) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.resourceNotFound("User not found"));
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash())) {
            audit.logIndependent(userId, "PASSWORD_CHANGE_FAILED", "USER", String.valueOf(userId), null);
            throw ApiException.badRequest("Current password is incorrect");   // 400, not 401: a 401 would log the user out
        }
        PasswordPolicy.check(r.newPassword(), r.confirmPassword());
        if (encoder.matches(r.newPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("The new password must be different from the current one");
        }
        u.setPasswordHash(encoder.encode(r.newPassword()));
        audit.log(userId, "PASSWORD_CHANGED", "USER", String.valueOf(userId), null);
    }

    /** Always behaves the same from the caller's point of view, whether or not the email exists. */
    @Transactional
    public void forgot(String email) {
        String e = email.trim();
        users.findByUsernameIgnoreCaseOrEmailIgnoreCase(e, e).filter(User::isEnabled).ifPresentOrElse(u -> {
            String raw = issueResetToken(u);
            audit.log(u.getId(), "PASSWORD_RESET_REQUESTED", "USER", String.valueOf(u.getId()), null);
            if (logLink) {   // development only: there is no email service configured
                log.info("DEV ONLY reset link for {}: {}/reset-password?token={}", u.getEmail(), frontendUrl, raw);
            }
        }, () -> audit.logIndependent(null, "PASSWORD_RESET_REQUESTED_UNKNOWN", "USER", null, null));
    }

    /** Creates a single-use token and returns the raw value. Only its SHA-256 hash is stored. */
    @Transactional
    public String issueResetToken(User u) {
        Instant now = Instant.now();
        tokens.invalidateOpen(u.getId(), now);   // a newer request replaces older links
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new PasswordResetToken(users.getReferenceById(u.getId()), sha256(raw),
                now.plus(expiryMinutes, ChronoUnit.MINUTES)));
        return raw;
    }

    @Transactional
    public void reset(ResetPasswordRequest r) {
        PasswordPolicy.check(r.password(), r.confirmPassword());   // before claiming, so a weak password does not burn the link
        String hash = sha256(r.token().trim());
        if (tokens.claim(hash, Instant.now()) != 1) {
            audit.logIndependent(null, "PASSWORD_RESET_FAILED", "USER", null, null);
            throw ApiException.badRequest("This reset link is invalid or has expired");
        }
        User u = tokens.findByTokenHash(hash).orElseThrow().getUser();
        if (!u.isEnabled()) throw ApiException.badRequest("This reset link is invalid or has expired");
        u.setPasswordHash(encoder.encode(r.password()));
        audit.log(u.getId(), "PASSWORD_RESET_COMPLETED", "USER", String.valueOf(u.getId()), null);
    }

    private static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}