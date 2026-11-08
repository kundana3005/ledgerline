package com.ledgerline.backend.repository;

import com.ledgerline.backend.entity.Account;
import com.ledgerline.backend.entity.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.*;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByOwnerIdOrderByIdAsc(Long ownerId);

    // Ownership check built into the query: other people's accounts simply do not exist for you
    Optional<Account> findByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsByAccountNumber(String accountNumber);

    long countByTypeNot(AccountType type);

    @Query("select a.id from Account a where a.accountNumber = :number")
    Optional<Long> findIdByAccountNumber(@Param("number") String number);

    @Query("select a from Account a left join fetch a.owner order by a.id")
    List<Account> findAllWithOwner();

    @Query("select sum(a.balance) from Account a where a.type <> :type")
    BigDecimal totalBalanceExcluding(@Param("type") AccountType type);

    // Locks rows in ID order so concurrent requests cannot overdraw or deadlock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> lockAllByIdIn(@Param("ids") Collection<Long> ids);
}