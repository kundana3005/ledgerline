import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Landmark } from 'lucide-react';
import { api } from '../api/client';
import { Alert } from '../components/ui';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [done, setDone] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (!email.trim()) return setError('Enter your email address.');
    setBusy(true); setError('');
    try { setDone((await api('/api/auth/forgot-password', { method: 'POST', body: { email: email.trim() }, auth: false })).message); }
    catch (err) { setError(err.message); } finally { setBusy(false); }
  };

  return (
    <div className="login-page">
      <form className="card login-card" onSubmit={submit} noValidate>
        <div className="login-brand"><Landmark size={28} /><h1>Reset password</h1></div>
        <Alert>{error}</Alert>
        {done ? <Alert type="success">{done}</Alert> : (
          <>
            <label>Email<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoFocus /></label>
            <button className="btn primary" disabled={busy}>{busy ? 'Sending...' : 'Send reset link'}</button>
          </>
        )}
        <p className="muted"><Link className="link" to="/login">Back to sign in</Link></p>
      </form>
    </div>
  );
}