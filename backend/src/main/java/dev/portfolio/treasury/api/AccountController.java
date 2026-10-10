package dev.portfolio.treasury.api;

import dev.portfolio.treasury.api.ApiModels.AccountResponse;
import dev.portfolio.treasury.api.ApiModels.CreateAccountRequest;
import dev.portfolio.treasury.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service) { this.service = service; }

    @GetMapping
    public List<AccountResponse> list() { return service.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request, Authentication auth) {
        return service.create(request, auth.getName());
    }
}
