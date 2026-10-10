CREATE TABLE cash_accounts (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    bank_name VARCHAR(120) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    opening_balance NUMERIC(19,4) NOT NULL,
    current_balance NUMERIC(19,4) NOT NULL,
    overdraft_limit NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_account_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_account_overdraft_nonnegative CHECK (overdraft_limit >= 0)
);

CREATE TABLE cash_movements (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES cash_accounts(id),
    idempotency_key VARCHAR(120) NOT NULL,
    external_reference VARCHAR(120),
    direction VARCHAR(8) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    value_date DATE NOT NULL,
    description VARCHAR(240) NOT NULL,
    balance_after NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_movement_direction CHECK (direction IN ('CREDIT', 'DEBIT')),
    CONSTRAINT ck_movement_amount_positive CHECK (amount > 0),
    CONSTRAINT uq_movement_account_idempotency UNIQUE (account_id, idempotency_key)
);
CREATE INDEX ix_movement_account_date ON cash_movements(account_id, value_date DESC, created_at DESC);
CREATE INDEX ix_movement_created_at ON cash_movements(created_at DESC);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor VARCHAR(120) NOT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID NOT NULL,
    details TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_created_at ON audit_events(created_at DESC);
