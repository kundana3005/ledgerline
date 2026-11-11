import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { Landmark } from 'lucide-react';
import { useAuth, homeFor } from '../auth/AuthContext';
import { Alert } from '../components/ui';

export default function Login() {
  const { user, login } = useAuth();
  const nav = useNavigate();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={homeFor(user)} replace />;

  const submit = async (e) => {
    e.preventDefault();
    if (!identifier.trim() || !password) { setError('Enter your username or email and your password.'); return; }
    setBusy(true); setError('');
    try {
      const u = await login(identifier.trim(), password);
      nav(homeFor(u), { replace: true });
    } catch (err) {
      setError(err.status === 401 ? 'Invalid username or password.' : err.message);
    } finally { setBusy(false); }
  };

  return (
    <div className="login-page">
      <form className="card login-card" onSubmit={submit} noValidate>
        <div className="login-brand"><Landmark size={28} /><h1>Balance Vault</h1></div>
        <p className="muted">Secure money movement with a ledger you can prove.</p>
        <Alert>{error}</Alert>
        <label>Username or email
          <input value={identifier} onChange={(e) => setIdentifier(e.target.value)} autoComplete="username" autoFocus />
        </label>
        <label>Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
        </label>
        <button className="btn primary" disabled={busy}>{busy ? 'Signing in...' : 'Sign in'}</button>
        <p className="muted"><Link className="link" to="/forgot-password">Forgot password?</Link></p>
        <p className="muted">New here? <Link className="link" to="/register">Create an account</Link></p>
        <div className="demo">
          <strong>Demo logins</strong>
          <span>alice / Alice@123 (account holder)</span>
          <span>admin / Admin@123 &middot; auditor / Auditor@123</span>
        </div>
      </form>
    </div>
  );
}