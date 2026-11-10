import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { api, setUnauthorizedHandler } from '../api/client';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);
export const homeFor = (user) => {
  if (user.role === 'ADMIN') return '/admin';
  if (user.role === 'AUDITOR') return '/auditor';
  return user.status === 'ACTIVE' ? '/' : '/onboarding';
};

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
    const u = { id: r.userId, username: r.username, role: r.role, status: r.status };
    sessionStorage.setItem('ll_token', r.token);
    sessionStorage.setItem('ll_user', JSON.stringify(u));
    setUser(u);
    return u;
  };

  // Called when the server reports a different status than the one stored at login
  const setStatus = useCallback((status) => {
    setUser((prev) => {
      if (!prev || prev.status === status) return prev;
      const next = { ...prev, status };
      sessionStorage.setItem('ll_user', JSON.stringify(next));
      return next;
    });
  }, []);

  return <Ctx.Provider value={{ user, login, logout, setStatus }}>{children}</Ctx.Provider>;
}