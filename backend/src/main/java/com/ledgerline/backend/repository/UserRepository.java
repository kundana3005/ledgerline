package com.ledgerline.backend.repository;

import com.ledgerline.backend.entity.Role;
import com.ledgerline.backend.entity.User;
import com.ledgerline.backend.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    long countByRole(Role role);
    long countByStatus(UserStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") Long id);

    @Query("select u from User u where (lower(u.username) like :q or lower(u.email) like :q " +
           "or lower(coalesce(u.fullName, '')) like :q) " +
           "and u.role in :roles and u.status in :statuses and u.enabled in :enabled")
    Page<User> search(@Param("q") String q, @Param("roles") Collection<Role> roles,
                      @Param("statuses") Collection<UserStatus> statuses,
                      @Param("enabled") Collection<Boolean> enabled, Pageable pageable);
}