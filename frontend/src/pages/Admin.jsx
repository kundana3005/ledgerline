import { useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useFetch } from '../hooks';
import { Alert, Badge, Card, DataTable, Stat, dateTime, money } from '../components/ui';

export default function Admin() {
  const { user } = useAuth();
  const stats = useFetch(() => api('/api/admin/stats'), []);
  const users = useFetch(() => api('/api/admin/users'), []);
  const accts = useFetch(() => api('/api/admin/accounts'), []);
  const [msg, setMsg] = useState({ type: 'error', text: '' });
  const [nu, setNu] = useState({ username: '', email: '', password: '', role: 'ACCOUNT_HOLDER' });
  const [na, setNa] = useState({ ownerId: '', type: 'CHECKING' });

  const run = async (fn, ok) => {
    try {
      await fn();
      setMsg({ type: 'success', text: ok });
      users.reload(); accts.reload(); stats.reload();
      return true;
    } catch (e) { setMsg({ type: 'error', text: e.message }); return false; }
  };

  const createUser = async (e) => {
    e.preventDefault();
    if (await run(() => api('/api/admin/users', { method: 'POST', body: nu }), 'User created.'))
      setNu({ username: '', email: '', password: '', role: 'ACCOUNT_HOLDER' });
  };
  const createAccount = async (e) => {
    e.preventDefault();
    if (!na.ownerId) return setMsg({ type: 'error', text: 'Select an account holder.' });
    await run(() => api('/api/admin/accounts', { method: 'POST', body: { ownerId: Number(na.ownerId), type: na.type } }), 'Account created.');
  };
  const holders = (users.data || []).filter((u) => u.role === 'ACCOUNT_HOLDER');
  const s = stats.data;

  return (
    <>
      <h1 className="page-title">Administration</h1>
      <Alert type={msg.type}>{msg.text}</Alert>
      <Alert>{stats.error}</Alert>

      {s && (
        <div className="grid stats4">
          <Stat label="Users" value={s.totalUsers} hint={`${s.accountHolders} holders, ${s.auditors} auditors, ${s.admins} admins`} />
          <Stat label="Customer accounts" value={s.customerAccounts} hint={money(s.totalCustomerBalance)} />
          <Stat label="Transactions" value={s.totalTransactions} hint={`${s.deposits} dep, ${s.withdrawals} wd, ${s.transfers} tr`} />
          <Stat label="Ledger check" value={s.ledgerBalanced ? 'Balanced' : 'MISMATCH'}
            hint={`Debits ${money(s.totalDebits)} / Credits ${money(s.totalCredits)}`} />
        </div>
      )}

      <Card title="Users">
        <DataTable loading={users.loading} rows={users.data}
          columns={[
            { key: 'id', label: 'ID' }, { key: 'username', label: 'Username' }, { key: 'email', label: 'Email' },
            { key: 'role', label: 'Role' },
            { key: 'enabled', label: 'Status', render: (u) => <Badge value={u.enabled ? 'ACTIVE' : 'DISABLED'} /> },
            { key: 'createdAt', label: 'Created', render: (u) => dateTime(u.createdAt) },
            { key: 'act', label: '', render: (u) => u.id === user.id ? null : (
              <button className="btn ghost small"
                onClick={() => run(() => api(`/api/admin/users/${u.id}/enabled`, { method: 'PUT', body: { enabled: !u.enabled } }),
                  u.enabled ? 'User disabled.' : 'User enabled.')}>
                {u.enabled ? 'Disable' : 'Enable'}</button>) },
          ]} />
        <form className="inline-form" onSubmit={createUser}>
          <input placeholder="Username" value={nu.username} onChange={(e) => setNu({ ...nu, username: e.target.value })} />
          <input placeholder="Email" value={nu.email} onChange={(e) => setNu({ ...nu, email: e.target.value })} />
          <input type="password" placeholder="Password (8+ chars)" value={nu.password} onChange={(e) => setNu({ ...nu, password: e.target.value })} />
          <select value={nu.role} onChange={(e) => setNu({ ...nu, role: e.target.value })}>
            <option value="ACCOUNT_HOLDER">Account holder</option><option value="AUDITOR">Auditor</option><option value="ADMIN">Admin</option>
          </select>
          <button className="btn primary">Add user</button>
        </form>
      </Card>

      <Card title="Accounts">
        <DataTable loading={accts.loading} rows={accts.data}
          columns={[
            { key: 'accountNumber', label: 'Account' }, { key: 'ownerUsername', label: 'Owner', render: (a) => a.ownerUsername || 'System' },
            { key: 'type', label: 'Type' }, { key: 'balance', label: 'Balance', render: (a) => money(a.balance) },
            { key: 'status', label: 'Status', render: (a) => <Badge value={a.status} /> },
            { key: 'act', label: 'Change status', render: (a) => a.type === 'SYSTEM' ? '-' : (
              <select value={a.status}
                onChange={(e) => run(() => api(`/api/admin/accounts/${a.id}/status`, { method: 'PUT', body: { status: e.target.value } }), 'Account status updated.')}>
                <option>ACTIVE</option><option>FROZEN</option><option>CLOSED</option>
              </select>) },
          ]} />
        <form className="inline-form" onSubmit={createAccount}>
          <select value={na.ownerId} onChange={(e) => setNa({ ...na, ownerId: e.target.value })}>
            <option value="">Select account holder</option>
            {holders.map((h) => <option key={h.id} value={h.id}>{h.username}</option>)}
          </select>
          <select value={na.type} onChange={(e) => setNa({ ...na, type: e.target.value })}>
            <option>CHECKING</option><option>SAVINGS</option>
          </select>
          <button className="btn primary">Open account</button>
        </form>
      </Card>
    </>
  );
}