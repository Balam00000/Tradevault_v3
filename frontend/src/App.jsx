import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';

// Pages
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import Dashboard from './pages/Dashboard';
import LCManagement from './pages/LCManagement';
import BGManagement from './pages/BGManagement';
import BillsCollections from './pages/BillsCollections';
import ComplianceCases from './pages/ComplianceCases';
import Reports from './pages/Reports';
import AuditLedger from './pages/AuditLedger';
import Profile from './pages/Profile';
import UserManagement from './pages/UserManagement';
import CorporateManagement from './pages/CorporateManagement';
import Notifications from './pages/Notifications';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Auth Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />

          {/* Secure Banking Operations Portal (Protected via RBAC) */}
          <Route 
            path="/" 
            element={
              <ProtectedRoute>
                <Layout>
                  <Dashboard />
                </Layout>
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/profile" 
            element={
              <ProtectedRoute>
                <Layout>
                  <Profile />
                </Layout>
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/lcs" 
            element={
              <ProtectedRoute>
                <Layout>
                  <LCManagement />
                </Layout>
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/bgs" 
            element={
              <ProtectedRoute>
                <Layout>
                  <BGManagement />
                </Layout>
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/bills" 
            element={
              <ProtectedRoute allowedRoles={['CLIENT', 'OPERATIONS', 'ADMIN', 'RELATIONSHIP_MANAGER']}>
                <Layout>
                  <BillsCollections />
                </Layout>
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/compliance" 
            element={
              <ProtectedRoute allowedRoles={['COMPLIANCE', 'ADMIN']}>
                <Layout>
                  <ComplianceCases />
                </Layout>
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/reports" 
            element={
              <ProtectedRoute allowedRoles={['CLIENT', 'TREASURY', 'OPERATIONS', 'ADMIN']}>
                <Layout>
                  <Reports />
                </Layout>
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/audit-logs" 
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Layout>
                  <AuditLedger />
                </Layout>
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/user-management" 
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Layout>
                  <UserManagement />
                </Layout>
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/corporates" 
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'RELATIONSHIP_MANAGER']}>
                <Layout>
                  <CorporateManagement />
                </Layout>
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/notifications" 
            element={
              <ProtectedRoute>
                <Layout>
                  <Notifications />
                </Layout>
              </ProtectedRoute>
            } 
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
