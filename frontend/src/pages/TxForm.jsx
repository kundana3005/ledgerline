import { useRef, useState } from 'react';
import { api } from '../api/client';
import { useFetch } from '../hooks';
import { Alert, Badge, Card, money } from '../components/ui';

const CFG = {
  deposit: { title: 'Deposit money', path: '/api/transactions/deposit', verb: 'Deposit', accountLabel: 'Deposit into' },
  withdraw: { title: 'Withdraw money', path: '/api/transactions/withdraw', verb: 'Withdraw', accountLabel: 'Withdraw from' },
  transfer: { title: 'Transfer money', path: '/api/transactions/transfer', verb: 'Transfer', accountLabel: 'From account' },
};
const AMOUNT = /^\d{1,13}(\.\d{1,2})?$/;
const newKey = () => crypto.randomUUID();

export default function TxForm({ kind }) {
  const cfg = CFG[kind];
  const accounts = useFetch(() => api('/api/accounts'), []);
  const [account, setAccount] = useState('');
  const [dest, setDest] = useState('');
  const [amount, setAmount] = useState('');
  const [step, setStep] = useState('edit');
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  // One key per distinct request: retrying the SAME data reuses it, so it can never pay twice.
  const key = useRef(newKey());

  const active = (accounts.data || []).filter((a) => a.status === 'ACTIVE');
  const selected = account || active[0]?.accountNumber || '';
  const edit = (setter) => (e) => { setter(e.target.value); key.current = newKey(); setError(''); };

  const review = (e) => {
    e.preventDefault();
    if (!selected) return setError('You have no active account to use.');
    if (!AMOUNT.test(amount) || Number(amount) <= 0) return setError('Enter an amount greater than 0 with at most 2 decimals.');
    if (kind === 'transfer') {
      if (!dest.trim()) return setError('Enter the recipient account number.');
      if (dest.trim() === selected) return setError('Source and recipient must be different accounts.');
    }
    setError(''); setStep('confirm');
  };

  const confirm = async () => {
    setBusy(true); setError('');
    const body = kind === 'transfer'
      ? { sourceAccount: selected, destinationAccount: dest.trim(), amount: Number(amount) }
      : { accountNumber: selected, amount: Number(amount) };
    try {
      setResult(await api(cfg.path, { method: 'POST', body, idempotencyKey: key.current }));
      setStep('done');
    } catch (err) {
      setError(err.message); setStep('edit');
    } finally { setBusy(false); }
  };

  const reset = () => {
    setAmount(''); setDest(''); setResult(null); setStep('edit'); setError(''); key.current = newKey();
    accounts.reload();
  };

  if (step === 'done') {
    return (
      <>
        <h1 className="page-title">{cfg.title}</h1>
        <Card title="Transaction result" className="narrow">
          <Alert type="success">
            {result.replayed ? 'This request was already processed. Showing the original result.' : `${cfg.verb} completed successfully.`}
          </Alert>
          <dl className="details">
            <dt>Reference</dt><dd><code>{result.reference}</code></dd>
            <dt>Status</dt><dd><Badge value={result.status} /></dd>
            <dt>Type</dt><dd>{result.type}</dd>
            <dt>Amount</dt><dd>{money(result.amount)}</dd>
            <dt>From</dt><dd>{result.sourceAccount}</dd>
            <dt>To</dt><dd>{result.destinationAccount}</dd>
          </dl>
          <button className="btn primary" onClick={reset}>New {kind}</button>
        </Card>
      </>
    );
  }

  return (
    <>
      <h1 className="page-title">{cfg.title}</h1>
      <Card className="narrow">
        <Alert>{error || accounts.error}</Alert>
        {step === 'edit' ? (
          <form onSubmit={review} noValidate className="form">
            <label>{cfg.accountLabel}
              <select value={selected} onChange={edit(setAccount)}>
                {active.map((a) => <option key={a.id} value={a.accountNumber}>
                  {a.accountNumber} ({a.type}) - {money(a.balance)}</option>)}
              </select>
            </label>
            {kind === 'transfer' && (
              <label>Recipient account number
                <input value={dest} onChange={edit(setDest)} placeholder="e.g. LL1000000003" />
              </label>
            )}
            <label>Amount
              <input inputMode="decimal" value={amount} onChange={edit(setAmount)} placeholder="0.00" />
            </label>
            <button className="btn primary">Review</button>
          </form>
        ) : (
          <div className="form">
            <h3>Please confirm</h3>
            <dl className="details">
              <dt>Action</dt><dd>{cfg.verb}</dd>
              <dt>{kind === 'transfer' ? 'From' : 'Account'}</dt><dd>{selected}</dd>
              {kind === 'transfer' && (<><dt>To</dt><dd>{dest.trim()}</dd></>)}
              <dt>Amount</dt><dd><strong>{money(amount)}</strong></dd>
            </dl>
            <div className="row">
              <button className="btn ghost" disabled={busy} onClick={() => setStep('edit')}>Back</button>
              <button className="btn primary" disabled={busy} onClick={confirm}>
                {busy ? 'Processing...' : `Confirm ${kind}`}
              </button>
            </div>
          </div>
        )}
      </Card>
    </>
  );
}