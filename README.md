# ledgerline
Secure money movement with a ledger you can prove
## Additional features
Registration and admin approval, account requests, account statuses (ACTIVE, SUSPENDED, CLOSED), password
change and reset, in-app notifications, advanced transaction search, CSV and PDF statements, charts,
Redis rate limiting, and security monitoring.

### Configuration
RATE_LIMIT_STORE (memory|redis), REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, FRONTEND_URL, LOG_RESET_LINK,
MIGRATION_USER, MIGRATION_PASSWORD (Flyway uses a separate migration role when set).

### Database roles
`docs/db-roles.sql` (run once as postgres) creates `ledgerline_migrator` as owner and leaves the app role with
SELECT/INSERT/UPDATE only (ledger, audit and transactions: SELECT/INSERT only). The test database uses a single owner
role so the append-only trigger tests can run. On hosts with a single DB user this separation is not available.

### Known limits
- No email service: reset links are logged in development only.
- Failed transactions are not stored as transactions (they roll back); they are in the audit log and notifications.
- "Received/sent" analytics count deposits and withdrawals as well as transfers.
- Tokens stay valid until they expire (30 min) after a password change; disabled users are rejected immediately.
- Redis rate limiting fails open if Redis is unreachable, and is not covered by the automated tests (the memory store is).
- The PENDING account status exists but is not currently assigned by any workflow.