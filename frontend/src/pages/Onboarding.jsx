import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useFetch } from '../hooks';
import { Alert, Badge, Card, DataTable, dateTime } from '../components/ui';

const LABEL = {
  PENDING_APPROVAL: 'Your registration is waiting for administrator approval.',
  REJECTED: 'Your registration was not approved.',
  ACTIVE: 'Your registration is approved.',
};

export default function Onboarding() {
  const { user, setStatus } = useAuth();
  const { data, loading, error, reload } = useFetch(() => api('/api/onboarding/status'), []);
  const [type, setType] = useState('CHECKING');
  const [msg, setMsg] = useState({ type: 'error', text: '' });
  const [busy, setBusy] = useState(false);

  useEffect(() => { if (data && data.status !== user.status) setStatus(data.status); }, [data]); // eslint-disable-line

  const submit = async () => {
    setBusy(true); setMsg({ type: 'error', text: '' });
    try {
      await api('/api/onboarding/account-requests', { method: 'POST', body: { type } });
      setMsg({ type: 'success', text: 'Account request submitted. An administrator will review it.' });
      reload();
    } catch (e) { setMsg({ type: 'error', text: e.message }); } finally { setBusy(false); }
  };

  const hasPending = (data?.requests || []).some((r) => r.status === 'PENDING');

  return (
    <>
      <h1 className="page-title">My application</h1>
      <Alert>{error}</Alert>
      {loading && <p className="muted">Loading...</p>}
      {data && (
        <>
          <Card title="Registration status">
            <p><Badge value={data.status} /> {LABEL[data.status]}</p>
            {data.status === 'REJECTED' && data.rejectionReason && <Alert>Reason: {data.rejectionReason}</Alert>}
          </Card>

          {data.status === 'ACTIVE' && (
            <Card title="Open an account">
              <Alert type={msg.type}>{msg.text}</Alert>
              <div className="inline-form">
                <select value={type} onChange={(e) => setType(e.target.value)} disabled={hasPending}>
                  <option value="CHECKING">Checking</option><option value="SAVINGS">Savings</option>
                </select>
                <button className="btn primary" disabled={busy || hasPending} onClick={submit}>
                  {hasPending ? 'Request pending' : 'Request account'}
                </button>
              </div>
              <DataTable rows={data.requests} empty="You have not requested an account yet."
                columns={[
                  { key: 'createdAt', label: 'Submitted', render: (r) => dateTime(r.createdAt) },
                  { key: 'type', label: 'Type' },
                  { key: 'status', label: 'Status', render: (r) => <Badge value={r.status} /> },
                  { key: 'rejectionReason', label: 'Reason', render: (r) => r.rejectionReason || '-' },
                ]} />
            </Card>
          )}
        </>
      )}
    </>
  );
}