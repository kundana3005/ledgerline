package com.ledgerline.backend.service;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.entity.User;
import com.ledgerline.backend.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final AuditService audit;

    public AuthService(UserRepository users, PasswordEncoder encoder, TokenService tokens, AuditService audit) {
        this.users = users; this.encoder = encoder; this.tokens = tokens; this.audit = audit;
    }

    public LoginResponse login(LoginRequest req) {
        Optional<User> found = users.findByUsernameIgnoreCaseOrEmailIgnoreCase(req.identifier(), req.identifier());
        if (found.isEmpty() || !found.get().isEnabled()
                || !encoder.matches(req.password(), found.get().getPasswordHash())) {
            audit.logIndependent(found.map(User::getId).orElse(null), "LOGIN_FAILED", "USER", null,
                    Map.of("identifier", req.identifier()));
            throw new BadCredentialsException("Invalid credentials");   // same message either way
        }
        User user = found.get();
        audit.logIndependent(user.getId(), "LOGIN_SUCCESS", "USER", String.valueOf(user.getId()), null);
        return new LoginResponse(tokens.issue(user), user.getId(), user.getUsername(), user.getRole().name());
    }
}