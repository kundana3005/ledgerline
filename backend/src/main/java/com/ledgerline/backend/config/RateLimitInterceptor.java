package com.ledgerline.backend.config;

import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.service.AuditService;
import com.ledgerline.backend.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.time.Duration;
import java.util.Map;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private record Rule(String name, int limit, Duration window, boolean perUser) {}

    private final RateLimiter limiter;
    private final AuditService audit;
    private final Rule login, register, forgot, reset, change, tx;

    public RateLimitInterceptor(RateLimiter limiter, AuditService audit,
            @Value("${app.rate-limit.login-per-minute:10}") int loginPerMin,
            @Value("${app.rate-limit.register-per-hour:5}") int registerPerHour,
            @Value("${app.rate-limit.forgot-per-hour:5}") int forgotPerHour,
            @Value("${app.rate-limit.reset-per-hour:10}") int resetPerHour,
            @Value("${app.rate-limit.password-change-per-hour:5}") int changePerHour,
            @Value("${app.rate-limit.transactions-per-minute:30}") int txPerMin) {
        this.limiter = limiter; this.audit = audit;
        login = new Rule("login", loginPerMin, Duration.ofMinutes(1), false);
        register = new Rule("register", registerPerHour, Duration.ofHours(1), false);
        forgot = new Rule("forgot", forgotPerHour, Duration.ofHours(1), false);
        reset = new Rule("reset", resetPerHour, Duration.ofHours(1), false);
        change = new Rule("change-password", changePerHour, Duration.ofHours(1), true);
        tx = new Rule("transactions", txPerMin, Duration.ofMinutes(1), true);
    }

    private Rule match(String path) {
        return switch (path) {
            case "/api/auth/login" -> login;
            case "/api/auth/register" -> register;
            case "/api/auth/forgot-password" -> forgot;
            case "/api/auth/reset-password" -> reset;
            case "/api/auth/change-password" -> change;
            default -> path.startsWith("/api/transactions/") ? tx : null;
        };
    }

    private Long currentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a instanceof JwtAuthenticationToken t) {
            try { return Long.valueOf(t.getName()); } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (!"POST".equals(req.getMethod())) return true;
        Rule rule = match(req.getRequestURI());
        if (rule == null) return true;
        Long userId = rule.perUser() ? currentUserId() : null;
        String who = userId != null ? "u:" + userId : "ip:" + req.getRemoteAddr();
        long n = limiter.hit("rl:" + rule.name() + ":" + who, rule.window());
        if (n > rule.limit()) {
            if (n == rule.limit() + 1L) {   // audit only the first rejection per window, so a flood cannot fill the log
                audit.logIndependent(userId, "RATE_LIMIT_EXCEEDED", "ENDPOINT", req.getRequestURI(),
                        Map.of("rule", rule.name()));
            }
            res.setHeader("Retry-After", String.valueOf(rule.window().toSeconds()));
            throw ApiException.tooManyRequests("Too many requests. Please wait a moment and try again.");
        }
        return true;
    }
}