CREATE TABLE account_requests (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('SAVINGS','CHECKING')),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
  reviewed_by BIGINT REFERENCES users(id),
  rejection_reason VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  reviewed_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX one_pending_request_per_user ON account_requests (user_id) WHERE status = 'PENDING';