const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8081';
let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

export class ApiError extends Error {
  constructor(status, code, message) { super(message); this.status = status; this.code = code; }
}

export async function api(path, { method = 'GET', body, idempotencyKey, auth = true } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = sessionStorage.getItem('ll_token');
  if (auth && token) headers.Authorization = `Bearer ${token}`;
  if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey;

  let res;
  try {
    res = await fetch(BASE + path, {
      method, headers, body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'NETWORK', 'Cannot reach the server. Please try again.');
  }
  let data = null;
  try { data = await res.json(); } catch { /* empty body */ }
  if (!res.ok) {
    if (res.status === 401 && auth) onUnauthorized();   // expired or invalid token
    throw new ApiError(res.status, data?.error, data?.message || `Request failed (${res.status})`);
  }
  return data;
}

export const qs = (obj) => {
  const p = new URLSearchParams();
  Object.entries(obj).forEach(([k, v]) => { if (v !== '' && v != null) p.set(k, v); });
  const s = p.toString();
  return s ? `?${s}` : '';
};

export async function download(path, filename) {
  const token = sessionStorage.getItem('ll_token');
  let res;
  try { res = await fetch(BASE + path, { headers: { Authorization: `Bearer ${token}` } }); }
  catch { throw new ApiError(0, 'NETWORK', 'Cannot reach the server. Please try again.'); }
  if (!res.ok) {
    let message = `Download failed (${res.status})`;
    try { message = (await res.json()).message || message; } catch { /* no body */ }
    if (res.status === 401) onUnauthorized();
    throw new ApiError(res.status, null, message);
  }
  const url = URL.createObjectURL(await res.blob());
  const a = document.createElement('a');
  a.href = url; a.download = filename;
  document.body.appendChild(a); a.click(); a.remove();
  URL.revokeObjectURL(url);
}