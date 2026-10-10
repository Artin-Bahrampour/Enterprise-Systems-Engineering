package dev.portfolio.treasury.repository;

import dev.portfolio.treasury.domain.CashAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashAccountRepository extends JpaRepository<CashAccount, UUID> {
    List<CashAccount> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CashAccount a where a.id = :id")
    Optional<CashAccount> findByIdForUpdate(@Param("id") UUID id);
}
