import { useState } from 'react';

export default function ConfirmDialog({ title, message, confirmLabel = 'Confirm', danger = false,
                                        askReason = false, onConfirm, onCancel }) {
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const go = async () => {
    if (askReason && !reason.trim()) { setError('A reason is required.'); return; }
    setBusy(true); setError('');
    try { await onConfirm(reason.trim()); }
    catch (e) { setError(e.message); setBusy(false); }
  };

  return (
    <div className="modal-back" role="dialog" aria-modal="true" onClick={onCancel}>
      <div className="modal card" onClick={(e) => e.stopPropagation()}>
        <h2>{title}</h2>
        {message && <p className="muted">{message}</p>}
        {askReason && (
          <label>Reason
            <input value={reason} maxLength={255} onChange={(e) => { setReason(e.target.value); setError(''); }} autoFocus />
          </label>
        )}
        {error && <div className="alert error" role="alert">{error}</div>}
        <div className="row end">
          <button className="btn ghost" disabled={busy} onClick={onCancel}>Cancel</button>
          <button className={`btn ${danger ? 'danger' : 'primary'}`} disabled={busy} onClick={go}>
            {busy ? 'Working...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}