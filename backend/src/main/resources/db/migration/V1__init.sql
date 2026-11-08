CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(120) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','ACCOUNT_HOLDER','AUDITOR')),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
  id BIGSERIAL PRIMARY KEY,
  account_number VARCHAR(20) NOT NULL UNIQUE,
  owner_id BIGINT REFERENCES users(id),
  type VARCHAR(20) NOT NULL CHECK (type IN ('SAVINGS','CHECKING','SYSTEM')),
  balance NUMERIC(19,4) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','FROZEN','CLOSED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  -- no negative balance, except the internal SYSTEM cash account
  CONSTRAINT balance_non_negative CHECK (type = 'SYSTEM' OR balance >= 0)
);

CREATE TABLE transactions (
  id BIGSERIAL PRIMARY KEY,
  reference VARCHAR(36) NOT NULL UNIQUE,
  type VARCHAR(20) NOT NULL CHECK (type IN ('DEPOSIT','WITHDRAWAL','TRANSFER')),
  status VARCHAR(20) NOT NULL CHECK (status IN ('COMPLETED','FAILED')),
  amount NUMERIC(19,4) NOT NULL CHECK (amount > 0),
  source_account_id BIGINT REFERENCES accounts(id),
  destination_account_id BIGINT REFERENCES accounts(id),
  initiated_by BIGINT NOT NULL REFERENCES users(id),
  idempotency_key VARCHAR(64) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (initiated_by, idempotency_key)      -- duplicate protection at DB level
);

CREATE TABLE ledger_entries (
  id BIGSERIAL PRIMARY KEY,
  transaction_id BIGINT NOT NULL REFERENCES transactions(id),
  account_id BIGINT NOT NULL REFERENCES accounts(id),
  direction VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
  amount NUMERIC(19,4) NOT NULL CHECK (amount > 0),
  balance_after NUMERIC(19,4) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
  id BIGSERIAL PRIMARY KEY,
  actor_id BIGINT,
  action VARCHAR(60) NOT NULL,
  entity_type VARCHAR(40),
  entity_id VARCHAR(40),
  details JSONB,
  ip_address VARCHAR(45),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- APPEND-ONLY: block UPDATE/DELETE/TRUNCATE even for the app user
CREATE FUNCTION forbid_modification() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER ledger_no_update BEFORE UPDATE OR DELETE ON ledger_entries
  FOR EACH ROW EXECUTE FUNCTION forbid_modification();
CREATE TRIGGER audit_no_update BEFORE UPDATE OR DELETE ON audit_logs
  FOR EACH ROW EXECUTE FUNCTION forbid_modification();
CREATE TRIGGER ledger_no_truncate BEFORE TRUNCATE ON ledger_entries
  FOR EACH STATEMENT EXECUTE FUNCTION forbid_modification();
CREATE TRIGGER audit_no_truncate BEFORE TRUNCATE ON audit_logs
  FOR EACH STATEMENT EXECUTE FUNCTION forbid_modification();

-- Internal cash account: the counterparty for deposits/withdrawals
INSERT INTO accounts (account_number, type, balance) VALUES ('SYSTEM-CASH', 'SYSTEM', 0);