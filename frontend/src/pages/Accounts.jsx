import { useState } from 'react';
import { api, qs } from '../api/client';
import { useFetch } from '../hooks';
import { Alert, Badge, Card, DataTable, Stat, dateTime, money } from '../components/ui';

function Statement({ account }) {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const { data, loading, error } = useFetch(
    () => api(`/api/accounts/${account.id}/statement${qs({ from, to })}`), [account.id, from, to]);

  return (
    <Card title={`Statement: ${account.accountNumber}`}>
      <div className="filters">
        <label>From <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
        <label>To <input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></label>
      </div>
      <Alert>{error}</Alert>
      {data && (
        <>
          <p className="muted">Period {data.from} to {data.to} (UTC days)</p>
          <div className="grid stats4">
            <Stat label="Opening" value={money(data.openingBalance)} />
            <Stat label="Credits" value={money(data.totalCredits)} />
            <Stat label="Debits" value={money(data.totalDebits)} />
            <Stat label="Closing" value={money(data.closingBalance)} />
          </div>
          <DataTable loading={loading} rows={data.entries} empty="No activity in this period."
            columns={[
              { key: 'date', label: 'Date', render: (r) => dateTime(r.date) },
              { key: 'type', label: 'Type' },
              { key: 'reference', label: 'Reference', render: (r) => <code>{r.reference.slice(0, 8)}</code> },
              { key: 'direction', label: 'Direction', render: (r) => <Badge value={r.direction} /> },
              { key: 'amount', label: 'Amount', render: (r) => money(r.amount) },
              { key: 'balanceAfter', label: 'Balance after', render: (r) => money(r.balanceAfter) },
            ]} />
        </>
      )}
    </Card>
  );
}

export default function Accounts() {
  const { data, loading, error } = useFetch(() => api('/api/accounts'), []);
  const [selected, setSelected] = useState(null);

  return (
    <>
      <h1 className="page-title">Accounts</h1>
      <Alert>{error}</Alert>
      <Card>
        <DataTable loading={loading} rows={data} empty="You have no accounts yet. Ask an administrator to open one."
          columns={[
            { key: 'accountNumber', label: 'Account number' },
            { key: 'type', label: 'Type' },
            { key: 'balance', label: 'Balance', render: (a) => money(a.balance) },
            { key: 'status', label: 'Status', render: (a) => <Badge value={a.status} /> },
            { key: 'act', label: '', render: (a) => (
              <button className="btn ghost small" onClick={() => setSelected(a)}>Statement</button>) },
          ]} />
      </Card>
      {selected && <Statement account={selected} />}
    </>
  );
}