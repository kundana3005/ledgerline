# PROMPTS.md: AI Interaction Log

Tool used: Claude (Anthropic)

---

## 1. 08 Nov 2026: Project plan and PostgreSQL setup
**Prompt:** Asked for a step-by-step plan to build LedgerLine (Spring Boot + React + PostgreSQL). I only had MySQL installed.
**AI response:** Explained that the spec requires PostgreSQL (NUMERIC type, triggers), that it can run beside MySQL on port 5432, and gave a build plan with a package structure.
**What I did / verified:** Installed PostgreSQL, created the `ledgerline` user and databases in psql, and confirmed the connection.

## 2. 08 Nov 2026: Creating the Maven backend
**Prompt:** Asked why `./mvnw` was not recognized and what to do when the backend folder is empty.
**AI response:** Explained that PowerShell needs `.\mvnw.cmd` and that the project had to be generated with Spring Initializr.
**What I did / verified:** Generated the project (Java 21, Maven, Spring Boot 4.1.2) and confirmed `pom.xml` and `src` existed.

## 3. 08 Nov 2026: Database schema and append-only audit trail (audit trail)
**Prompt:** Asked how to make ledger entries and audit logs impossible to edit or delete.
**AI response:** Suggested enforcing it in the database with PostgreSQL triggers that raise an exception on UPDATE, DELETE and TRUNCATE, with the application having no update/delete endpoints either. Also added CHECK constraints for no negative balances and a UNIQUE (initiated_by, idempotency_key) for duplicate protection.
**What I did / verified:** Added the schema as Flyway `V1__init.sql` and confirmed the tables were created at startup. Will test the triggers with automated tests.

## 4. 08 Nov 2026: JWT login and role-based access control (authorization)
**Prompt:** Asked how to implement login, JWT and role-based authorization for Admin, Account Holder and Auditor.
**AI response:** Noted that the jjwt library may conflict with Spring Boot 4 (Jackson 3) and suggested Spring's built-in OAuth2 resource server (Nimbus). Provided SecurityConfig with `hasRole` rules, BCrypt, a stateless session, and an auditor area that allows only GET and uses `denyAll()` for everything else.
**What I did / verified:** Replaced the jjwt dependencies with `spring-boot-starter-oauth2-resource-server`. Logged in as alice and received a token. Tested that no token gives 401.

## 5. 08 Nov 2026: Account ownership rule (authorization)
**Prompt:** Asked how to stop one user from viewing or using another user's accounts.
**AI response:** Suggested loading accounts only through the logged-in user's ID taken from the JWT subject (`findByOwnerIdOrderByIdAsc`), never from a client-supplied ID, and returning 404 rather than 403 for other people's accounts.
**What I did / verified:** Implemented `GET /api/accounts` so it returns only the caller's accounts. Alice sees her two accounts only.

## 6. 08 Nov 2026: Debugging startup and test errors
**Prompt:** Pasted errors for: port 8080 in use, PostgreSQL error 28P01 (password authentication failed), and a 401 when calling `/api/accounts`.
**AI response:** Moved the app to port 8081, showed how to reset the database user's password, and explained that I had copied a truncated token (ending in `...`) instead of using `$r.token`.
**What I did / verified:** Set `server.port: 8081`, reset the password with `ALTER USER`, and re-ran the login and accounts calls using the full token variable.

---
(Add new entries below as I continue: ledger service, idempotency, concurrency, audit logging.)