import { Link, Navigate } from 'react-router-dom';
import { Landmark, ShieldCheck, Scale, Copy, Lock, FileText, Bell } from 'lucide-react';
import { useAuth, homeFor } from '../auth/AuthContext';

const OBJECTIVES = [
  [Scale, 'Accurate balances', 'Every transaction writes matching debit and credit entries, so total debits always equal total credits.'],
  [Copy, 'No duplicate payments', 'Idempotency keys make sure a repeated or double-clicked request is processed only once.'],
  [Lock, 'Safe under concurrency', 'Row locking stops simultaneous withdrawals from overdrawing an account.'],
  [ShieldCheck, 'Authorized access only', 'Role-based access for administrators, account holders and auditors, enforced on the backend.'],
  [FileText, 'Traceable history', 'An append-only ledger and audit trail that cannot be edited or deleted through the application.'],
  [Bell, 'Clear visibility', 'Statements in PDF and CSV, notifications and dashboards built from real data.'],
];

export default function Landing() {
  const { user } = useAuth();
  if (user) return <Navigate to={homeFor(user)} replace />;

  return (
    <div className="landing">
      <header className="land-nav">
        <div className="brand dark"><Landmark size={22} /> LedgerLine</div>
        <div className="row">
          <Link className="btn ghost" to="/login">Sign in</Link>
          <Link className="btn primary" to="/register">Sign up</Link>
        </div>
      </header>

      <section className="hero">
        <h1>Secure money movement with a ledger you can prove.</h1>
        <p>LedgerLine is a financial management platform where every transaction is authorized,
          processed exactly once, recorded in a double-entry ledger and traceable through an audit trail.</p>
        <div className="row center">
          <Link className="btn primary big" to="/register">Create an account</Link>
          <Link className="btn ghost big" to="/login">Sign in</Link>
        </div>
      </section>

      <section className="land-section">
        <h2>Project objectives</h2>
        <div className="obj-grid">
          {OBJECTIVES.map(([Icon, title, text]) => (
            <div key={title} className="card obj">
              <Icon size={24} className="obj-icon" />
              <h3>{title}</h3>
              <p className="muted">{text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="land-section roles">
        <h2>Built for three kinds of users</h2>
        <div className="obj-grid three">
          <div className="card obj"><h3>Account holder</h3><p className="muted">View balances, deposit, withdraw, transfer, search history and download statements.</p></div>
          <div className="card obj"><h3>Administrator</h3><p className="muted">Approve registrations and account requests, manage users and accounts, monitor activity.</p></div>
          <div className="card obj"><h3>Auditor</h3><p className="muted">Read-only access to transactions, ledger entries and audit logs.</p></div>
        </div>
      </section>

      <footer className="land-foot muted">LedgerLine &middot; React, Spring Boot and PostgreSQL</footer>
    </div>
  );
}