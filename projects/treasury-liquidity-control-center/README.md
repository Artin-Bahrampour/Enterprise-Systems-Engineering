# Treasury Liquidity Control Center

A full-stack corporate treasury application for monitoring cash positions, recording controlled cash movements, and projecting account-level liquidity under base, stress, and upside scenarios.

## Business problem

Treasury teams need a reliable view of cash across bank accounts, a traceable record of movements, controls against accidental overdrafts, and forward-looking liquidity signals. Spreadsheet-based processes make idempotent ingestion, auditability, and scenario analysis difficult to enforce consistently.

This project demonstrates a compact, production-minded modular application. It is a portfolio reference implementation, not a regulated banking product and not intended for public deployment without additional controls.

## Capabilities

- Multi-account cash positions with explicit ISO currency codes; balances are never summed across currencies without FX conversion.
- Transactional cash movement posting using `BigDecimal`, row-level account locking, an overdraft-limit business rule, and per-account idempotency keys.
- Conflict detection when an idempotency key is reused with a different payload.
- Append-only audit events written in the same database transaction as account/movement changes.
- Daily liquidity forecasts based on the average daily cash flows for each weekday over the previous 56 calendar days, including zero-flow days, with BASE, STRESS, and UPSIDE assumptions.
- Spring Security HTTP Basic authentication for a controlled local demo, role-based endpoint rules, validation, Problem Details errors, Actuator health/metrics, and structured application logs.
- React + TypeScript interface for account management, movement posting, and scenario review.
- PostgreSQL, Flyway migrations, Docker Compose, integration tests with Testcontainers, and GitHub Actions CI.

## Architecture

```text
React + TypeScript (Nginx)
          | same-origin /api proxy
          v
Spring Boot REST API
  | account / movement / forecast services
  | validation + security + audit
  v
PostgreSQL (Flyway-managed schema)
```

The application is intentionally a modular monolith. Cash posting, balance updates, idempotency checks, and audit writes share a transaction boundary. That is simpler and safer than introducing distributed services for a portfolio-scale system.

## Technology choices

- **Java 21 + Spring Boot**: typed domain logic, transactional boundaries, validation, security, and mature operational tooling.
- **PostgreSQL**: durable ledger-like movement history, uniqueness constraints, row locks, and transactional audit records.
- **React + TypeScript + Vite**: explicit frontend contracts and a maintainable component boundary without a heavy UI framework.
- **Flyway**: versioned, reviewable schema evolution.
- **Testcontainers**: integration tests against the same database engine used in deployment.
- **Docker Compose + GitHub Actions**: repeatable local setup and automated verification.

## Run locally

Requirements: Docker Engine and Docker Compose v2.

```bash
cp .env.example .env
docker compose up --build --wait
```

Open <http://localhost:8088>. Demo credentials are configured in `.env.example` for local use only. Change them before running the stack on any shared machine.

Useful endpoints:
- `GET /actuator/health`
- `GET /actuator/prometheus`
- `GET /api/accounts`
- `POST /api/accounts`
- `GET /api/forecasts?days=30&scenario=BASE`
- `GET /api/audit?limit=50`

The `demo` Spring profile creates sample accounts and a deterministic set of historical movements on an empty database. To run without sample data, remove `SPRING_PROFILES_ACTIVE=demo` from Compose.

## Run tests

With Java 21 and Maven 3.9+ installed:

```bash
cd backend
mvn -B verify
```

The integration test uses Testcontainers and requires Docker. Frontend checks:

```bash
cd frontend
npm install
npm run build
npm run lint
```

## Important design boundaries

- **Posting semantics:** movement value dates must be today or earlier. A successful posting updates the current balance and creates its audit event atomically.
- **Idempotency:** keys are unique per account. Replaying the same key and payload returns the original movement; reusing it with a different payload returns HTTP 409.
- **Overdraft:** a debit is rejected if the resulting balance would fall below the account's configured overdraft limit.
- **Forecast:** forecasts are statistical planning estimates, not payment commitments. For each future weekday, the service averages total daily credits and debits across the eight prior matching weekdays in the previous 56 calendar days, including days without movements. Stress reduces expected credits by 20% and increases debits by 15%; upside increases credits by 10% and reduces debits by 5%.
- **Currencies:** the UI groups balances by currency and does not calculate a misleading cross-currency total.
- **Authentication:** HTTP Basic is a deliberately simple local-demo mechanism. Use OIDC/OAuth2, managed secrets, TLS, tenant isolation, rate limiting, and an audited authorization model before any real deployment.
- **Scope:** FX conversion, payment initiation, bank connectivity, maker-checker approval, and regulatory reporting are intentionally out of scope.

## Repository layout

- `backend/` — Spring Boot API, domain services, migrations, unit and integration tests.
- `frontend/` — React + TypeScript treasury workspace.
- `docs/` — architecture, API, threat model, and operations runbook.
- `.github/workflows/ci.yml` — build/test/container CI for this project when used as a repository root.

## License

MIT. See `LICENSE`.
