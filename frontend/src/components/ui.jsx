const CURRENCY = 'INR';   // change here if you want another currency
const fmt = new Intl.NumberFormat('en-IN', { style: 'currency', currency: CURRENCY });
export const money = (v) => (v == null ? '-' : fmt.format(Number(v)));
export const dateTime = (v) => (v ? new Date(v).toLocaleString() : '-');

export function Card({ title, action, children, className = '' }) {
  return (
    <section className={`card ${className}`}>
      {(title || action) && (
        <div className="card-head"><h2>{title}</h2>{action}</div>
      )}
      {children}
    </section>
  );
}

export function Stat({ label, value, hint }) {
  return (
    <div className="card stat">
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
      {hint && <span className="stat-hint">{hint}</span>}
    </div>
  );
}

export function Badge({ value }) {
  return <span className={`badge b-${String(value).toLowerCase()}`}>{value}</span>;
}

export function Alert({ type = 'error', children }) {
  if (!children) return null;
  return <div className={`alert ${type}`} role="alert">{children}</div>;
}

export function DataTable({ columns, rows, empty = 'Nothing to show yet.', loading }) {
  if (loading) return <p className="muted pad">Loading...</p>;
  if (!rows || rows.length === 0) return <p className="muted pad">{empty}</p>;
  return (
    <div className="table-wrap">
      <table>
        <thead><tr>{columns.map((c) => <th key={c.key}>{c.label}</th>)}</tr></thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={r.id ?? r.reference ?? i}>
              {columns.map((c) => <td key={c.key}>{c.render ? c.render(r) : r[c.key]}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <div className="pager">
      <button className="btn ghost" disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</button>
      <span>Page {page + 1} of {totalPages}</span>
      <button className="btn ghost" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Next</button>
    </div>
  );
}