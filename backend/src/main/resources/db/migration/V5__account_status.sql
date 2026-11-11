ALTER TABLE accounts DROP CONSTRAINT accounts_status_check;
UPDATE accounts SET status = 'SUSPENDED' WHERE status = 'FROZEN';
ALTER TABLE accounts ADD CONSTRAINT accounts_status_check
  CHECK (status IN ('PENDING','ACTIVE','SUSPENDED','CLOSED'));