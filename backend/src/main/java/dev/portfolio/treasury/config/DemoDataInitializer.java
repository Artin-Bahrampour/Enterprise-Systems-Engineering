package dev.portfolio.treasury.config;

import dev.portfolio.treasury.domain.*;
import dev.portfolio.treasury.repository.AuditEventRepository;
import dev.portfolio.treasury.repository.CashAccountRepository;
import dev.portfolio.treasury.repository.CashMovementRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Configuration
@Profile("demo")
public class DemoDataInitializer {
    @Bean
    ApplicationRunner seedDemoData(CashAccountRepository accounts, CashMovementRepository movements,
                                   AuditEventRepository audit, PlatformTransactionManager transactionManager,
                                   @Value("${app.demo-data.enabled:false}") boolean enabled) {
        return args -> {
            if (!enabled || accounts.count() > 0) return;
            new TransactionTemplate(transactionManager).executeWithoutResult(
                    status -> seed(accounts, movements, audit));
        };
    }

    void seed(CashAccountRepository accounts, CashMovementRepository movements, AuditEventRepository audit) {
        record SeedAccount(String name, String bank, String currency, String opening) {}
        List<SeedAccount> specs = List.of(
                new SeedAccount("Operating NOK", "Nordic Commercial Bank", "NOK", "850000.0000"),
                new SeedAccount("Reserve EUR", "Continental Bank", "EUR", "420000.0000"),
                new SeedAccount("Settlement USD", "Atlantic Trust", "USD", "675000.0000"));
        for (SeedAccount spec : specs) {
            CashAccount account = new CashAccount(UUID.randomUUID(), spec.name(), spec.bank(), spec.currency(),
                    new BigDecimal(spec.opening()), new BigDecimal("25000.0000"));
            accounts.save(account);
            audit.save(new AuditEvent(UUID.randomUUID(), "demo-seed", "ACCOUNT_CREATED", "CASH_ACCOUNT",
                    account.getId(), "Demo account seeded; currency=" + account.getCurrency()));
            BigDecimal balance = account.getCurrentBalance();
            for (int i = 55; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                Direction direction = (i % 4 == 0 || i % 4 == 1) ? Direction.CREDIT : Direction.DEBIT;
                BigDecimal amount = new BigDecimal((direction == Direction.CREDIT ? 18500 + (i % 5) * 1300 : 11200 + (i % 7) * 900) + ".0000");
                balance = direction == Direction.CREDIT ? balance.add(amount) : balance.subtract(amount);
                CashMovement movement = new CashMovement(UUID.randomUUID(), account, "demo-" + account.getId() + "-" + i,
                        "SEED-" + spec.currency() + "-" + i, direction, amount, date,
                        direction == Direction.CREDIT ? "Customer receipts" : "Supplier and operating payments", balance);
                movements.save(movement);
            }
            account.setCurrentBalance(balance);
            accounts.save(account);
        }
    }
}
