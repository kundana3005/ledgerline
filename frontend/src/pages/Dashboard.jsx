import { Link } from 'react-router-dom';
import { ArrowDownToLine, ArrowUpFromLine, ArrowLeftRight } from 'lucide-react';
import { api } from '../api/client';
import { useFetch } from '../hooks';
import { Alert, Badge, Card, DataTable, Stat, dateTime, money } from '../components/ui';

export const txColumns = [
  { key: 'createdAt', label: 'Date', render: (t) => dateTime(t.createdAt) },
  { key: 'reference', label: 'Reference', render: (t) => <code>{t.reference.slice(0, 8)}</code> },
  { key: 'type', label: 'Type' },
  { key: 'amount', label: 'Amount', render: (t) => money(t.amount) },
  { key: 'from', label: 'From', render: (t) => t.sourceAccount || '-' },
  { key: 'to', label: 'To', render: (t) => t.destinationAccount || '-' },
  { key: 'status', label: 'Status', render: (t) => <Badge value={t.status} /> },
];

export default function Dashboard() {
  const accounts = useFetch(() => api('/api/accounts'), []);
  const recent = useFetch(() => api('/api/transactions?size=5'), []);
  const total = (accounts.data || []).reduce((s, a) => s + Number(a.balance), 0);

  return (
    <>
      <h1 className="page-title">Dashboard</h1>
      <Alert>{accounts.error || recent.error}</Alert>

      <div className="grid stats">
        <Stat label="Total balance" value={money(total)} hint={`${accounts.data?.length ?? 0} accounts`} />
        <div className="card quick">
          <span className="stat-label">Quick actions</span>
          <div className="quick-row">
            <Link className="btn primary" to="/deposit"><ArrowDownToLine size={16} /> Deposit</Link>
            <Link className="btn ghost" to="/withdraw"><ArrowUpFromLine size={16} /> Withdraw</Link>
            <Link className="btn ghost" to="/transfer"><ArrowLeftRight size={16} /> Transfer</Link>
          </div>
        </div>
      </div>

      <Card title="Your accounts">
        <div className="grid accounts">
          {(accounts.data || []).map((a) => (
            <div key={a.id} className="acct">
              <div className="acct-top"><span>{a.type}</span><Badge value={a.status} /></div>
              <div className="acct-num">{a.accountNumber}</div>
              <div className="acct-bal">{money(a.balance)}</div>
            </div>
          ))}
        </div>
      </Card>

      <Card title="Recent transactions" action={<Link to="/history" className="link">View all</Link>}>
        <DataTable columns={txColumns} rows={recent.data?.content} loading={recent.loading}
          empty="No transactions yet. Make a deposit to get started." />
      </Card>
    </>
  );
}