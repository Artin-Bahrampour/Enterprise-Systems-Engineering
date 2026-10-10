package dev.portfolio.treasury.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private UUID id;
    @Column(nullable = false, length = 120)
    private String actor;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    @Column(nullable = false, columnDefinition = "text")
    private String details;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {}
    public AuditEvent(UUID id, String actor, String action, String entityType, UUID entityId, String details) {
        this.id = id; this.actor = actor; this.action = action; this.entityType = entityType;
        this.entityId = entityId; this.details = details; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
