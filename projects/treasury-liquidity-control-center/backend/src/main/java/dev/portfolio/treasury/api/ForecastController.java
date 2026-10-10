package dev.portfolio.treasury.api;

import dev.portfolio.treasury.api.ApiModels.ForecastResponse;
import dev.portfolio.treasury.service.ForecastService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forecasts")
public class ForecastController {
    private final ForecastService service;
    public ForecastController(ForecastService service) { this.service = service; }

    @GetMapping
    public ForecastResponse forecast(@RequestParam(defaultValue = "30") int days,
                                     @RequestParam(defaultValue = "BASE") String scenario) {
        return service.forecast(days, scenario);
    }
}
