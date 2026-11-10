import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import {
  Landmark, LayoutDashboard, Wallet, ArrowDownToLine, ArrowUpFromLine, ArrowLeftRight,
  History, Users, ShieldCheck, LogOut, Menu, FilePlus,
} from 'lucide-react';
import { useAuth } from '../auth/AuthContext';

const NAV = {
  ACCOUNT_HOLDER: [
    ['/', 'Dashboard', LayoutDashboard], ['/accounts', 'Accounts', Wallet],
    ['/deposit', 'Deposit', ArrowDownToLine], ['/withdraw', 'Withdraw', ArrowUpFromLine],
    ['/transfer', 'Transfer', ArrowLeftRight], ['/history', 'History', History],
    ['/onboarding', 'Open an account', FilePlus],
  ],
  ADMIN: [['/admin', 'Administration', Users], ['/auditor', 'Ledger & Audit', ShieldCheck]],
  AUDITOR: [['/auditor', 'Audit Dashboard', ShieldCheck]],
};
const PENDING_NAV = [['/onboarding', 'My application', FilePlus]];
const ROLE_LABEL = { ADMIN: 'Administrator', ACCOUNT_HOLDER: 'Account Holder', AUDITOR: 'Auditor' };

export default function Layout() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const items = user.role === 'ACCOUNT_HOLDER' && user.status !== 'ACTIVE' ? PENDING_NAV : NAV[user.role];

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
            <div className="avatar">{user.username[0].toUpperCase()}</div>
            <div className="who"><strong>{user.username}</strong><small>{ROLE_LABEL[user.role]}</small></div>
          </div>
        </header>
        <main className="content"><Outlet /></main>
      </div>
    </div>
  );
}