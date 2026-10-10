package dev.portfolio.treasury.service;

import dev.portfolio.treasury.api.ApiModels.AccountResponse;
import dev.portfolio.treasury.api.ApiModels.CreateAccountRequest;
import dev.portfolio.treasury.domain.CashAccount;
import dev.portfolio.treasury.repository.CashAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {
    private final CashAccountRepository accounts;
    private final AuditService audit;

    public AccountService(CashAccountRepository accounts, AuditService audit) {
        this.accounts = accounts; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        return accounts.findAllByOrderByCreatedAtDesc().stream().map(AccountResponse::from).toList();
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request, String actor) {
        BigDecimal opening = request.openingBalance().setScale(4, java.math.RoundingMode.UNNECESSARY);
        BigDecimal overdraft = request.overdraftLimit().setScale(4, java.math.RoundingMode.UNNECESSARY);
        if (opening.compareTo(overdraft.negate()) < 0) {
            throw new DomainExceptions.InsufficientFunds("Opening balance cannot be below the configured overdraft limit.");
        }
        CashAccount account = new CashAccount(UUID.randomUUID(), request.name().trim(),
                request.bankName().trim(), request.currency(), opening, overdraft);
        accounts.save(account);
        audit.record(actor, "ACCOUNT_CREATED", "CASH_ACCOUNT", account.getId(),
                "currency=" + account.getCurrency() + "; openingBalance=" + opening);
        return AccountResponse.from(account);
    }
}
