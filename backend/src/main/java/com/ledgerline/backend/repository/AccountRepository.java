package com.ledgerline.backend.repository;

import com.ledgerline.backend.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByOwnerIdOrderByIdAsc(Long ownerId);

    @Query("select a.id from Account a where a.accountNumber = :number")
    Optional<Long> findIdByAccountNumber(@Param("number") String number);

    // Used by transfers: locks rows in ID order so concurrent requests cannot overdraw or deadlock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> lockAllByIdIn(@Param("ids") Collection<Long> ids);
}