import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Users, Search, RefreshCw, Edit, Trash2, ShieldCheck, X,
  UserCheck, ShieldAlert, AlertTriangle, Building, Mail, User2
} from 'lucide-react';

const UserManagement = () => {
  const [users, setUsers] = useState([]);
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [roleFilter, setRoleFilter] = useState('');

  // Edit Modal State
  const [editingUser, setEditingUser] = useState(null);
  const [editForm, setEditForm] = useState({
    fullName: '',
    email: '',
    role: '',
    status: '',
    corporateClientId: ''
  });

  const fetchUsersAndClients = async () => {
    try {
      setLoading(true);
      const userRes = await api.get('/users');
      setUsers(userRes.data.data || []);

      const clientRes = await api.get('/corporates/clients');
      setClients(clientRes.data.data || []);
    } catch (e) {
      console.error('Error fetching users and clients', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsersAndClients();
  }, []);

  const handleQuickStatus = async (user, newStatus) => {
    try {
      await api.put(`/users/${user.id}`, {
        fullName: user.fullName,
        email: user.email,
        role: user.role,
        status: newStatus,
        corporateClientId: user.corporateClient?.id || null
      });
      fetchUsersAndClients();
    } catch (e) {
      console.error('Error updating status', e);
      alert('Failed to update status');
    }
  };

  const handleDelete = async (userId) => {
    if (!window.confirm('Are you sure you want to delete this user?')) return;
    try {
      await api.delete(`/users/${userId}`);
      fetchUsersAndClients();
    } catch (e) {
      console.error('Error deleting user', e);
      alert('Failed to delete user');
    }
  };

  const handleEditOpen = (user) => {
    setEditingUser(user);
    setEditForm({
      fullName: user.fullName,
      email: user.email,
      role: user.role,
      status: user.status,
      corporateClientId: user.corporateClient?.id || ''
    });
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.put(`/users/${editingUser.id}`, {
        fullName: editForm.fullName,
        email: editForm.email,
        role: editForm.role,
        status: editForm.status,
        corporateClientId: editForm.corporateClientId ? parseInt(editForm.corporateClientId) : null
      });
      setEditingUser(null);
      fetchUsersAndClients();
    } catch (e) {
      console.error('Error updating user details', e);
      alert('Failed to update user details');
    }
  };

  const filteredUsers = users.filter(u => {
    const matchesSearch = u.username.toLowerCase().includes(search.toLowerCase()) ||
      u.fullName.toLowerCase().includes(search.toLowerCase()) ||
      u.email.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter ? u.status === statusFilter : true;
    const matchesRole = roleFilter ? u.role === roleFilter : true;
    return matchesSearch && matchesStatus && matchesRole;
  });

  const getStatusBadge = (status) => {
    const badges = {
      ACTIVE: 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20',
      PENDING: 'bg-amber-500/10 text-amber-500 border border-amber-500/20 animate-pulse',
      SUSPENDED: 'bg-rose-500/10 text-rose-500 border border-rose-500/20'
    };
    return badges[status] || 'bg-slate-500/10 text-slate-500';
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b dark:border-slate-900 pb-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight flex items-center gap-2">
            <Users className="h-6 w-6 text-brand-500" /> Identity &amp; Access Governance
          </h1>
          <p className="text-xs text-slate-400 mt-1">Authorise pending client registrations, suspend sessions, and manage role parameters</p>
        </div>
        <button onClick={fetchUsersAndClients} className="p-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950/20 bg-white hover:bg-slate-100 transition-colors">
          <RefreshCw className="h-4 w-4" />
        </button>
      </div>

      {/* Filters */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="relative md:col-span-2">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search by username, email, full name..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>

        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-3 py-2 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900 bg-white text-xs focus:outline-none"
        >
          <option value="">All Statuses</option>
          <option value="ACTIVE">ACTIVE</option>
          <option value="PENDING">PENDING</option>
          <option value="SUSPENDED">SUSPENDED</option>
        </select>

        <select
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value)}
          className="px-3 py-2 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900 bg-white text-xs focus:outline-none"
        >
          <option value="">All Roles</option>
          <option value="CLIENT">CLIENT</option>
          <option value="OPERATIONS">OPERATIONS</option>
          <option value="RELATIONSHIP_MANAGER">RELATIONSHIP_MANAGER</option>
          <option value="TREASURY">TREASURY</option>
          <option value="COMPLIANCE">COMPLIANCE</option>
          <option value="ADMIN">ADMIN</option>
        </select>
      </div>

      {/* Users List */}
      <div className="glass-card-light dark:glass-card-dark rounded-3xl p-6 overflow-hidden space-y-4">
        <h3 className="font-bold text-base flex items-center gap-1.5">User Accounts Ledger</h3>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="text-slate-400 font-bold border-b dark:border-slate-800">
                <th className="py-3">Username</th>
                <th>Full Name</th>
                <th>Role</th>
                <th>Corporate Tenant</th>
                <th>Status</th>
                <th className="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-400 font-semibold">No registered users found matching the filter credentials.</td>
                </tr>
              ) : (
                filteredUsers.map(u => (
                  <tr key={u.id} className="border-b dark:border-slate-900/60 hover:bg-slate-500/5 transition-colors">
                    <td className="py-4 font-bold tracking-tight text-brand-500 dark:text-brand-400 flex items-center gap-2">
                      <User2 className="h-4 w-4" /> {u.username}
                    </td>
                    <td className="font-semibold text-slate-700 dark:text-slate-200">
                      <div>{u.fullName}</div>
                      <div className="text-[10px] text-slate-400 font-normal mt-0.5">{u.email}</div>
                    </td>
                    <td className="font-semibold text-[11px] uppercase tracking-wider">{u.role.replace('_', ' ')}</td>
                    <td className="text-slate-400 font-medium">
                      {u.corporateClient ? (
                        <span className="flex items-center gap-1">
                          <Building className="h-3.5 w-3.5 text-brand-500" /> {u.corporateClient.companyName}
                        </span>
                      ) : (
                        <span className="text-[10px] uppercase font-bold text-slate-500 tracking-wider">Internal Bank Employee</span>
                      )}
                    </td>
                    <td>
                      <span className={`px-2 py-0.5 rounded-full text-[9px] font-bold ${getStatusBadge(u.status)}`}>
                        {u.status}
                      </span>
                    </td>
                    <td className="text-right space-x-1">
                      {u.status === 'PENDING' && (
                        <button
                          onClick={() => handleQuickStatus(u, 'ACTIVE')}
                          className="px-2.5 py-1.5 rounded-lg bg-emerald-500 hover:bg-emerald-600 text-white text-[10px] font-bold transition-all inline-flex items-center gap-1"
                        >
                          <UserCheck className="h-3.5 w-3.5" /> Admit
                        </button>
                      )}
                      {u.status === 'ACTIVE' && (
                        <button
                          onClick={() => handleQuickStatus(u, 'SUSPENDED')}
                          className="px-2.5 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500 text-rose-500 hover:text-white border border-rose-500/20 text-[10px] font-bold transition-all inline-flex items-center gap-1"
                        >
                          <ShieldAlert className="h-3.5 w-3.5" /> Suspend
                        </button>
                      )}
                      {u.status === 'SUSPENDED' && (
                        <button
                          onClick={() => handleQuickStatus(u, 'ACTIVE')}
                          className="px-2.5 py-1.5 rounded-lg bg-brand-500/10 hover:bg-brand-500 text-brand-500 hover:text-white border border-brand-500/20 text-[10px] font-bold transition-all inline-flex items-center gap-1"
                        >
                          Reactivate
                        </button>
                      )}

                      <button
                        onClick={() => handleEditOpen(u)}
                        className="p-1.5 rounded-lg border dark:border-slate-800 border-slate-200 dark:hover:bg-slate-800 hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition-colors inline-flex items-center"
                      >
                        <Edit className="h-4 w-4" />
                      </button>

                      {u.username !== 'admin' && (
                        <button
                          onClick={() => handleDelete(u.id)}
                          className="p-1.5 rounded-lg border border-rose-500/10 hover:bg-rose-500/5 text-rose-500 transition-colors inline-flex items-center"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Edit Modal */}
      <AnimatePresence>
        {editingUser && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setEditingUser(null)}
              className="absolute inset-0 bg-slate-950/60 backdrop-blur-sm"
            ></motion.div>

            <motion.div
              initial={{ scale: 0.95, y: 20 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 20 }}
              className="w-full max-w-md rounded-3xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900 bg-white shadow-2xl p-6 space-y-4 relative z-10 text-xs"
            >
              <div className="flex justify-between items-center border-b dark:border-slate-800 pb-3">
                <h4 className="font-extrabold text-sm flex items-center gap-2">
                  <Edit className="h-4.5 w-4.5 text-brand-500" /> Edit User Authority: {editingUser.username}
                </h4>
                <button onClick={() => setEditingUser(null)} className="p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800">
                  <X className="h-4.5 w-4.5" />
                </button>
              </div>

              <form onSubmit={handleEditSubmit} className="space-y-4 text-xs">
                <div className="space-y-1">
                  <label className="font-bold text-slate-400">Full Name</label>
                  <input
                    type="text"
                    value={editForm.fullName}
                    onChange={(e) => setEditForm(prev => ({ ...prev, fullName: e.target.value }))}
                    className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none"
                    required
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">Email Address</label>
                  <input
                    type="email"
                    value={editForm.email}
                    onChange={(e) => setEditForm(prev => ({ ...prev, email: e.target.value }))}
                    className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none"
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400">System Role</label>
                    <select
                      value={editForm.role}
                      onChange={(e) => setEditForm(prev => ({ ...prev, role: e.target.value }))}
                      className="w-full px-3 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none"
                    >
                      <option value="CLIENT">CLIENT</option>
                      <option value="OPERATIONS">OPERATIONS</option>
                      <option value="RELATIONSHIP_MANAGER">RELATIONSHIP_MANAGER</option>
                      <option value="TREASURY">TREASURY</option>
                      <option value="COMPLIANCE">COMPLIANCE</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
                  </div>

                  <div className="space-y-1">
                    <label className="font-bold text-slate-400">Account Status</label>
                    <select
                      value={editForm.status}
                      onChange={(e) => setEditForm(prev => ({ ...prev, status: e.target.value }))}
                      className="w-full px-3 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none"
                    >
                      <option value="ACTIVE">ACTIVE</option>
                      <option value="PENDING">PENDING</option>
                      <option value="SUSPENDED">SUSPENDED</option>
                    </select>
                  </div>
                </div>

                {editForm.role === 'CLIENT' && (
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400">Corporate Client Mapping</label>
                    <select
                      value={editForm.corporateClientId}
                      onChange={(e) => setEditForm(prev => ({ ...prev, corporateClientId: e.target.value }))}
                      className="w-full px-3 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none"
                    >
                      <option value="">-- Unassigned --</option>
                      {clients.map(c => (
                        <option key={c.id} value={c.id}>{c.companyName} ({c.taxId})</option>
                      ))}
                    </select>
                  </div>
                )}

                <button type="submit" className="w-full py-3 rounded-xl bg-brand-500 hover:bg-brand-600 text-white font-bold transition-all">
                  Commit Authority Changes
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default UserManagement;
