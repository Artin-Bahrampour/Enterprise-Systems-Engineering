package dev.portfolio.treasury.api;

import dev.portfolio.treasury.api.ApiModels.CreateMovementRequest;
import dev.portfolio.treasury.api.ApiModels.MovementResponse;
import dev.portfolio.treasury.service.MovementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class MovementController {
    private final MovementService service;
    public MovementController(MovementService service) { this.service = service; }

    @PostMapping("/api/accounts/{accountId}/movements")
    public ResponseEntity<MovementResponse> post(@PathVariable UUID accountId,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody CreateMovementRequest request, Authentication auth) {
        var result = service.post(accountId, key, request, auth.getName());
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(result.movement());
    }

    @GetMapping("/api/accounts/{accountId}/movements")
    public List<MovementResponse> list(@PathVariable UUID accountId,
            @RequestParam(defaultValue = "2000-01-01") LocalDate from,
            @RequestParam(defaultValue = "2100-12-31") LocalDate to) {
        return service.list(accountId, from, to);
    }

    @GetMapping("/api/movements/recent")
    public List<MovementResponse> recent() { return service.latest(); }
}
