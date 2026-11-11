import { Link } from 'react-router-dom';
import { ArrowDownToLine, ArrowUpFromLine, ArrowLeftRight } from 'lucide-react';
import { api } from '../api/client';
import { useFetch } from '../hooks';
import { Bars, Distribution } from '../components/Charts';
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
  const stats = useFetch(() => api('/api/analytics/me?months=6'), []);
  const a = stats.data;

  return (
    <>
      <h1 className="page-title">Dashboard</h1>
      <Alert>{accounts.error || recent.error || stats.error}</Alert>

      <div className="grid stats4">
        <Stat label="Current balance" value={money(a?.currentBalance ?? 0)} hint={`${accounts.data?.length ?? 0} accounts`} />
        <Stat label="Money received" value={money(a?.totalReceived ?? 0)} hint="Deposits and incoming transfers" />
        <Stat label="Money sent" value={money(a?.totalSent ?? 0)} hint="Withdrawals and outgoing transfers" />
        <div className="card quick">
          <span className="stat-label">Quick actions</span>
          <div className="quick-row">
            <Link className="btn primary" to="/deposit"><ArrowDownToLine size={16} /> Deposit</Link>
            <Link className="btn ghost" to="/withdraw"><ArrowUpFromLine size={16} /> Withdraw</Link>
            <Link className="btn ghost" to="/transfer"><ArrowLeftRight size={16} /> Transfer</Link>
          </div>
        </div>
      </div>

      <div className="grid two">
        <Card title="Monthly summary">
          <Bars data={a?.monthly} xKey="month" series={[{ key: 'received', name: 'Received' }, { key: 'sent', name: 'Sent' }]} />
        </Card>
        <Card title="Transaction types">
          <Distribution data={a?.typeDistribution} nameKey="type" valueKey="transactions" />
        </Card>
      </div>

      <Card title="Your accounts">
        <div className="grid accounts">
          {(accounts.data || []).map((x) => (
            <div key={x.id} className="acct">
              <div className="acct-top"><span>{x.type}</span><Badge value={x.status} /></div>
              <div className="acct-num">{x.accountNumber}</div>
              <div className="acct-bal">{money(x.balance)}</div>
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