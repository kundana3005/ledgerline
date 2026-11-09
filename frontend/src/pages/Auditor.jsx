import { useState } from 'react';
import { api, qs } from '../api/client';
import { useFetch } from '../hooks';
import { Alert, Badge, Card, DataTable, Pagination, Stat, dateTime, money } from '../components/ui';

const TABS = {
  ledger: { label: 'Ledger entries', columns: [
    { key: 'id', label: 'Entry' },
    { key: 'createdAt', label: 'Date', render: (r) => dateTime(r.createdAt) },
    { key: 'transactionReference', label: 'Transaction', render: (r) => <code>{r.transactionReference.slice(0, 8)}</code> },
    { key: 'accountNumber', label: 'Account' },
    { key: 'direction', label: 'Direction', render: (r) => <Badge value={r.direction} /> },
    { key: 'amount', label: 'Amount', render: (r) => money(r.amount) },
    { key: 'balanceAfter', label: 'Balance after', render: (r) => money(r.balanceAfter) },
  ] },
  'audit-logs': { label: 'Audit log', columns: [
    { key: 'id', label: '#' },
    { key: 'createdAt', label: 'Date', render: (r) => dateTime(r.createdAt) },
    { key: 'action', label: 'Action' },
    { key: 'actorId', label: 'Actor', render: (r) => r.actorId ?? '-' },
    { key: 'entityId', label: 'Entity', render: (r) => r.entityId ? <code>{r.entityId.slice(0, 12)}</code> : '-' },
    { key: 'details', label: 'Details', render: (r) => <small>{r.details || '-'}</small> },
    { key: 'ipAddress', label: 'IP', render: (r) => r.ipAddress || '-' },
  ] },
  transactions: { label: 'Transactions', columns: [
    { key: 'createdAt', label: 'Date', render: (r) => dateTime(r.createdAt) },
    { key: 'reference', label: 'Reference', render: (r) => <code>{r.reference.slice(0, 8)}</code> },
    { key: 'type', label: 'Type' },
    { key: 'amount', label: 'Amount', render: (r) => money(r.amount) },
    { key: 'sourceAccount', label: 'From', render: (r) => r.sourceAccount || '-' },
    { key: 'destinationAccount', label: 'To', render: (r) => r.destinationAccount || '-' },
    { key: 'initiatedBy', label: 'By' },
    { key: 'status', label: 'Status', render: (r) => <Badge value={r.status} /> },
  ] },
};

function PagedList({ path, columns }) {
  const [q, setQ] = useState('');
  const [applied, setApplied] = useState('');
  const [page, setPage] = useState(0);
  const { data, loading, error } = useFetch(
    () => api(`/api/auditor/${path}${qs({ search: applied, page, size: 15 })}`), [path, applied, page]);

  return (
    <>
      <form className="filters" onSubmit={(e) => { e.preventDefault(); setPage(0); setApplied(q); }}>
        <label>Search<input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Reference, account or action" /></label>
        <div className="row end"><button className="btn primary">Search</button></div>
      </form>
      <Alert>{error}</Alert>
      <DataTable columns={columns} rows={data?.content} loading={loading} />
      <Pagination page={page} totalPages={data?.totalPages} onChange={setPage} />
    </>
  );
}

export default function Auditor() {
  const [tab, setTab] = useState('ledger');
  const sum = useFetch(() => api('/api/auditor/summary'), []);
  const s = sum.data;

  return (
    <>
      <h1 className="page-title">Audit dashboard <Badge value="READ-ONLY" /></h1>
      <Alert>{sum.error}</Alert>
      {s && (
        <div className="grid stats4">
          <Stat label="Total debits" value={money(s.totalDebits)} />
          <Stat label="Total credits" value={money(s.totalCredits)} />
          <Stat label="Ledger integrity" value={s.ledgerBalanced ? 'Balanced' : 'MISMATCH'} />
          <Stat label="Transactions" value={s.totalTransactions} hint={money(s.totalVolume) + ' volume'} />
        </div>
      )}
      <Card>
        <div className="tabs">
          {Object.entries(TABS).map(([k, t]) => (
            <button key={k} className={`tab ${tab === k ? 'active' : ''}`} onClick={() => setTab(k)}>{t.label}</button>
          ))}
        </div>
        <PagedList key={tab} path={tab} columns={TABS[tab].columns} />
      </Card>
    </>
  );
}