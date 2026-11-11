-- Run ONCE as the postgres superuser, with the backend stopped:  psql -U postgres -f docs/db-roles.sql
-- Edit the password first. Do not commit the edited file.
CREATE ROLE ledgerline_migrator LOGIN PASSWORD 'Ledgerline@1234';
\c ledgerline
ALTER DATABASE ledgerline OWNER TO ledgerline_migrator;
ALTER SCHEMA public OWNER TO ledgerline_migrator;
DO $$ DECLARE r record; BEGIN
  FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP
    EXECUTE format('ALTER TABLE public.%I OWNER TO ledgerline_migrator', r.tablename); END LOOP;
  FOR r IN SELECT sequencename FROM pg_sequences WHERE schemaname = 'public' LOOP
    EXECUTE format('ALTER SEQUENCE public.%I OWNER TO ledgerline_migrator', r.sequencename); END LOOP;
  FOR r IN SELECT p.oid::regprocedure AS f FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
           WHERE n.nspname = 'public' LOOP
    EXECUTE format('ALTER FUNCTION %s OWNER TO ledgerline_migrator', r.f); END LOOP;
END $$;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM ledgerline;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM ledgerline;
GRANT CONNECT ON DATABASE ledgerline TO ledgerline;
GRANT USAGE ON SCHEMA public TO ledgerline;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO ledgerline;
REVOKE UPDATE ON ledger_entries, audit_logs, transactions FROM ledgerline;   -- insert and read only
REVOKE ALL ON flyway_schema_history FROM ledgerline;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ledgerline;
ALTER DEFAULT PRIVILEGES FOR ROLE ledgerline_migrator IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO ledgerline;
ALTER DEFAULT PRIVILEGES FOR ROLE ledgerline_migrator IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO ledgerline;