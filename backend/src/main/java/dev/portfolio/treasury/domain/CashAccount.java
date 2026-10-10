package dev.portfolio.treasury.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cash_accounts")
public class CashAccount {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "bank_name", nullable = false, length = 120)
    private String bankName;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingBalance;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Column(name = "overdraft_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal overdraftLimit;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected CashAccount() {}

    public CashAccount(UUID id, String name, String bankName, String currency,
                       BigDecimal openingBalance, BigDecimal overdraftLimit) {
        this.id = id;
        this.name = name;
        this.bankName = bankName;
        this.currency = currency;
        this.openingBalance = openingBalance;
        this.currentBalance = openingBalance;
        this.overdraftLimit = overdraftLimit;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getBankName() { return bankName; }
    public String getCurrency() { return currency; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getOverdraftLimit() { return overdraftLimit; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    public void setCurrentBalance(BigDecimal value) { this.currentBalance = value; }
}
