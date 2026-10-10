package dev.portfolio.treasury.service;

import dev.portfolio.treasury.domain.*;
import dev.portfolio.treasury.repository.CashAccountRepository;
import dev.portfolio.treasury.repository.CashMovementRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ForecastServiceTest {
    @Test
    void stressScenarioReducesInflowsAndIncreasesOutflows() {
        CashAccountRepository accounts = mock(CashAccountRepository.class);
        CashMovementRepository movements = mock(CashMovementRepository.class);
        CashAccount account = new CashAccount(UUID.randomUUID(), "Operating", "Bank", "NOK",
                new BigDecimal("1000.0000"), new BigDecimal("100.0000"));
        when(accounts.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(account));
        LocalDate today = LocalDate.now();
        List<CashMovement> history = List.of(
                new CashMovement(UUID.randomUUID(), account, "k1", null, Direction.CREDIT,
                        new BigDecimal("100.0000"), today.minusDays(6), "receipt", new BigDecimal("1100.0000")),
                new CashMovement(UUID.randomUUID(), account, "k2", null, Direction.DEBIT,
                        new BigDecimal("40.0000"), today.minusDays(13), "payment", new BigDecimal("1060.0000")));
        when(movements.findByAccountIdAndValueDateBetween(eq(account.getId()), any(), any())).thenReturn(history);

        ForecastService service = new ForecastService(accounts, movements);
        var base = service.forecast(7, "BASE");
        var stress = service.forecast(7, "STRESS");

        assertThat(base.points()).hasSize(7);
        var basePoint = base.points().get(0);
        var stressPoint = stress.points().get(0);
        assertThat(stressPoint.expectedInflows()).isLessThanOrEqualTo(basePoint.expectedInflows());
        assertThat(stressPoint.expectedOutflows()).isGreaterThanOrEqualTo(basePoint.expectedOutflows());
        assertThat(stress.scenario()).isEqualTo("STRESS");
    }

    @Test
    void rejectsForecastBeyondNinetyDays() {
        ForecastService service = new ForecastService(mock(CashAccountRepository.class), mock(CashMovementRepository.class));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.forecast(91, "BASE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
