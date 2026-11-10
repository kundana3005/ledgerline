import { useState } from 'react';
import { api, qs } from '../api/client';
import { useFetch } from '../hooks';
import { Alert, Badge, Card, DataTable, Pagination, Stat, dateTime, money } from '../components/ui';

const TYPES = ['DEPOSIT', 'WITHDRAWAL', 'TRANSFER'];
const FIELDS = {
  search: { label: 'Search', placeholder: 'Reference, account or action' },
  account: { label: 'Account number', placeholder: 'e.g. LL1000000001' },
  type: { label: 'Type', options: TYPES },
  status: { label: 'Status', options: ['COMPLETED', 'FAILED'] },
  from: { label: 'From', date: true },
  to: { label: 'To', date: true },
};

const ref = (open) => (r) => {
  const value = r.transactionReference || r.reference;
  return <button className="btn ghost small" onClick={() => open(value)}><code>{value.slice(0, 8)}</code></button>;
};

const TABS = {
  ledger: { label: 'Ledger entries', filters: ['search', 'account', 'type', 'from', 'to'], columns: (open) => [
    { key: 'id', label: 'Entry' },
    { key: 'createdAt', label: 'Date', render: (r) => dateTime(r.createdAt) },
    { key: 'transactionReference', label: 'Transaction', render: ref(open) },
    { key: 'accountNumber', label: 'Account' },
    { key: 'direction', label: 'Direction', render: (r) => <Badge value={r.direction} /> },
    { key: 'amount', label: 'Amount', render: (r) => money(r.amount) },
    { key: 'balanceAfter', label: 'Balance after', render: (r) => money(r.balanceAfter) },
  ] },
  transactions: { label: 'Transactions', filters: ['search', 'type', 'status', 'from', 'to'], columns: (open) => [
    { key: 'createdAt', label: 'Date', render: (r) => dateTime(r.createdAt) },
    { key: 'reference', label: 'Reference', render: ref(open) },
    { key: 'type', label: 'Type' },
    { key: 'amount', label: 'Amount', render: (r) => money(r.amount) },
    { key: 'sourceAccount', label: 'From', render: (r) => r.sourceAccount || '-' },
    { key: 'destinationAccount', label: 'To', render: (r) => r.destinationAccount || '-' },
    { key: 'initiatedBy', label: 'By' },
    { key: 'status', label: 'Status', render: (r) => <Badge value={r.status} /> },
  ] },
  'audit-logs': { label: 'Audit log', filters: ['search'], columns: () => [
    { key: 'id', label: '#' },
    { key: 'createdAt', label: 'Date', render: (r) => dateTime(r.createdAt) },
    { key: 'action', label: 'Action' },
    { key: 'actorId', label: 'Actor', render: (r) => r.actorId ?? '-' },
    { key: 'entityId', label: 'Entity', render: (r) => (r.entityId ? <code>{r.entityId.slice(0, 12)}</code> : '-') },
    { key: 'details', label: 'Details', render: (r) => <small>{r.details || '-'}</small> },
    { key: 'ipAddress', label: 'IP', render: (r) => r.ipAddress || '-' },
  ] },
};

function PagedList({ path, filters, columns, onDetail }) {
  const empty = Object.fromEntries(filters.map((f) => [f, '']));
  const [draft, setDraft] = useState(empty);
  const [applied, setApplied] = useState(empty);
  const [page, setPage] = useState(0);
  const { data, loading, error } = useFetch(
    () => api(`/api/auditor/${path}${qs({ ...applied, page, size: 15 })}`), [path, applied, page]);

  return (
    <>
      <form className="filters" onSubmit={(e) => { e.preventDefault(); setPage(0); setApplied(draft); }}>
        {filters.map((f) => {
          const def = FIELDS[f];
          return (
            <label key={f}>{def.label}
              {def.options ? (
                <select value={draft[f]} onChange={(e) => setDraft({ ...draft, [f]: e.target.value })}>
                  <option value="">All</option>{def.options.map((o) => <option key={o}>{o}</option>)}
                </select>
              ) : (
                <input type={def.date ? 'date' : 'text'} placeholder={def.placeholder} value={draft[f]}
                  onChange={(e) => setDraft({ ...draft, [f]: e.target.value })} />
              )}
            </label>
          );
        })}
        <div className="row end">
          <button className="btn primary">Apply</button>
          <button type="button" className="btn ghost" onClick={() => { setDraft(empty); setApplied(empty); setPage(0); }}>Clear</button>
        </div>
      </form>
      <Alert>{error}</Alert>
      <DataTable columns={columns(onDetail)} rows={data?.content} loading={loading} empty="No records match your filters." />
      <Pagination page={page} totalPages={data?.totalPages} onChange={setPage} />
    </>
  );
}

function TransactionDetail({ reference, onClose }) {
  const { data, loading, error } = useFetch(() => api(`/api/auditor/transactions/${reference}`), [reference]);
  const s = data?.summary;
  return (
    <div className="modal-back" role="dialog" aria-modal="true" onClick={onClose}>
      <div className="modal wide card" onClick={(e) => e.stopPropagation()}>
        <h2>Transaction details</h2>
        <Alert>{error}</Alert>
        {loading && <p className="muted">Loading...</p>}
        {s && (
          <>
            <dl className="details">
              <dt>Reference</dt><dd><code>{s.reference}</code></dd>
              <dt>Type</dt><dd>{s.type}</dd>
              <dt>Status</dt><dd><Badge value={s.status} /></dd>
              <dt>Amount</dt><dd>{money(s.amount)}</dd>
              <dt>From</dt><dd>{s.sourceAccount || '-'}</dd>
              <dt>To</dt><dd>{s.destinationAccount || '-'}</dd>
              <dt>Initiated by</dt><dd>{s.initiatedBy}</dd>
              <dt>Date</dt><dd>{dateTime(s.createdAt)}</dd>
            </dl>
            <DataTable rows={data.entries}
              columns={[
                { key: 'accountNumber', label: 'Account' },
                { key: 'direction', label: 'Direction', render: (r) => <Badge value={r.direction} /> },
                { key: 'amount', label: 'Amount', render: (r) => money(r.amount) },
                { key: 'balanceAfter', label: 'Balance after', render: (r) => money(r.balanceAfter) },
              ]} />
          </>
        )}
        <div className="row end"><button className="btn ghost" onClick={onClose}>Close</button></div>
      </div>
    </div>
  );
}

export default function Auditor() {
  const [tab, setTab] = useState('ledger');
  const [detail, setDetail] = useState(null);
  const sum = useFetch(() => api('/api/auditor/summary'), []);
  const s = sum.data;
  const t = TABS[tab];

  return (
    <>
      <h1 className="page-title">Audit dashboard <Badge value="READ-ONLY" /></h1>
      <Alert>{sum.error}</Alert>
      {s && (
        <div className="grid stats4">
          <Stat label="Total debits" value={money(s.totalDebits)} />
          <Stat label="Total credits" value={money(s.totalCredits)} />
          <Stat label="Ledger integrity" value={s.ledgerBalanced ? 'Balanced' : 'MISMATCH'} />
          <Stat label="Transactions" value={s.totalTransactions} hint={`${money(s.totalVolume)} volume`} />
        </div>
      )}
      <Card>
        <div className="tabs">
          {Object.entries(TABS).map(([k, v]) => (
            <button key={k} className={`tab ${tab === k ? 'active' : ''}`} onClick={() => setTab(k)}>{v.label}</button>
          ))}
        </div>
        <PagedList key={tab} path={tab} filters={t.filters} columns={t.columns} onDetail={setDetail} />
      </Card>
      {detail && <TransactionDetail reference={detail} onClose={() => setDetail(null)} />}
    </>
  );
}