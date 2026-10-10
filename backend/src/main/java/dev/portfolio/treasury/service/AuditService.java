package dev.portfolio.treasury.service;

import dev.portfolio.treasury.domain.AuditEvent;
import dev.portfolio.treasury.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditEventRepository repository;
    public AuditService(AuditEventRepository repository) { this.repository = repository; }

    @Transactional
    public void record(String actor, String action, String type, UUID id, String details) {
        repository.save(new AuditEvent(UUID.randomUUID(), actor, action, type, id, details));
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> latest(int limit) {
        if (limit <= 50) return repository.findTop50ByOrderByCreatedAtDesc().stream().limit(limit).toList();
        return repository.findTop100ByOrderByCreatedAtDesc().stream().limit(limit).toList();
    }
}
