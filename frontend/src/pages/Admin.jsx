import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, qs } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useFetch } from '../hooks';
import ConfirmDialog from '../components/ConfirmDialog';
import { Alert, Badge, Card, DataTable, Pagination, Stat, dateTime, money } from '../components/ui';

function Overview() {
  const stats = useFetch(() => api('/api/admin/stats'), []);
  const recent = useFetch(() => api('/api/auditor/audit-logs?size=8'), []);
  const s = stats.data;
  return (
    <>
      <Alert>{stats.error}</Alert>
      {s && (
        <>
          <div className="grid stats4">
            <Stat label="Registered users" value={s.totalUsers} hint={`${s.accountHolders} holders, ${s.auditors} auditors, ${s.admins} admins`} />
            <Stat label="Active accounts" value={s.activeAccounts} hint={`${s.customerAccounts} customer accounts`} />
            <Stat label="Pending registrations" value={s.pendingRegistrations} />
            <Stat label="Pending account requests" value={s.pendingAccountRequests} />
          </div>
          <div className="grid stats4">
            <Stat label="Transactions" value={s.totalTransactions} hint={`${money(s.totalVolume)} volume`} />
            <Stat label="Deposits" value={s.deposits} />
            <Stat label="Withdrawals" value={s.withdrawals} />
            <Stat label="Transfers" value={s.transfers} />
          </div>
          <div className="grid stats4">
            <Stat label="Ledger check" value={s.ledgerBalanced ? 'Balanced' : 'MISMATCH'}
              hint={`Debits ${money(s.totalDebits)} / Credits ${money(s.totalCredits)}`} />
            <Stat label="Customer balances" value={money(s.totalCustomerBalance)} />
          </div>
        </>
      )}
      <Card title="Recent system activity" action={<Link to="/auditor" className="link">Ledger, transactions and audit logs</Link>}>
        <Alert>{recent.error}</Alert>
        <DataTable loading={recent.loading} rows={recent.data?.content} empty="No activity yet."
          columns={[
            { key: 'createdAt', label: 'Date', render: (r) => dateTime(r.createdAt) },
            { key: 'action', label: 'Action' },
            { key: 'actorId', label: 'Actor', render: (r) => r.actorId ?? '-' },
            { key: 'entityId', label: 'Entity', render: (r) => (r.entityId ? <code>{r.entityId.slice(0, 12)}</code> : '-') },
          ]} />
      </Card>
    </>
  );
}

function Requests() {
  const regs = useFetch(() => api(`/api/admin/users${qs({ status: 'PENDING_APPROVAL', role: 'ACCOUNT_HOLDER', size: 50 })}`), []);
  const reqs = useFetch(() => api('/api/admin/account-requests?status=PENDING&size=50'), []);
  const [dialog, setDialog] = useState(null);
  const [msg, setMsg] = useState({ type: 'success', text: '' });

  const confirm = async (reason) => {
    const { kind, row, approved } = dialog;
    const path = kind === 'user' ? `/api/admin/users/${row.id}/approval` : `/api/admin/account-requests/${row.id}/decision`;
    await api(path, { method: 'PUT', body: { approved, reason: approved ? null : reason } });
    setDialog(null);
    setMsg({ type: 'success', text: approved ? 'Approved.' : 'Rejected.' });
    regs.reload(); reqs.reload();
  };

  const buttons = (kind) => (row) => (
    <div className="row">
      <button className="btn primary small" onClick={() => setDialog({ kind, row, approved: true })}>Approve</button>
      <button className="btn ghost small" onClick={() => setDialog({ kind, row, approved: false })}>Reject</button>
    </div>
  );
  const who = dialog && (dialog.row.fullName || dialog.row.email);

  return (
    <>
      <Alert type={msg.type}>{msg.text}</Alert>
      <Card title="Pending registrations">
        <Alert>{regs.error}</Alert>
        <DataTable loading={regs.loading} rows={regs.data?.content} empty="No registrations are waiting."
          columns={[
            { key: 'createdAt', label: 'Registered', render: (u) => dateTime(u.createdAt) },
            { key: 'fullName', label: 'Name' }, { key: 'email', label: 'Email' }, { key: 'phone', label: 'Phone' },
            { key: 'act', label: '', render: buttons('user') },
          ]} />
      </Card>
      <Card title="Pending account requests">
        <Alert>{reqs.error}</Alert>
        <DataTable loading={reqs.loading} rows={reqs.data?.content} empty="No account requests are waiting."
          columns={[
            { key: 'createdAt', label: 'Submitted', render: (r) => dateTime(r.createdAt) },
            { key: 'fullName', label: 'Name' }, { key: 'email', label: 'Email' }, { key: 'type', label: 'Type' },
            { key: 'act', label: '', render: buttons('request') },
          ]} />
      </Card>
      {dialog && (
        <ConfirmDialog
          title={`${dialog.approved ? 'Approve' : 'Reject'} ${dialog.kind === 'user' ? 'registration' : 'account request'}?`}
          message={`${who}${dialog.kind === 'request' ? ` (${dialog.row.type})` : ''}`}
          confirmLabel={dialog.approved ? 'Approve' : 'Reject'} danger={!dialog.approved}
          askReason={!dialog.approved} onConfirm={confirm} onCancel={() => setDialog(null)} />
      )}
    </>
  );
}

function UsersTab() {
  const { user } = useAuth();
  const EMPTY = { search: '', role: '', status: '' };
  const [draft, setDraft] = useState(EMPTY);
  const [applied, setApplied] = useState(EMPTY);
  const [page, setPage] = useState(0);
  const users = useFetch(() => api(`/api/admin/users${qs({ ...applied, page, size: 10 })}`), [applied, page]);
  const [dialog, setDialog] = useState(null);
  const [msg, setMsg] = useState({ type: 'error', text: '' });
  const [nu, setNu] = useState({ username: '', email: '', password: '', role: 'AUDITOR' });

  const set = (k) => (e) => setDraft({ ...draft, [k]: e.target.value });
  const toggle = async () => {
    await api(`/api/admin/users/${dialog.id}/enabled`, { method: 'PUT', body: { enabled: !dialog.enabled } });
    setDialog(null); setMsg({ type: 'success', text: 'User updated.' }); users.reload();
  };
  const create = async (e) => {
    e.preventDefault();
    try {
      await api('/api/admin/users', { method: 'POST', body: nu });
      setMsg({ type: 'success', text: 'User created.' });
      setNu({ username: '', email: '', password: '', role: 'AUDITOR' }); users.reload();
    } catch (err) { setMsg({ type: 'error', text: err.message }); }
  };

  return (
    <Card title="Users">
      <Alert type={msg.type}>{msg.text}</Alert>
      <form className="filters" onSubmit={(e) => { e.preventDefault(); setPage(0); setApplied(draft); }}>
        <label>Search<input placeholder="Name, email or username" value={draft.search} onChange={set('search')} /></label>
        <label>Role
          <select value={draft.role} onChange={set('role')}>
            <option value="">All</option><option value="ACCOUNT_HOLDER">Account holder</option>
            <option value="AUDITOR">Auditor</option><option value="ADMIN">Admin</option>
          </select></label>
        <label>Status
          <select value={draft.status} onChange={set('status')}>
            <option value="">All</option><option value="PENDING_APPROVAL">Pending approval</option>
            <option value="ACTIVE">Active</option><option value="REJECTED">Rejected</option>
            <option value="DISABLED">Disabled</option>
          </select></label>
        <div className="row end">
          <button className="btn primary">Apply</button>
          <button type="button" className="btn ghost" onClick={() => { setDraft(EMPTY); setApplied(EMPTY); setPage(0); }}>Clear</button>
        </div>
      </form>
      <Alert>{users.error}</Alert>
      <DataTable loading={users.loading} rows={users.data?.content} empty="No users match your filters."
        columns={[
          { key: 'fullName', label: 'Name', render: (u) => u.fullName || u.username },
          { key: 'email', label: 'Email' }, { key: 'role', label: 'Role' },
          { key: 'status', label: 'Status', render: (u) => <Badge value={u.enabled ? u.status : 'DISABLED'} /> },
          { key: 'createdAt', label: 'Created', render: (u) => dateTime(u.createdAt) },
          { key: 'act', label: '', render: (u) => (u.id === user.id ? null : (
            <button className="btn ghost small" onClick={() => setDialog(u)}>{u.enabled ? 'Suspend' : 'Activate'}</button>)) },
        ]} />
      <Pagination page={page} totalPages={users.data?.totalPages} onChange={setPage} />

      <form className="inline-form" onSubmit={create}>
        <input placeholder="Username" value={nu.username} onChange={(e) => setNu({ ...nu, username: e.target.value })} />
        <input placeholder="Email" value={nu.email} onChange={(e) => setNu({ ...nu, email: e.target.value })} />
        <input type="password" placeholder="Password (8+ chars)" value={nu.password} onChange={(e) => setNu({ ...nu, password: e.target.value })} />
        <select value={nu.role} onChange={(e) => setNu({ ...nu, role: e.target.value })}>
          <option value="AUDITOR">Auditor</option><option value="ADMIN">Admin</option><option value="ACCOUNT_HOLDER">Account holder</option>
        </select>
        <button className="btn primary">Add staff user</button>
      </form>

      {dialog && (
        <ConfirmDialog title={`${dialog.enabled ? 'Suspend' : 'Activate'} this user?`}
          message={dialog.email} confirmLabel={dialog.enabled ? 'Suspend' : 'Activate'} danger={dialog.enabled}
          onConfirm={toggle} onCancel={() => setDialog(null)} />
      )}
    </Card>
  );
}

function AccountsTab() {
  const accts = useFetch(() => api('/api/admin/accounts'), []);
  const holders = useFetch(() => api('/api/admin/users?role=ACCOUNT_HOLDER&status=ACTIVE&size=100'), []);
  const [na, setNa] = useState({ ownerId: '', type: 'CHECKING' });
  const [dialog, setDialog] = useState(null);
  const [msg, setMsg] = useState({ type: 'error', text: '' });

  const create = async (e) => {
    e.preventDefault();
    if (!na.ownerId) return setMsg({ type: 'error', text: 'Select an account holder.' });
    try {
      await api('/api/admin/accounts', { method: 'POST', body: { ownerId: Number(na.ownerId), type: na.type } });
      setMsg({ type: 'success', text: 'Account created.' }); accts.reload();
    } catch (err) { setMsg({ type: 'error', text: err.message }); }
  };
  const changeStatus = async () => {
    await api(`/api/admin/accounts/${dialog.account.id}/status`, { method: 'PUT', body: { status: dialog.status } });
    setDialog(null); setMsg({ type: 'success', text: 'Account status updated.' }); accts.reload();
  };

  return (
    <Card title="Accounts">
      <Alert type={msg.type}>{msg.text}</Alert>
      <Alert>{accts.error}</Alert>
      <DataTable loading={accts.loading} rows={accts.data}
        columns={[
          { key: 'accountNumber', label: 'Account' },
          { key: 'ownerUsername', label: 'Owner', render: (a) => a.ownerUsername || 'System' },
          { key: 'type', label: 'Type' }, { key: 'balance', label: 'Balance', render: (a) => money(a.balance) },
          { key: 'status', label: 'Status', render: (a) => <Badge value={a.status} /> },
          { key: 'act', label: 'Change status', render: (a) => (a.type === 'SYSTEM' ? '-' : (
            <select value={a.status} onChange={(e) => e.target.value !== a.status && setDialog({ account: a, status: e.target.value })}>
              <option>ACTIVE</option><option>FROZEN</option><option>CLOSED</option>
            </select>)) },
        ]} />
      <form className="inline-form" onSubmit={create}>
        <select value={na.ownerId} onChange={(e) => setNa({ ...na, ownerId: e.target.value })}>
          <option value="">Select account holder</option>
          {(holders.data?.content || []).map((h) => <option key={h.id} value={h.id}>{h.fullName || h.username} ({h.email})</option>)}
        </select>
        <select value={na.type} onChange={(e) => setNa({ ...na, type: e.target.value })}>
          <option>CHECKING</option><option>SAVINGS</option>
        </select>
        <button className="btn primary">Open account</button>
      </form>
      {dialog && (
        <ConfirmDialog title="Change account status?"
          message={`${dialog.account.accountNumber}: ${dialog.account.status} to ${dialog.status}`}
          confirmLabel="Change status" danger={dialog.status !== 'ACTIVE'}
          onConfirm={changeStatus} onCancel={() => setDialog(null)} />
      )}
    </Card>
  );
}

const TABS = { overview: ['Overview', Overview], requests: ['Requests', Requests], users: ['Users', UsersTab], accounts: ['Accounts', AccountsTab] };

export default function Admin() {
  const [tab, setTab] = useState('overview');
  const Active = TABS[tab][1];
  return (
    <>
      <h1 className="page-title">Administration</h1>
      <div className="tabs">
        {Object.entries(TABS).map(([k, [label]]) => (
          <button key={k} className={`tab ${tab === k ? 'active' : ''}`} onClick={() => setTab(k)}>{label}</button>
        ))}
      </div>
      <Active />
    </>
  );
}