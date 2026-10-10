# Architecture and key decisions

## Bounded modules

- `api`: transport DTOs, REST controllers, validation, and consistent Problem Details responses.
- `service`: account lifecycle, transactional movement posting, audit recording, and forecast calculation.
- `domain`: persistence entities and the cash movement direction.
- `repository`: Spring Data interfaces and an explicit pessimistic lock for cash-account posting.

The project uses a modular monolith. The transaction boundary is the critical design choice: an account balance update, movement record, and audit event either commit together or roll back together.

## Concurrency and idempotency

Movement posting locks the account row with `SELECT ... FOR UPDATE` through a JPA pessimistic write lock. Requests for the same account serialize around balance validation and update. A unique database constraint on `(account_id, idempotency_key)` is the final integrity guard.

The service checks the existing idempotency record before applying a balance change. A matching request returns the original movement. A changed payload with the same key returns HTTP 409. A concurrent uniqueness conflict is also converted into a safe conflict response; clients should retry only with the same key and payload.

## Money and currency

All monetary values use `BigDecimal` and PostgreSQL `NUMERIC(19,4)`. Inputs with more than four decimal places are rejected rather than silently rounded. Account balances are currency-specific. No aggregate across NOK, EUR, USD, or other currencies is presented as a single monetary amount.

## Forecast method

For each account and each forecast date, the service averages each day's total credits and total debits across the matching weekdays in a 56-calendar-day lookback, including zero-flow days. The current balance is the forecast opening point. BASE applies 1.00 multipliers; STRESS applies 0.80 to credits and 1.15 to debits; UPSIDE applies 1.10 to credits and 0.95 to debits. This is intentionally transparent and testable, not a substitute for treasury forecasting software with contractual schedules, seasonality, FX curves, and calibrated statistical models.

## Observability

Spring Boot Actuator exposes liveness/readiness health and Prometheus metrics. The API logs through Spring Boot's structured logging facilities. Operational logs must not include passwords, authorization headers, or full movement payloads. Metrics are intended for a trusted internal network; do not expose the actuator endpoint publicly without access controls.
