import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import Landing from './pages/Landing';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Onboarding from './pages/Onboarding';
import Dashboard from './pages/Dashboard';
import Accounts from './pages/Accounts';
import TxForm from './pages/TxForm';
import History from './pages/History';
import Notifications from './pages/Notifications';
import ChangePassword from './pages/ChangePassword';
import Admin from './pages/Admin';
import Auditor from './pages/Auditor';

const HOLDER = ['ACCOUNT_HOLDER'];
const guard = (roles, el, allowPending = false) => (
  <ProtectedRoute roles={roles} allowPending={allowPending}>{el}</ProtectedRoute>
);

export default function App() {
  return (
    <Routes>
            <Route path="/welcome" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/" element={guard(HOLDER, <Dashboard />)} />
        <Route path="/onboarding" element={guard(HOLDER, <Onboarding />, true)} />
        <Route path="/notifications" element={guard(HOLDER, <Notifications />, true)} />
        <Route path="/accounts" element={guard(HOLDER, <Accounts />)} />
        <Route path="/deposit" element={guard(HOLDER, <TxForm kind="deposit" />)} />
        <Route path="/withdraw" element={guard(HOLDER, <TxForm kind="withdraw" />)} />
        <Route path="/transfer" element={guard(HOLDER, <TxForm kind="transfer" />)} />
        <Route path="/history" element={guard(HOLDER, <History />)} />
        <Route path="/change-password" element={<ChangePassword />} />
        <Route path="/admin" element={guard(['ADMIN'], <Admin />)} />
        <Route path="/auditor" element={guard(['AUDITOR', 'ADMIN'], <Auditor />)} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}