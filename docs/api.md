# API contract

All `/api/**` routes require HTTP Basic authentication. JSON errors use RFC 9457-style Spring `ProblemDetail` responses.

## Accounts

### `GET /api/accounts`
Returns accounts ordered by creation time, newest first.

### `POST /api/accounts`
```json
{
  "name": "Operating NOK",
  "bankName": "Nordic Commercial Bank",
  "currency": "NOK",
  "openingBalance": 850000.0000,
  "overdraftLimit": 25000.0000
}
```
Returns `201 Created`. Currency must be three uppercase letters. Opening balance may not be below the negative overdraft limit.

## Cash movements

### `POST /api/accounts/{accountId}/movements`
Requires `Idempotency-Key` (1–120 characters).

```json
{
  "direction": "DEBIT",
  "amount": 1250.0000,
  "valueDate": "2026-10-09",
  "description": "Supplier invoice 48192",
  "externalReference": "ERP-48192"
}
```

- `201 Created`: new movement posted.
- `200 OK`: same idempotency key and same payload replayed; no second balance change.
- `409 Conflict`: key reused with a different payload or a database uniqueness conflict.
- `422 Unprocessable Entity`: movement breaches the overdraft limit.
- `400 Bad Request`: invalid input or future value date.

### `GET /api/accounts/{accountId}/movements?from=2026-01-01&to=2026-12-31`
Returns movements within the inclusive value-date range.

### `GET /api/movements/recent`
Returns up to 50 most recently created movements.

## Forecasts

### `GET /api/forecasts?days=30&scenario=BASE`
- `days`: 1–90, default 30.
- `scenario`: `BASE`, `STRESS`, or `UPSIDE`.

The response contains one point per account per forecast date. Balances are not aggregated across currencies.

## Audit

### `GET /api/audit?limit=50`
Returns up to 100 recent audit events. Events are written within the same transaction as account and movement changes.
