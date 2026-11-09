import { useState } from 'react';
import { api, qs } from '../api/client';
import { useFetch } from '../hooks';
import { Alert, Card, DataTable, Pagination } from '../components/ui';
import { txColumns } from './Dashboard';

const EMPTY = { search: '', type: '', status: '', from: '', to: '' };

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
          <label>Search<input placeholder="Reference or account number" value={draft.search} onChange={set('search')} /></label>
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
          <div className="row end">
            <button className="btn primary">Apply</button>
            <button type="button" className="btn ghost" onClick={clear}>Clear</button>
          </div>
        </form>
        <Alert>{error}</Alert>
        <DataTable columns={txColumns} rows={data?.content} loading={loading} empty="No transactions match your filters." />
        <Pagination page={page} totalPages={data?.totalPages} onChange={setPage} />
      </Card>
    </>
  );
}