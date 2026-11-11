import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Landmark } from 'lucide-react';
import { api } from '../api/client';
import { Alert } from '../components/ui';

export const STRONG = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,72}$/;

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [done, setDone] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (!STRONG.test(password)) return setError('Password needs 8+ characters with upper and lower case letters, a digit and a symbol.');
    if (password !== confirmPassword) return setError('Passwords do not match.');
    setBusy(true); setError('');
    try { setDone((await api('/api/auth/reset-password', { method: 'POST', auth: false,
      body: { token, password, confirmPassword } })).message); }
    catch (err) { setError(err.message); } finally { setBusy(false); }
  };

  return (
    <div className="login-page">
      <form className="card login-card" onSubmit={submit} noValidate>
        <div className="login-brand"><Landmark size={28} /><h1>Choose a new password</h1></div>
        {!token && <Alert>This link is missing its token. Request a new one.</Alert>}
        <Alert>{error}</Alert>
        {done ? <Alert type="success">{done}</Alert> : (
          <>
            <label>New password<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" /></label>
            <label>Confirm password<input type="password" value={confirmPassword} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" /></label>
            <button className="btn primary" disabled={busy || !token}>{busy ? 'Saving...' : 'Update password'}</button>
          </>
        )}
        <p className="muted"><Link className="link" to="/login">Back to sign in</Link></p>
      </form>
    </div>
  );
}