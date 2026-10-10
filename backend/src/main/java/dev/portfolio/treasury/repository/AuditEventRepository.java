package dev.portfolio.treasury.repository;

import dev.portfolio.treasury.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findTop100ByOrderByCreatedAtDesc();
    List<AuditEvent> findTop50ByOrderByCreatedAtDesc();
}
