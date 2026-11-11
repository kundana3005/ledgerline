import { useState } from 'react';
import { api } from '../api/client';
import { Alert, Card } from '../components/ui';
import { STRONG } from './ResetPassword';

export default function ChangePassword() {
  const [f, setF] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [msg, setMsg] = useState({ type: 'error', text: '' });
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => { setF({ ...f, [k]: e.target.value }); setMsg({ type: 'error', text: '' }); };

  const submit = async (e) => {
    e.preventDefault();
    if (!f.currentPassword) return setMsg({ type: 'error', text: 'Enter your current password.' });
    if (!STRONG.test(f.newPassword)) return setMsg({ type: 'error', text: 'New password needs 8+ characters with upper and lower case letters, a digit and a symbol.' });
    if (f.newPassword !== f.confirmPassword) return setMsg({ type: 'error', text: 'Passwords do not match.' });
    setBusy(true);
    try {
      await api('/api/auth/change-password', { method: 'POST', body: f });
      setMsg({ type: 'success', text: 'Password changed.' });
      setF({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) { setMsg({ type: 'error', text: err.message }); } finally { setBusy(false); }
  };

  return (
    <>
      <h1 className="page-title">Change password</h1>
      <Card className="narrow">
        <Alert type={msg.type}>{msg.text}</Alert>
        <form className="form" onSubmit={submit} noValidate>
          <label>Current password<input type="password" value={f.currentPassword} onChange={set('currentPassword')} autoComplete="current-password" /></label>
          <label>New password<input type="password" value={f.newPassword} onChange={set('newPassword')} autoComplete="new-password" /></label>
          <label>Confirm new password<input type="password" value={f.confirmPassword} onChange={set('confirmPassword')} autoComplete="new-password" /></label>
          <button className="btn primary" disabled={busy}>{busy ? 'Saving...' : 'Change password'}</button>
        </form>
      </Card>
    </>
  );
}