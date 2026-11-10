Tool Used: Claude (Anthropic)
Project: LedgerLine (React + Vite, Java 21 Spring Boot, PostgreSQL)
# 08 November 2026 – Project Setup
Asked Claude how to set up the LedgerLine project using PostgreSQL and Maven.

Claude explained how to install PostgreSQL, create the database, and generate a Spring Boot project.

Decision: Installed PostgreSQL along with MySQL, created the Spring Boot project, fixed the port conflict, and resolved the database password issue.
# 08 November 2026 – Login and Role-Based Access
Asked Claude to implement login and different access permissions for Admin, Account Holder, and Auditor.

Claude implemented password encryption, JWT authentication, and role-based access.

Decision: Used the JWT approach and tested login and access permissions.
# 08 November 2026 – Deposits, Withdrawals and Transfers
Asked Claude to implement money transfers, deposits, and withdrawals without duplicate payments or incorrect balances.

Claude implemented double-entry ledger entries, duplicate request prevention using idempotency keys, and account locking.

Decision: Used the suggested implementation and fixed a compilation error.
# 08 November 2026 – Audit Trail and Account Security
Asked Claude to prevent changes to transaction records and restrict users from accessing other people's accounts.

Claude implemented database triggers, ownership checks, and audit logging.

Decision: Applied the changes and tested whether transaction records could be updated or deleted.
# 08 November 2026 – Admin and Auditor Features
Asked Claude to implement transaction history, account statements, admin APIs, and auditor access.

Claude implemented read-only access for auditors, user and account management for admins, and ledger balance verification.

Decision: Kept admins restricted from performing financial transactions.
# 08 November 2026 – Frontend Development
Asked Claude to build the frontend for Admin, Account Holder, and Auditor.

Claude created a React application with login, role-based pages, transaction forms, and dashboards.

Decision: Used Fetch API for backend communication and frontend route protection.
# 09 November 2026 – Automated Testing
Asked Claude to create tests for login security, duplicate transactions, overdrafts, simultaneous withdrawals, and ledger security.

Claude created four test classes using a separate test database.

Decision: Fixed compilation errors and database schema issues found during testing.
# 09 November 2026 – Bug Fixing
Asked Claude why the system account security test was failing.

Claude identified an issue in the validation order and suggested a fix.

Decision: Fixed the application logic and documented the issue in THREATS.md.
# 09 November 2026 – GitHub and Deployment
Asked Claude how to upload the project to GitHub and prepare it for deployment.

Claude provided Git commands, security improvements, environment configuration, Dockerfile setup, and deployment instructions for Render and Vercel.

Decision: Updated .gitignore and configured environment variables for deployment.

Testing Status: To be updated after running mvnw test.
# 10 November 2026 – Planning Additional Features
Asked Claude to check the existing project and suggest improvements before adding new features.

The suggested features included:
- User registration
- Account approval
- Redis rate limiting
- Password reset
- Notifications
- PDF and CSV statements
- Analytics dashboard
Claude provided a list of existing and missing features, database changes, an implementation plan, and questions about the new requirements.

Decision: To be updated after answering Claude's seven questions.