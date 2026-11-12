## 10 Nov 2026: Registration and User Approval

What I asked: Add user registration, admin approval, and read-only auditor access to the existing project without rebuilding it.

What I got: Added a registration and approval plan with database updates, user status management, and admin-only approval APIs. Unapproved users receive limited access until approved.

My decision: Used email as the username and followed the account status-based approval system.

## 10 Nov 2026: Security Configuration Fix

What I asked: Fix the SecurityConfig file after making changes to access permissions.

What I got: Identified that an admin security rule had been replaced with a placeholder comment, which could allow unauthorized access to admin APIs.

My decision: Replaced the SecurityConfig file and checked the order of access rules.

## 11 Nov 2026: Rate Limiting and Notifications

What I asked: Add rate limiting, secure password reset, and in-app notifications.

What I got: Implemented memory-based and Redis-based rate limiting, secure password reset tokens, and notifications with masked account numbers.

My decision: Kept the in-memory rate limiter for deployment and retained Redis support for future use.

## 11 Nov 2026: Deposit Error Fix

What I asked: Find and fix the 500 error occurring during deposit operations.

What I got: Identified a database permission issue caused by restricting UPDATE access on the transactions table. Also identified a Flyway migration configuration issue.

My decision: Reverted to a single database owner role, corrected the Flyway configuration, and documented the issue for future reference.

## 12 Nov 2026: Application Deployment

What I asked: Deploy the backend, database, and frontend using Render and Vercel.

What I got: Set up Docker configuration, environment variables, database connection settings, and deployment steps for the backend and frontend.

My decision: Used Render for backend and database hosting and Vercel for frontend deployment. Fixed configuration and deployment issues during setup.