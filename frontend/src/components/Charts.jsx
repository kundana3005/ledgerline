import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, Legend, CartesianGrid, PieChart, Pie, Cell } from 'recharts';

const COLORS = ['#16a34a', '#0b1f3a', '#b45309', '#64748b'];
const num = (v) => Number(v);

export function Bars({ data, xKey, series, height = 260 }) {
  if (!data || data.length === 0) return <p className="muted pad">No data yet.</p>;
  const rows = data.map((r) => ({ ...r, ...Object.fromEntries(series.map((s) => [s.key, num(r[s.key])])) }));
  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={rows}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} />
        <XAxis dataKey={xKey} /><YAxis /><Tooltip /><Legend />
        {series.map((s, i) => <Bar key={s.key} dataKey={s.key} name={s.name} fill={COLORS[i % COLORS.length]} radius={[4, 4, 0, 0]} />)}
      </BarChart>
    </ResponsiveContainer>
  );
}

export function Distribution({ data, nameKey, valueKey, height = 260 }) {
  if (!data || data.length === 0) return <p className="muted pad">No data yet.</p>;
  const rows = data.map((r) => ({ ...r, [valueKey]: num(r[valueKey]) }));
  return (
    <ResponsiveContainer width="100%" height={height}>
      <PieChart>
        <Pie data={rows} dataKey={valueKey} nameKey={nameKey} outerRadius={90} label>
          {rows.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
        </Pie>
        <Tooltip /><Legend />
      </PieChart>
    </ResponsiveContainer>
  );
}