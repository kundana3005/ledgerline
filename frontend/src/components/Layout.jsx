import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import {
  Landmark, LayoutDashboard, Wallet, ArrowDownToLine, ArrowUpFromLine, ArrowLeftRight,
  History, Users, ShieldCheck, LogOut, Menu, FilePlus, Bell, KeyRound,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';

const NAV = {
  ACCOUNT_HOLDER: [
    ['/', 'Dashboard', LayoutDashboard], ['/accounts', 'Accounts', Wallet],
    ['/deposit', 'Deposit', ArrowDownToLine], ['/withdraw', 'Withdraw', ArrowUpFromLine],
    ['/transfer', 'Transfer', ArrowLeftRight], ['/history', 'History', History],
    ['/onboarding', 'Open an account', FilePlus], ['/notifications', 'Notifications', Bell],
  ],
  ADMIN: [['/admin', 'Administration', Users], ['/auditor', 'Ledger & Audit', ShieldCheck]],
  AUDITOR: [['/auditor', 'Audit Dashboard', ShieldCheck]],
};
const PENDING_NAV = [['/onboarding', 'My application', FilePlus], ['/notifications', 'Notifications', Bell]];
const COMMON = [['/change-password', 'Change password', KeyRound]];
const ROLE_LABEL = { ADMIN: 'Administrator', ACCOUNT_HOLDER: 'Account Holder', AUDITOR: 'Auditor' };

export default function Layout() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const isHolder = user.role === 'ACCOUNT_HOLDER';
  const base = isHolder && user.status !== 'ACTIVE' ? PENDING_NAV : NAV[user.role];
  const items = [...base, ...COMMON];

  useEffect(() => {
    if (!isHolder) return undefined;
    let alive = true;
    const load = () => api('/api/notifications/unread-count').then((r) => alive && setUnread(r.count)).catch(() => {});
    load();
    const timer = setInterval(load, 60000);
    window.addEventListener('ll-notifications', load);
    return () => { alive = false; clearInterval(timer); window.removeEventListener('ll-notifications', load); };
  }, [isHolder]);

  return (
    <div className="shell">
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="brand"><Landmark size={22} /> LedgerLine</div>
        <nav>
          {items.map(([to, label, Icon]) => (
            <NavLink key={to} to={to} end onClick={() => setOpen(false)}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              <Icon size={18} /> {label}
            </NavLink>
          ))}
        </nav>
        <button className="nav-link logout" onClick={logout}><LogOut size={18} /> Sign out</button>
      </aside>
      {open && <div className="scrim" onClick={() => setOpen(false)} />}

      <div className="main">
        <header className="topbar">
          <button className="menu-btn" aria-label="Open menu" onClick={() => setOpen(true)}><Menu size={22} /></button>
          <span className="app-name">LedgerLine</span>
          <div className="profile">
            {isHolder && (
              <Link className="bell" to="/notifications" aria-label="Notifications">
                <Bell size={20} />{unread > 0 && <span className="bell-dot">{unread > 99 ? '99+' : unread}</span>}
              </Link>
            )}
            <div className="avatar">{user.username[0].toUpperCase()}</div>
            <div className="who"><strong>{user.username}</strong><small>{ROLE_LABEL[user.role]}</small></div>
          </div>
        </header>
        <main className="content"><Outlet /></main>
      </div>
    </div>
  );
}