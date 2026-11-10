package com.ledgerline.backend.service;

import com.ledgerline.backend.dto.RegisterRequest;
import com.ledgerline.backend.dto.RegisterResponse;
import com.ledgerline.backend.entity.Role;
import com.ledgerline.backend.entity.User;
import com.ledgerline.backend.entity.UserStatus;
import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class RegistrationService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public RegistrationService(UserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users; this.encoder = encoder; this.audit = audit;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest r) {
        if (!r.password().equals(r.confirmPassword())) {
            throw ApiException.badRequest("Passwords do not match");
        }
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email) || users.existsByUsernameIgnoreCase(email)) {
            audit.logIndependent(null, "REGISTRATION_REJECTED", "USER", null, Map.of("reason", "DUPLICATE_EMAIL"));
            throw ApiException.conflict("An account with this email already exists");
        }
        // The role is fixed here. The request has no role field, so users cannot choose one.
        User u = new User(email, email, encoder.encode(r.password()), Role.ACCOUNT_HOLDER);
        u.setFullName(r.fullName().trim());
        u.setPhone(r.phone());
        u.setStatus(UserStatus.PENDING_APPROVAL);
        u = users.saveAndFlush(u);   // a concurrent duplicate fails here on the unique constraint (409)

        audit.log(u.getId(), "USER_REGISTERED", "USER", String.valueOf(u.getId()), Map.of("status", u.getStatus().name()));
        return new RegisterResponse(email, u.getStatus().name(),
                "Registration received. An administrator will review your application.");
    }
}