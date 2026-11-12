1. Security Measures
        LedgerLine uses different security methods to protect user accounts, transactions, and financial data.
        - User Authentication: Uses JWT tokens to ensure only logged-in users can access protected APIs.
        - Role-Based Access: Gives different permissions to users, admins, and auditors.
        - Account Security: Prevents unauthorized users from accessing other users' accounts.
        - Registration Security: Prevents users from assigning themselves admin or auditor roles.
        - Duplicate Transaction Prevention: Uses idempotency keys to ensure the same transaction is not processed twice.
        - Transaction Validation: Checks transaction amounts and account balances before processing.
        - Overdraft Prevention: Prevents users from withdrawing or transferring more money than they have.
        - Concurrent Transaction Handling: Uses database locking to handle multiple transactions safely at the same time.
        - Ledger Protection: Prevents changes or deletion of existing ledger records.
        - Audit Logging: Maintains records of important user activities and transactions.
        - Password Security: Uses secure password reset tokens with limited validity.
        - Rate Limiting: Restricts repeated requests to reduce misuse and unauthorized access.
        - Data Privacy: Protects account details, statements, notifications, and other sensitive information.
2. Testing
        Automated tests are implemented to check whether the application works correctly and securely.

        The main testing areas include:
        - Login and authentication testing.
        - User registration and account approval testing.
        - Role and permission testing.
        - Deposit, withdrawal, and transfer testing.
        - Duplicate transaction testing.
        - Simultaneous transaction testing.
        - Account balance validation.
        - Invalid transaction amount testing.
        - Ledger and audit log protection testing.
        - Password reset testing.
        - Rate limiting testing.
        - Statement, analytics, and notification testing.
        Test Execution:

        Command: cd backend && .\mvnw.cmd test
        - Test Date: [Date]
        - Total Tests: [Number]
        - Failures: [Number]
        - Errors: [Number]
3. Issues Found and Fixed
        During development, some issues were identified and corrected.
        - An internal account detail was exposed through an incorrect error message. The error handling was corrected.
        - Database permission settings caused deposit operations to fail. The database permissions were updated.
        - A database migration permission issue prevented proper startup. The database configuration was corrected.
4. Current Limitations and Future Improvements
        Some features can be improved further:
        - Token Security: Move JWT storage from session storage to secure HTTP-only cookies.
        - Token Expiry: Add token revocation when a password is changed.
        - Email Service: Integrate email for password reset instead of displaying reset links in server logs.
        - Rate Limiting: Use Redis in deployment to support multiple application instances.
        - Database Security: Configure separate database roles for better access control.
        - Performance: Improve transaction handling to support more simultaneous transactions.
        - Failed Transactions: Provide better visibility of failed transactions through audit logs and notifications.
        - Demo Accounts: Remove predefined demo credentials before production deployment.