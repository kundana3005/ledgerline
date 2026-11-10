import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { Landmark } from 'lucide-react';
import { api } from '../api/client';
import { useAuth, homeFor } from '../auth/AuthContext';
import { Alert } from '../components/ui';

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE = /^\+?[0-9]{10,15}$/;
const STRONG = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,72}$/;
const EMPTY = { fullName: '', email: '', phone: '', password: '', confirmPassword: '' };

export default function Register() {
  const { user } = useAuth();
  const [f, setF] = useState(EMPTY);
  const [error, setError] = useState('');
  const [done, setDone] = useState(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={homeFor(user)} replace />;
  const set = (k) => (e) => { setF({ ...f, [k]: e.target.value }); setError(''); };

  const validate = () => {
    if (f.fullName.trim().length < 2) return 'Enter your full name.';
    if (!EMAIL.test(f.email.trim())) return 'Enter a valid email address.';
    if (!PHONE.test(f.phone.replace(/[\s-]/g, ''))) return 'Enter a phone number with 10-15 digits.';
    if (!STRONG.test(f.password)) return 'Password needs 8+ characters with upper and lower case letters, a digit and a symbol.';
    if (f.password !== f.confirmPassword) return 'Passwords do not match.';
    return '';
  };

  const submit = async (e) => {
    e.preventDefault();
    const problem = validate();
    if (problem) return setError(problem);
    setBusy(true); setError('');
    try {
      setDone(await api('/api/auth/register', {
        method: 'POST', auth: false,
        body: { ...f, fullName: f.fullName.trim(), email: f.email.trim(), phone: f.phone.replace(/[\s-]/g, '') },
      }));
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  };

  if (done) {
    return (
      <div className="login-page">
        <div className="card login-card">
          <div className="login-brand"><Landmark size={28} /><h1>Application received</h1></div>
          <Alert type="success">{done.message}</Alert>
          <p className="muted">Sign in with {done.email} to follow the status of your application.</p>
          <Link className="btn primary" to="/login">Go to sign in</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="login-page">
      <form className="card login-card" onSubmit={submit} noValidate>
        <div className="login-brand"><Landmark size={28} /><h1>Create your account</h1></div>
        <Alert>{error}</Alert>
        <label>Full name<input value={f.fullName} onChange={set('fullName')} autoComplete="name" /></label>
        <label>Email<input type="email" value={f.email} onChange={set('email')} autoComplete="email" /></label>
        <label>Phone<input value={f.phone} onChange={set('phone')} placeholder="+919876543210" autoComplete="tel" /></label>
        <label>Password<input type="password" value={f.password} onChange={set('password')} autoComplete="new-password" /></label>
        <label>Confirm password<input type="password" value={f.confirmPassword} onChange={set('confirmPassword')} autoComplete="new-password" /></label>
        <button className="btn primary" disabled={busy}>{busy ? 'Submitting...' : 'Register'}</button>
        <p className="muted">Already registered? <Link className="link" to="/login">Sign in</Link></p>
      </form>
    </div>
  );
}