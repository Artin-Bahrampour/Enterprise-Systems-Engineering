package dev.portfolio.treasury.api;

import dev.portfolio.treasury.api.ApiModels.AuditResponse;
import dev.portfolio.treasury.service.AuditService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final AuditService service;
    public AuditController(AuditService service) { this.service = service; }

    @GetMapping
    public List<AuditResponse> latest(@RequestParam(defaultValue = "50") int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100.");
        return service.latest(limit).stream().map(AuditResponse::from).toList();
    }
}
