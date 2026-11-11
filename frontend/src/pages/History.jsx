import { useState } from 'react';
import { api, qs } from '../api/client';
import { useFetch } from '../hooks';
import { Alert, Card, DataTable, Pagination } from '../components/ui';
import { txColumns } from './Dashboard';

const EMPTY = { search: '', id: '', type: '', status: '', from: '', to: '', minAmount: '', maxAmount: '', sort: 'id', dir: 'desc' };

export default function History() {
  const [draft, setDraft] = useState(EMPTY);
  const [applied, setApplied] = useState(EMPTY);
  const [page, setPage] = useState(0);
  const { data, loading, error } = useFetch(
    () => api(`/api/transactions${qs({ ...applied, page, size: 15 })}`), [applied, page]);

  const set = (k) => (e) => setDraft({ ...draft, [k]: e.target.value });
  const apply = (e) => { e.preventDefault(); setPage(0); setApplied(draft); };
  const clear = () => { setDraft(EMPTY); setApplied(EMPTY); setPage(0); };

  return (
    <>
      <h1 className="page-title">Transaction history</h1>
      <Card>
        <form className="filters" onSubmit={apply}>
          <label>Reference or account<input value={draft.search} onChange={set('search')} /></label>
          <label>Transaction ID<input inputMode="numeric" value={draft.id} onChange={set('id')} /></label>
          <label>Type
            <select value={draft.type} onChange={set('type')}>
              <option value="">All</option><option>DEPOSIT</option><option>WITHDRAWAL</option><option>TRANSFER</option>
            </select></label>
          <label>Status
            <select value={draft.status} onChange={set('status')}>
              <option value="">All</option><option>COMPLETED</option><option>FAILED</option>
            </select></label>
          <label>From<input type="date" value={draft.from} onChange={set('from')} /></label>
          <label>To<input type="date" value={draft.to} onChange={set('to')} /></label>
          <label>Min amount<input inputMode="decimal" value={draft.minAmount} onChange={set('minAmount')} /></label>
          <label>Max amount<input inputMode="decimal" value={draft.maxAmount} onChange={set('maxAmount')} /></label>
          <label>Sort by
            <select value={draft.sort} onChange={set('sort')}>
              <option value="id">Newest</option><option value="createdAt">Date</option>
              <option value="amount">Amount</option><option value="type">Type</option>
            </select></label>
          <label>Order
            <select value={draft.dir} onChange={set('dir')}><option value="desc">Descending</option><option value="asc">Ascending</option></select></label>
          <div className="row end">
            <button className="btn primary">Apply</button>
            <button type="button" className="btn ghost" onClick={clear}>Clear</button>
          </div>
        </form>
        <p className="muted">Failed attempts are not stored as transactions. They appear in your notifications.</p>
        <Alert>{error}</Alert>
        <DataTable columns={txColumns} rows={data?.content} loading={loading} empty="No transactions match your filters." />
        <Pagination page={page} totalPages={data?.totalPages} onChange={setPage} />
      </Card>
    </>
  );
}