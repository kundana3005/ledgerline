package com.ledgerline.backend.repository;

import com.ledgerline.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserId(Long userId, Pageable p);
    Page<Notification> findByUserIdAndSeenFalse(Long userId, Pageable p);
    long countByUserIdAndSeenFalse(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.seen = true where n.id = :id and n.userId = :uid")
    int markRead(@Param("id") Long id, @Param("uid") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.seen = true where n.userId = :uid and n.seen = false")
    int markAllRead(@Param("uid") Long userId);
}