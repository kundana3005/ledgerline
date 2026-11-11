package com.ledgerline.backend.controller;

import com.ledgerline.backend.dto.NotificationView;
import com.ledgerline.backend.dto.PageResponse;
import com.ledgerline.backend.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    private static Long id(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    @GetMapping
    public PageResponse<NotificationView> list(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(defaultValue = "false") boolean unreadOnly,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return service.list(id(jwt), unreadOnly, page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal Jwt jwt) { return Map.of("count", service.unread(id(jwt))); }

    @PutMapping("/{nid}/read")
    public Map<String, String> read(@AuthenticationPrincipal Jwt jwt, @PathVariable Long nid) {
        service.markRead(id(jwt), nid);
        return Map.of("status", "read");
    }

    @PutMapping("/read-all")
    public Map<String, String> readAll(@AuthenticationPrincipal Jwt jwt) {
        service.markAllRead(id(jwt));
        return Map.of("status", "read");
    }
}