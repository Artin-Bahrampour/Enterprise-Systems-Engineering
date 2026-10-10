package dev.portfolio.treasury.service;

import dev.portfolio.treasury.api.ApiModels.ForecastPoint;
import dev.portfolio.treasury.api.ApiModels.ForecastResponse;
import dev.portfolio.treasury.domain.CashAccount;
import dev.portfolio.treasury.domain.CashMovement;
import dev.portfolio.treasury.domain.Direction;
import dev.portfolio.treasury.repository.CashAccountRepository;
import dev.portfolio.treasury.repository.CashMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ForecastService {
    public enum Scenario {
        BASE(new BigDecimal("1.00"), new BigDecimal("1.00")),
        STRESS(new BigDecimal("0.80"), new BigDecimal("1.15")),
        UPSIDE(new BigDecimal("1.10"), new BigDecimal("0.95"));
        final BigDecimal inflowMultiplier;
        final BigDecimal outflowMultiplier;
        Scenario(BigDecimal inflowMultiplier, BigDecimal outflowMultiplier) {
            this.inflowMultiplier = inflowMultiplier; this.outflowMultiplier = outflowMultiplier;
        }
    }

    private record DailyFlows(BigDecimal credits, BigDecimal debits) {
        DailyFlows add(CashMovement movement) {
            return movement.getDirection() == Direction.CREDIT
                    ? new DailyFlows(credits.add(movement.getAmount()), debits)
                    : new DailyFlows(credits, debits.add(movement.getAmount()));
        }
    }

    private final CashAccountRepository accounts;
    private final CashMovementRepository movements;

    public ForecastService(CashAccountRepository accounts, CashMovementRepository movements) {
        this.accounts = accounts; this.movements = movements;
    }

    @Transactional(readOnly = true)
    public ForecastResponse forecast(int days, String rawScenario) {
        if (days < 1 || days > 90) throw new IllegalArgumentException("days must be between 1 and 90.");
        Scenario scenario;
        try { scenario = Scenario.valueOf(rawScenario.toUpperCase(Locale.ROOT)); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("scenario must be BASE, STRESS, or UPSIDE."); }

        LocalDate today = LocalDate.now();
        LocalDate historyFrom = today.minusDays(55);
        List<ForecastPoint> points = new ArrayList<>();
        for (CashAccount account : accounts.findAllByOrderByCreatedAtDesc()) {
            List<CashMovement> history = movements.findByAccountIdAndValueDateBetween(
                    account.getId(), historyFrom, today);
            Map<LocalDate, DailyFlows> daily = new HashMap<>();
            for (LocalDate date = historyFrom; !date.isAfter(today); date = date.plusDays(1)) {
                daily.put(date, new DailyFlows(BigDecimal.ZERO, BigDecimal.ZERO));
            }
            for (CashMovement movement : history) {
                daily.computeIfPresent(movement.getValueDate(),
                        (date, flows) -> flows.add(movement));
            }

            BigDecimal balance = account.getCurrentBalance();
            for (int offset = 1; offset <= days; offset++) {
                LocalDate date = today.plusDays(offset);
                List<DailyFlows> comparableDays = daily.entrySet().stream()
                        .filter(entry -> entry.getKey().getDayOfWeek() == date.getDayOfWeek())
                        .map(Map.Entry::getValue)
                        .toList();
                BigDecimal inflows = averageDaily(comparableDays, true).multiply(scenario.inflowMultiplier)
                        .setScale(4, RoundingMode.HALF_UP);
                BigDecimal outflows = averageDaily(comparableDays, false).multiply(scenario.outflowMultiplier)
                        .setScale(4, RoundingMode.HALF_UP);
                BigDecimal opening = balance;
                balance = balance.add(inflows).subtract(outflows).setScale(4, RoundingMode.HALF_UP);
                points.add(new ForecastPoint(date, account.getId(), account.getName(), account.getCurrency(),
                        opening, inflows, outflows, balance));
            }
        }
        return new ForecastResponse(scenario.name(), days, Instant.now(), historyFrom, today, points);
    }

    private static BigDecimal averageDaily(List<DailyFlows> days, boolean credits) {
        if (days.isEmpty()) return BigDecimal.ZERO.setScale(4, RoundingMode.UNNECESSARY);
        BigDecimal total = days.stream()
                .map(day -> credits ? day.credits() : day.debits())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(days.size()), 4, RoundingMode.HALF_UP);
    }
}
