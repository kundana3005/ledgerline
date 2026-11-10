package com.ledgerline.backend.repository;

import com.ledgerline.backend.entity.AccountRequest;
import com.ledgerline.backend.entity.AccountRequestStatus;
import com.ledgerline.backend.entity.AccountStatus;
import com.ledgerline.backend.entity.AccountType;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ledgerline.backend.entity.AccountStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {
    List<AccountRequest> findByUser_IdOrderByIdDesc(Long userId);

    long countByStatus(AccountRequestStatus status);
    
    @EntityGraph(attributePaths = {"user"})
    Page<AccountRequest> findByStatus(AccountRequestStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AccountRequest r join fetch r.user where r.id = :id")
    Optional<AccountRequest> lockById(@Param("id") Long id);
}