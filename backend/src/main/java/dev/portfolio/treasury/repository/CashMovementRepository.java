package dev.portfolio.treasury.repository;

import dev.portfolio.treasury.domain.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashMovementRepository extends JpaRepository<CashMovement, UUID> {
    Optional<CashMovement> findByAccountIdAndIdempotencyKey(UUID accountId, String idempotencyKey);
    List<CashMovement> findByAccountIdAndValueDateBetweenOrderByValueDateDescCreatedAtDesc(
            UUID accountId, LocalDate from, LocalDate to);
    List<CashMovement> findTop50ByOrderByCreatedAtDesc();
    List<CashMovement> findByAccountIdAndValueDateBetween(UUID accountId, LocalDate from, LocalDate to);
}
