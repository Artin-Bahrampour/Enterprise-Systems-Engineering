# Operations runbook

## Start and stop

```bash
cp .env.example .env
docker compose up --build --wait
docker compose ps
docker compose logs -f api
docker compose down
```

Persisted PostgreSQL data remains in the named `treasury_pgdata` volume after `docker compose down`. To intentionally remove the local demo database, run `docker compose down -v` (destructive).

## Health and metrics

- Frontend: `http://localhost:8088`
- API health through proxy: `http://localhost:8088/actuator/health`
- Prometheus metrics: `http://localhost:8088/actuator/prometheus` (requires Basic authentication)

## Troubleshooting

- **API does not become healthy:** inspect `docker compose logs api postgres`; verify DB credentials and wait for Flyway migrations.
- **401 on API requests:** confirm `TREASURY_USER` and `TREASURY_PASSWORD` in `.env`; sign out and sign in again.
- **403 or proxy issue:** confirm the frontend proxy routes `/api/` to the API container.
- **Forecasts are flat:** check whether there are historical movements in the previous 56 days. The demo profile seeds these on the first start of an empty database.
- **No demo rows after enabling demo:** the initializer runs only when no accounts exist. Existing database volumes are not overwritten.

## Backup reminder

For any non-disposable environment, define and test PostgreSQL backups and restore drills before accepting business data. This repository does not ship a backup scheduler or retention policy.
