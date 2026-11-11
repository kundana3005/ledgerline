package com.ledgerline.backend.service;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.entity.User;
import com.ledgerline.backend.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final AuditService audit;
    private final SecurityMonitor monitor;

    public AuthService(UserRepository users, PasswordEncoder encoder, TokenService tokens,
                       AuditService audit, SecurityMonitor monitor) {
        this.users = users; this.encoder = encoder; this.tokens = tokens; this.audit = audit; this.monitor = monitor;
    }

    public LoginResponse login(LoginRequest req) {
        Optional<User> found = users.findByUsernameIgnoreCaseOrEmailIgnoreCase(req.identifier(), req.identifier());
        if (found.isEmpty() || !found.get().isEnabled()
                || !encoder.matches(req.password(), found.get().getPasswordHash())) {
            monitor.failedLogin(found.map(User::getId).orElse(null), mask(req.identifier()));
            throw new BadCredentialsException("Invalid credentials");   // same message either way
        }
        User user = found.get();
        audit.logIndependent(user.getId(), "LOGIN_SUCCESS", "USER", String.valueOf(user.getId()), null);
        return new LoginResponse(tokens.issue(user), user.getId(), user.getUsername(),
                user.getRole().name(), user.getStatus().name());
    }

    /** Only the first two characters are kept: people sometimes type a password into the username box. */
    private static String mask(String s) {
        String t = s == null ? "" : s.trim();
        return t.length() <= 2 ? "***" : t.substring(0, 2) + "***";
    }
}