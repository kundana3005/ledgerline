import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { api, setUnauthorizedHandler } from '../api/client';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);
export const homeFor = (role) => (role === 'ADMIN' ? '/admin' : role === 'AUDITOR' ? '/auditor' : '/');

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(sessionStorage.getItem('ll_user')); } catch { return null; }
  });

  const logout = useCallback(() => {
    sessionStorage.removeItem('ll_token');
    sessionStorage.removeItem('ll_user');
    setUser(null);
  }, []);

  useEffect(() => { setUnauthorizedHandler(logout); }, [logout]);

  const login = async (identifier, password) => {
    const r = await api('/api/auth/login', { method: 'POST', body: { identifier, password }, auth: false });
    const u = { id: r.userId, username: r.username, role: r.role };
    sessionStorage.setItem('ll_token', r.token);
    sessionStorage.setItem('ll_user', JSON.stringify(u));
    setUser(u);
    return u;
  };

  return <Ctx.Provider value={{ user, login, logout }}>{children}</Ctx.Provider>;
}