import { useState } from 'react';
import { api, qs } from '../api/client';
import { useFetch } from '../hooks';
import { Alert, Badge, Card, DataTable, Pagination, dateTime } from '../components/ui';

const refreshBell = () => window.dispatchEvent(new Event('ll-notifications'));

export default function Notifications() {
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [err, setErr] = useState('');
  const { data, loading, error, reload } = useFetch(
    () => api(`/api/notifications${qs({ unreadOnly: unreadOnly ? 'true' : '', page, size: 15 })}`), [unreadOnly, page]);

  const run = async (fn) => {
    setErr('');
    try { await fn(); reload(); refreshBell(); } catch (e) { setErr(e.message); }
  };

  return (
    <>
      <h1 className="page-title">Notifications</h1>
      <Card action={<div className="row">
        <label className="check"><input type="checkbox" checked={unreadOnly} onChange={(e) => { setUnreadOnly(e.target.checked); setPage(0); }} /> Unread only</label>
        <button className="btn ghost small" onClick={() => run(() => api('/api/notifications/read-all', { method: 'PUT' }))}>Mark all read</button>
      </div>}>
        <Alert>{error || err}</Alert>
        <DataTable loading={loading} rows={data?.content} empty="You have no notifications."
          columns={[
            { key: 'createdAt', label: 'Date', render: (n) => dateTime(n.createdAt) },
            { key: 'type', label: 'Type', render: (n) => <Badge value={n.read ? 'READ' : 'NEW'} /> },
            { key: 'message', label: 'Message', render: (n) => <span style={{ whiteSpace: 'normal' }}>{n.message}</span> },
            { key: 'act', label: '', render: (n) => n.read ? null : (
              <button className="btn ghost small" onClick={() => run(() => api(`/api/notifications/${n.id}/read`, { method: 'PUT' }))}>Mark read</button>) },
          ]} />
        <Pagination page={page} totalPages={data?.totalPages} onChange={setPage} />
      </Card>
    </>
  );
}