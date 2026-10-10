package dev.portfolio.treasury.service;

import dev.portfolio.treasury.api.ApiModels.CreateMovementRequest;
import dev.portfolio.treasury.api.ApiModels.MovementResponse;
import dev.portfolio.treasury.domain.CashAccount;
import dev.portfolio.treasury.domain.CashMovement;
import dev.portfolio.treasury.domain.Direction;
import dev.portfolio.treasury.repository.CashAccountRepository;
import dev.portfolio.treasury.repository.CashMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MovementService {
    public record PostingResult(MovementResponse movement, boolean replayed) {}

    private final CashAccountRepository accounts;
    private final CashMovementRepository movements;
    private final AuditService audit;

    public MovementService(CashAccountRepository accounts, CashMovementRepository movements, AuditService audit) {
        this.accounts = accounts; this.movements = movements; this.audit = audit;
    }

    @Transactional
    public PostingResult post(UUID accountId, String key, CreateMovementRequest request, String actor) {
        if (key == null || key.isBlank() || key.length() > 120) {
            throw new IllegalArgumentException("Idempotency-Key header is required and must be at most 120 characters.");
        }
        key = key.trim();
        if (request.valueDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Value date cannot be in the future; forecast assumptions are used for future liquidity.");
        }

        CashAccount account = accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> new DomainExceptions.ResourceNotFound("Cash account not found: " + accountId));

        var prior = movements.findByAccountIdAndIdempotencyKey(accountId, key);
        if (prior.isPresent()) {
            CashMovement existing = prior.get();
            boolean same = existing.getDirection() == request.direction()
                    && existing.getAmount().compareTo(request.amount()) == 0
                    && existing.getValueDate().equals(request.valueDate())
                    && existing.getDescription().equals(request.description().trim())
                    && java.util.Objects.equals(existing.getExternalReference(), normalize(request.externalReference()));
            if (!same) {
                throw new DomainExceptions.IdempotencyConflict("Idempotency-Key was already used with a different payload.");
            }
            return new PostingResult(MovementResponse.from(existing, true), true);
        }

        BigDecimal amount = request.amount().setScale(4, RoundingMode.UNNECESSARY);
        BigDecimal delta = request.direction() == Direction.CREDIT ? amount : amount.negate();
        BigDecimal newBalance = account.getCurrentBalance().add(delta).setScale(4, RoundingMode.UNNECESSARY);
        if (newBalance.compareTo(account.getOverdraftLimit().negate()) < 0) {
            throw new DomainExceptions.InsufficientFunds("Movement would breach the account overdraft limit.");
        }

        account.setCurrentBalance(newBalance);
        accounts.save(account);
        CashMovement movement = new CashMovement(UUID.randomUUID(), account, key, normalize(request.externalReference()),
                request.direction(), amount, request.valueDate(), request.description().trim(), newBalance);
        movements.save(movement);
        audit.record(actor, "CASH_MOVEMENT_POSTED", "CASH_MOVEMENT", movement.getId(),
                "accountId=" + accountId + "; direction=" + request.direction() + "; amount=" + amount
                        + "; currency=" + account.getCurrency() + "; balanceAfter=" + newBalance);
        return new PostingResult(MovementResponse.from(movement, false), false);
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> list(UUID accountId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new IllegalArgumentException("'from' must be on or before 'to'.");
        if (!accounts.existsById(accountId)) throw new DomainExceptions.ResourceNotFound("Cash account not found: " + accountId);
        return movements.findByAccountIdAndValueDateBetweenOrderByValueDateDescCreatedAtDesc(accountId, from, to)
                .stream().map(m -> MovementResponse.from(m, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> latest() {
        return movements.findTop50ByOrderByCreatedAtDesc().stream().map(m -> MovementResponse.from(m, false)).toList();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
