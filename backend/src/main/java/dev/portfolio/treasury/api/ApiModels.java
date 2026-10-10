package dev.portfolio.treasury.api;

import dev.portfolio.treasury.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ApiModels {
    private ApiModels() {}

    public record CreateAccountRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 120) String bankName,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @NotNull @Digits(integer = 15, fraction = 4) BigDecimal openingBalance,
            @NotNull @DecimalMin("0.0000") @Digits(integer = 15, fraction = 4) BigDecimal overdraftLimit) {}

    public record AccountResponse(UUID id, String name, String bankName, String currency,
                                  BigDecimal openingBalance, BigDecimal currentBalance,
                                  BigDecimal overdraftLimit, Instant createdAt, long version) {
        public static AccountResponse from(CashAccount a) {
            return new AccountResponse(a.getId(), a.getName(), a.getBankName(), a.getCurrency(),
                    a.getOpeningBalance(), a.getCurrentBalance(), a.getOverdraftLimit(), a.getCreatedAt(), a.getVersion());
        }
    }

    public record CreateMovementRequest(
            @NotNull Direction direction,
            @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
            @NotNull @PastOrPresent LocalDate valueDate,
            @NotBlank @Size(max = 240) String description,
            @Size(max = 120) String externalReference) {}

    public record MovementResponse(UUID id, UUID accountId, String accountName, String currency,
                                   String idempotencyKey, String externalReference, Direction direction,
                                   BigDecimal amount, LocalDate valueDate, String description,
                                   BigDecimal balanceAfter, Instant createdAt, boolean idempotentReplay) {
        public static MovementResponse from(CashMovement m, boolean replay) {
            return new MovementResponse(m.getId(), m.getAccount().getId(), m.getAccount().getName(),
                    m.getAccount().getCurrency(), m.getIdempotencyKey(), m.getExternalReference(),
                    m.getDirection(), m.getAmount(), m.getValueDate(), m.getDescription(),
                    m.getBalanceAfter(), m.getCreatedAt(), replay);
        }
    }

    public record ForecastPoint(LocalDate date, UUID accountId, String accountName, String currency,
                                BigDecimal openingBalance, BigDecimal expectedInflows,
                                BigDecimal expectedOutflows, BigDecimal closingBalance) {}

    public record ForecastResponse(String scenario, int days, Instant generatedAt,
                                   LocalDate historyFrom, LocalDate historyTo,
                                   java.util.List<ForecastPoint> points) {}

    public record AuditResponse(UUID id, String actor, String action, String entityType,
                                UUID entityId, String details, Instant createdAt) {
        public static AuditResponse from(AuditEvent e) {
            return new AuditResponse(e.getId(), e.getActor(), e.getAction(), e.getEntityType(),
                    e.getEntityId(), e.getDetails(), e.getCreatedAt());
        }
    }
}
