package com.ledgerline.backend.service;

import com.ledgerline.backend.dto.NotificationView;
import com.ledgerline.backend.dto.PageResponse;
import com.ledgerline.backend.entity.Notification;
import com.ledgerline.backend.exception.ApiException;
import com.ledgerline.backend.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository repo;
    public NotificationService(NotificationRepository repo) { this.repo = repo; }

    /** Joins the caller's transaction: it disappears if the money movement rolls back. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void notify(Long userId, String type, String message) { repo.save(new Notification(userId, type, message)); }

    /** Own transaction: survives a rollback (used for failure notices). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyIndependent(Long userId, String type, String message) { repo.save(new Notification(userId, type, message)); }

    @Transactional(readOnly = true)
    public PageResponse<NotificationView> list(Long userId, boolean unreadOnly, int page, int size) {
        Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of((unreadOnly ? repo.findByUserIdAndSeenFalse(userId, p) : repo.findByUserId(userId, p))
                .map(NotificationView::from));
    }

    @Transactional(readOnly = true)
    public long unread(Long userId) { return repo.countByUserIdAndSeenFalse(userId); }

    @Transactional
    public void markRead(Long userId, Long id) {
        if (repo.markRead(id, userId) == 0) throw ApiException.resourceNotFound("Notification not found");
    }

    @Transactional
    public void markAllRead(Long userId) { repo.markAllRead(userId); }
}