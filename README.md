# LedgerLine – Secure Money Transfer System

LedgerLine is a financial transaction management application that allows users to deposit, withdraw, and transfer money securely. It prevents duplicate transactions, maintains accurate balances, and records every transaction in a ledger.

- **Live App:** [Your Vercel URL]
- **API Health Check:** [Your Render URL]/api/health
- **GitHub Repository:** [Your GitHub URL]

## 1. Problem It Solves

LedgerLine helps prevent common financial problems such as:
- Duplicate payments.
- Withdrawals exceeding the available balance.
- Unauthorized account access.
- Incorrect transaction records.
- Changes to previous transaction history.

It uses security checks, database locking, and transaction validation to handle these problems.

## 2. Technologies Used

- **Frontend:** React, Vite, React Router, Recharts
- **Backend:** Java 21, Spring Boot, Spring Security, JWT
- **Database:** PostgreSQL
- **Database Management:** Spring Data JPA, Flyway
- **Hosting:** Render and Vercel

## 3. User Roles

**Account Holder**
- View account balance and transaction history.
- Deposit, withdraw, and transfer money.
- Download account statements.
- Receive notifications.

**Administrator**
- Approve or reject user registrations.
- Manage users and accounts.
- View application statistics.

**Auditor**
- View transactions, ledger entries, and audit logs.
- Cannot modify financial records.

## 4. Main Features

1. User registration and admin approval.
2. Login with JWT authentication.
3. Role-based access control.
4. Deposits, withdrawals, and money transfers.
5. Duplicate transaction prevention using idempotency keys.
6. Account balance validation.
7. Safe handling of simultaneous transactions.
8. Double-entry ledger to maintain accurate records.
9. Protected transaction history and audit logs.
10. Password change and reset functionality.
11. In-app notifications.
12. Transaction search and filtering.
13. CSV and PDF account statements.
14. Dashboard with financial charts and statistics.
15. Rate limiting and suspicious activity monitoring.

## 5. Run the Project Locally

**Requirements:** Java 21, Node.js, PostgreSQL.

**Step 1: Create the databases**

```sql
CREATE USER ledgerline WITH PASSWORD 'ledgerline123';
CREATE DATABASE ledgerline OWNER ledgerline;
CREATE DATABASE ledgerline_test OWNER ledgerline;
```

**Step 2: Start the backend**

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**Step 3: Start the frontend**

```powershell
cd frontend
copy .env.example .env
npm install
npm run dev
```

Open `http://localhost:5173` in your browser.

## 6. Testing

Run the following command:

```powershell
cd backend
.\mvnw.cmd test
```

Tests cover:
- Authentication and authorization.
- Duplicate transaction prevention.
- Deposits, withdrawals, and transfers.
- Simultaneous transactions.
- Account balance validation.
- Ledger and audit log protection.
- Password reset and rate limiting.
- Statements, notifications, and analytics.

See **THREATS.md** for security details and test results.

## 7. Known Limitations

- Password reset currently uses server logs instead of email.
- Redis rate limiting is implemented but not enabled in deployment.
- JWT tokens are stored in session storage.
- Demo accounts use predefined passwords.
- Separate database roles are not currently enabled.
- The shared system cash account may limit performance under heavy traffic.

## 8. Project Documentation

- **THREATS.md:** Security threats, prevention methods, and test results.
- **PROMPTS.md:** Record of AI-assisted development work.

