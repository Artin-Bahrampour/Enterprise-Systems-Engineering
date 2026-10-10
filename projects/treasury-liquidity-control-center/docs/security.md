# Security review and threat model

## Implemented controls

- All API routes except basic health/info endpoints require authentication.
- Passwords are hashed with BCrypt in the in-memory demo user store; the supplied password is read from environment configuration.
- HTTP request DTOs are validated, monetary precision is constrained, and the database independently enforces key integrity constraints.
- Cash posting uses transactional row locks and idempotency uniqueness constraints.
- The frontend stores its Basic credential only in `sessionStorage`, not persistent local storage. This is still browser-accessible credential material and is suitable only for a controlled demo.
- Nginx adds basic browser security headers and suppresses its version banner.
- Containers run the API as a non-root user; the UI container runs as the Nginx user.
- Secrets are not committed in `.env`; `.env.example` contains conspicuously local-only values.

## Residual risks / production blockers

This is a portfolio project, not a production banking control plane.

1. HTTP Basic is not an appropriate long-term identity architecture. Use OIDC/OAuth2 with an enterprise identity provider, short-lived tokens, MFA, and explicit permission scopes.
2. The sample user store is in-memory and has one configured user. It is not multi-tenant and does not implement maker-checker approval.
3. TLS termination, secret-manager integration, network policy, database encryption/backups, key rotation, WAF/rate limiting, and centralized security monitoring must be supplied by deployment infrastructure.
4. Audit records are append-only by application convention, not protected against a database owner with direct write privileges. Production requires separated database roles, retention controls, and possibly external immutable audit storage.
5. The demo profile seeds synthetic data. Disable it in all non-demo environments.
6. Forecast output is an estimate; it must not be used as a payment authorization or solvency decision without validation, governance, and appropriate controls.

## Operational guidance

Never expose this Compose stack directly to the public internet. Change all `.env` credentials, restrict ports, terminate TLS, and review the residual-risk list before any shared deployment.
