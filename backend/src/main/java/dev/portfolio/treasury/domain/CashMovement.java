package dev.portfolio.treasury.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cash_movements",
       uniqueConstraints = @UniqueConstraint(name = "uq_movement_account_idempotency",
               columnNames = {"account_id", "idempotency_key"}))
public class CashMovement {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private CashAccount account;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "external_reference", length = 120)
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Direction direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CashMovement() {}

    public CashMovement(UUID id, CashAccount account, String idempotencyKey, String externalReference,
                        Direction direction, BigDecimal amount, LocalDate valueDate,
                        String description, BigDecimal balanceAfter) {
        this.id = id;
        this.account = account;
        this.idempotencyKey = idempotencyKey;
        this.externalReference = externalReference;
        this.direction = direction;
        this.amount = amount;
        this.valueDate = valueDate;
        this.description = description;
        this.balanceAfter = balanceAfter;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public CashAccount getAccount() { return account; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getExternalReference() { return externalReference; }
    public Direction getDirection() { return direction; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getValueDate() { return valueDate; }
    public String getDescription() { return description; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public Instant getCreatedAt() { return createdAt; }
}
