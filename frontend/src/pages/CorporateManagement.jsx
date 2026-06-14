import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Building2, Plus, RefreshCw, X, ChevronDown, ChevronUp,
  Pencil, Search, AlertTriangle, CheckCircle, CreditCard,
  Globe2, Hash, TrendingUp, Layers, Calendar, DollarSign,
  ShieldCheck, Lock, Unlock, Trash2, Eye
} from 'lucide-react';
import api from '../services/api';

// ─── Helpers ────────────────────────────────────────────────────────────────

const FACILITY_TYPES = [
  { value: 'LETTER_OF_CREDIT_FACILITY', label: 'Letter of Credit Facility' },
  { value: 'GUARANTEE_FACILITY', label: 'Guarantee Facility' },
  { value: 'EXPORT_FINANCE_FACILITY', label: 'Export Finance Facility' },
];

const CURRENCIES = ['USD', 'EUR', 'GBP', 'AED', 'SAR', 'INR', 'SGD', 'CHF'];

const CLIENT_STATUSES = ['ACTIVE', 'SUSPENDED', 'INACTIVE'];

const fmtMoney = (n) =>
  Number(n).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 });

const statusBadge = (s) => {
  const map = {
    ACTIVE: 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20',
    SUSPENDED: 'bg-rose-500/10 text-rose-500 border border-rose-500/20',
    INACTIVE: 'bg-slate-500/10 text-slate-400 border border-slate-500/20',
  };
  return map[s] || map.INACTIVE;
};

const facilityBadge = (s) => {
  const map = {
    ACTIVE: 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20',
    EXPIRED: 'bg-rose-500/10 text-rose-500 border border-rose-500/20',
    SUSPENDED: 'bg-amber-500/10 text-amber-500 border border-amber-500/20',
    INACTIVE: 'bg-slate-500/10 text-slate-400 border border-slate-500/20',
  };
  return map[s] || map.INACTIVE;
};

const utilization = (limit, used) => {
  if (!limit || limit === 0) return 0;
  return Math.min(100, Math.round((used / limit) * 100));
};

// ─── Empty State ─────────────────────────────────────────────────────────────

const EmptyState = ({ icon: Icon, title, subtitle, action }) => (
  <div className="flex flex-col items-center justify-center py-20 text-center space-y-3">
    <div className="h-14 w-14 rounded-2xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center">
      <Icon className="h-7 w-7 text-slate-400" />
    </div>
    <p className="font-bold text-slate-600 dark:text-slate-300">{title}</p>
    <p className="text-xs text-slate-400 max-w-xs">{subtitle}</p>
    {action}
  </div>
);

// ─── Main Component ──────────────────────────────────────────────────────────

const CorporateManagement = () => {
  const { isAdmin } = useAuth();

  // ── State ──
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [expandedClient, setExpandedClient] = useState(null);
  const [clientFacilities, setClientFacilities] = useState({}); // { [clientId]: [...] }
  const [loadingFacilities, setLoadingFacilities] = useState({});

  // Modals
  const [showClientModal, setShowClientModal] = useState(false);
  const [editClient, setEditClient] = useState(null); // null = create mode
  const [showFacilityModal, setShowFacilityModal] = useState(false);
  const [facilityForClient, setFacilityForClient] = useState(null); // client object
  const [editFacility, setEditFacility] = useState(null);

  // Forms
  const [clientForm, setClientForm] = useState({
    companyName: '', country: '', taxId: '', creditLimit: '', status: 'ACTIVE', relationshipManagerId: null
  });
  const [relationshipManagers, setRelationshipManagers] = useState([]);
  const [facilityForm, setFacilityForm] = useState({
    facilityType: 'LETTER_OF_CREDIT_FACILITY', limitAmount: '',
    currency: 'USD', expiryDate: '', status: 'ACTIVE'
  });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  // ── Fetch ──
  const fetchClients = async () => {
    try {
      setLoading(true);
      const res = await api.get('/corporates');
      setClients(res.data.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const fetchRelationshipManagers = async () => {
    try {
      const res = await api.get('/users');
      const allUsers = res.data.data || [];
      const rms = allUsers.filter(u => u.role === 'RELATIONSHIP_MANAGER');
      setRelationshipManagers(rms);
    } catch (e) {
      console.error("Failed to load users for RM assignment", e);
    }
  };

  const fetchFacilities = async (clientId) => {
    if (clientFacilities[clientId]) return; // already loaded
    try {
      setLoadingFacilities(prev => ({ ...prev, [clientId]: true }));
      const res = await api.get(`/corporates/${clientId}/facilities`);
      setClientFacilities(prev => ({ ...prev, [clientId]: res.data.data || [] }));
    } catch (e) {
      setClientFacilities(prev => ({ ...prev, [clientId]: [] }));
    } finally {
      setLoadingFacilities(prev => ({ ...prev, [clientId]: false }));
    }
  };

  useEffect(() => {
    fetchClients();
    fetchRelationshipManagers();
  }, []);

  // ── Toggle client row ──
  const toggleExpand = (clientId) => {
    if (expandedClient === clientId) {
      setExpandedClient(null);
    } else {
      setExpandedClient(clientId);
      fetchFacilities(clientId);
    }
  };

  // ── Client Modal ──
  const openCreateClient = () => {
    setEditClient(null);
    setClientForm({ companyName: '', country: '', taxId: '', creditLimit: '', status: 'ACTIVE', relationshipManagerId: null });
    setFormError('');
    setShowClientModal(true);
  };

  const openEditClient = (client) => {
    setEditClient(client);
    setClientForm({
      companyName: client.companyName || '',
      country: client.country || '',
      taxId: client.taxId || '',
      creditLimit: client.creditLimit || '',
      status: client.status || 'ACTIVE',
      relationshipManagerId: client.relationshipManagerId || null
    });
    setFormError('');
    setShowClientModal(true);
  };

  const handleSaveClient = async (e) => {
    e.preventDefault();
    setFormError('');
    if (!clientForm.companyName.trim()) return setFormError('Company name is required.');
    if (!clientForm.country.trim()) return setFormError('Country is required.');
    if (!clientForm.creditLimit || isNaN(clientForm.creditLimit)) return setFormError('Valid credit limit is required.');
    try {
      setSaving(true);
      if (editClient) {
        await api.put(`/corporates/${editClient.id}`, clientForm);
      } else {
        await api.post('/corporates', clientForm);
      }
      setShowClientModal(false);
      setClients([]); // force refresh
      fetchClients();
    } catch (e) {
      setFormError(e.response?.data?.message || 'Failed to save client. Check all fields and try again.');
    } finally {
      setSaving(false);
    }
  };

  // ── Facility Modal ──
  const openCreateFacility = (client) => {
    setFacilityForClient(client);
    setEditFacility(null);
    setFacilityForm({
      facilityType: 'LETTER_OF_CREDIT_FACILITY', limitAmount: '',
      currency: 'USD', expiryDate: '', status: 'ACTIVE'
    });
    setFormError('');
    setShowFacilityModal(true);
  };

  const openEditFacility = (client, facility) => {
    setFacilityForClient(client);
    setEditFacility(facility);
    setFacilityForm({
      facilityType: facility.facilityType || 'LETTER_OF_CREDIT_FACILITY',
      limitAmount: facility.limitAmount || '',
      currency: facility.currency || 'USD',
      expiryDate: facility.expiryDate || '',
      status: facility.status || 'ACTIVE'
    });
    setFormError('');
    setShowFacilityModal(true);
  };

  const handleSaveFacility = async (e) => {
    e.preventDefault();
    setFormError('');
    if (!facilityForm.limitAmount || isNaN(facilityForm.limitAmount)) return setFormError('Valid limit amount is required.');
    if (!facilityForm.expiryDate) return setFormError('Expiry date is required.');
    try {
      setSaving(true);
      if (editFacility) {
        await api.put(`/corporates/facilities/${editFacility.id}`, facilityForm);
      } else {
        await api.post(`/corporates/facilities?clientId=${facilityForClient.id}`, facilityForm);
      }
      // Refresh that client's facilities
      setClientFacilities(prev => ({ ...prev, [facilityForClient.id]: undefined }));
      setTimeout(() => fetchFacilities(facilityForClient.id), 200);
      setShowFacilityModal(false);
    } catch (e) {
      setFormError(e.response?.data?.message || 'Failed to save facility. Check all fields and try again.');
    } finally {
      setSaving(false);
    }
  };

  // ── Filter ──
  const filtered = clients.filter(c =>
    c.companyName?.toLowerCase().includes(search.toLowerCase()) ||
    c.country?.toLowerCase().includes(search.toLowerCase()) ||
    c.taxId?.toLowerCase().includes(search.toLowerCase())
  );

  if (!isAdmin) {
    return (
      <EmptyState
        icon={Lock}
        title="Access Restricted"
        subtitle="Only Administrators can manage Corporate Clients and Credit Facilities."
      />
    );
  }

  return (
    <div className="space-y-6">

      {/* ── Page Header ── */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b dark:border-slate-900 pb-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight">Corporate Clients & Facilities</h1>
          <p className="text-xs text-slate-400 mt-1">
            Manage onboarded corporate entities, credit limits and line facilities (LC, BG, Export Finance)
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={fetchClients}
            className="p-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950/20 bg-white hover:bg-slate-100 transition-colors"
          >
            <RefreshCw className="h-4 w-4" />
          </button>
          <button
            onClick={openCreateClient}
            className="px-4 py-2.5 rounded-xl bg-brand-500 hover:bg-brand-600 text-white font-bold text-xs transition-all flex items-center gap-2 shadow-lg shadow-brand-500/20"
          >
            <Plus className="h-4 w-4" /> Add Corporate Client
          </button>
        </div>
      </div>

      {/* ── Search Bar ── */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
        <input
          type="text"
          placeholder="Search by company, country or tax ID…"
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-brand-500"
        />
      </div>

      {/* ── Summary Cards ── */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: 'Total Clients', value: clients.length, icon: Building2, color: 'brand' },
          { label: 'Active', value: clients.filter(c => c.status === 'ACTIVE').length, icon: Unlock, color: 'emerald' },
          { label: 'Suspended', value: clients.filter(c => c.status === 'SUSPENDED').length, icon: Lock, color: 'rose' },
          {
            label: 'Total Credit Limit',
            value: `$${fmtMoney(clients.reduce((s, c) => s + (c.creditLimit || 0), 0))}`,
            icon: DollarSign,
            color: 'amber'
          }
        ].map((card, i) => (
          <motion.div
            key={i}
            whileHover={{ y: -3 }}
            className="glass-card-light dark:glass-card-dark p-5 rounded-2xl flex items-center gap-4"
          >
            <div className={`h-10 w-10 rounded-xl bg-${card.color}-500/10 flex items-center justify-center flex-shrink-0`}>
              <card.icon className={`h-5 w-5 text-${card.color}-500`} />
            </div>
            <div>
              <p className="text-xs text-slate-400">{card.label}</p>
              <p className="text-lg font-extrabold">{card.value}</p>
            </div>
          </motion.div>
        ))}
      </div>

      {/* ── Clients Table / List ── */}
      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="h-8 w-8 rounded-full border-4 border-brand-500/30 border-t-brand-500 animate-spin" />
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={Building2}
          title="No Corporate Clients Found"
          subtitle="Start by adding your first corporate client using the button above."
          action={
            <button
              onClick={openCreateClient}
              className="mt-2 px-4 py-2 rounded-xl bg-brand-500 text-white text-xs font-bold hover:bg-brand-600 flex items-center gap-2 mx-auto"
            >
              <Plus className="h-4 w-4" /> Add First Client
            </button>
          }
        />
      ) : (
        <div className="space-y-3">
          {filtered.map(client => {
            const isExpanded = expandedClient === client.id;
            const facilities = clientFacilities[client.id] || [];
            const isLoadingFac = loadingFacilities[client.id];
            const pct = utilization(client.creditLimit, client.usedCredit || 0);

            return (
              <motion.div
                key={client.id}
                layout
                className="rounded-2xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900/60 bg-white overflow-hidden"
              >
                {/* Client Row */}
                <div
                  className="flex items-center gap-4 p-4 cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/40 transition-colors"
                  onClick={() => toggleExpand(client.id)}
                >
                  {/* Icon */}
                  <div className="h-10 w-10 rounded-xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center flex-shrink-0">
                    <Building2 className="h-5 w-5 text-brand-500" />
                  </div>

                  {/* Name + Country */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="font-bold text-sm truncate">{client.companyName}</p>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${statusBadge(client.status)}`}>
                        {client.status}
                      </span>
                    </div>
                    <div className="flex items-center gap-3 mt-0.5 flex-wrap">
                      <span className="text-[11px] text-slate-400 flex items-center gap-1">
                        <Globe2 className="h-3 w-3" /> {client.country}
                      </span>
                      <span className="text-[11px] text-slate-400 flex items-center gap-1">
                        <Hash className="h-3 w-3" /> {client.taxId}
                      </span>
                    </div>
                  </div>

                  {/* Credit Limit */}
                  <div className="hidden md:block text-right min-w-[120px]">
                    <p className="text-xs text-slate-400">Credit Limit</p>
                    <p className="text-sm font-extrabold text-brand-500">
                      USD {fmtMoney(client.creditLimit)}
                    </p>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-1 ml-2">
                    <button
                      onClick={e => { e.stopPropagation(); openEditClient(client); }}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-brand-500 hover:bg-brand-500/10 transition-colors"
                      title="Edit Client"
                    >
                      <Pencil className="h-3.5 w-3.5" />
                    </button>
                    <button
                      onClick={e => { e.stopPropagation(); openCreateFacility(client); }}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-emerald-500 hover:bg-emerald-500/10 transition-colors"
                      title="Add Facility"
                    >
                      <Plus className="h-3.5 w-3.5" />
                    </button>
                    {isExpanded ? (
                      <ChevronUp className="h-4 w-4 text-slate-400" />
                    ) : (
                      <ChevronDown className="h-4 w-4 text-slate-400" />
                    )}
                  </div>
                </div>

                {/* Expanded: Credit Facilities */}
                <AnimatePresence>
                  {isExpanded && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.2 }}
                      className="border-t dark:border-slate-800 border-slate-100 overflow-hidden"
                    >
                      <div className="p-4 space-y-3 dark:bg-slate-900/30 bg-slate-50/50">
                        <div className="flex items-center justify-between">
                          <p className="text-xs font-bold text-slate-500 uppercase tracking-wider flex items-center gap-2">
                            <Layers className="h-3.5 w-3.5" /> Credit Facilities
                          </p>
                          <button
                            onClick={() => openCreateFacility(client)}
                            className="px-3 py-1.5 rounded-lg bg-emerald-500/10 text-emerald-500 text-[11px] font-bold hover:bg-emerald-500/20 transition-colors flex items-center gap-1"
                          >
                            <Plus className="h-3 w-3" /> Add Facility
                          </button>
                        </div>

                        {isLoadingFac ? (
                          <div className="flex items-center gap-2 py-4 text-xs text-slate-400">
                            <div className="h-4 w-4 rounded-full border-2 border-brand-500/30 border-t-brand-500 animate-spin" />
                            Loading facilities…
                          </div>
                        ) : facilities.length === 0 ? (
                          <div className="flex items-center gap-3 py-4 text-xs text-slate-400">
                            <AlertTriangle className="h-4 w-4 text-amber-400" />
                            No credit facilities configured for this client.
                            <button onClick={() => openCreateFacility(client)} className="text-brand-500 underline">
                              Add one now
                            </button>
                          </div>
                        ) : (
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                            {facilities.map(fac => {
                              const pct = utilization(fac.limitAmount, fac.utilizedAmount);
                              return (
                                <div
                                  key={fac.id}
                                  className="p-3 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900 bg-white space-y-2"
                                >
                                  <div className="flex items-center justify-between">
                                    <div>
                                      <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                                        {fac.facilityType.replace(/_/g, ' ')}
                                      </p>
                                      <p className="text-sm font-extrabold mt-0.5">
                                        {fac.currency} {fmtMoney(fac.limitAmount)}
                                      </p>
                                    </div>
                                    <div className="flex items-center gap-1">
                                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${facilityBadge(fac.status)}`}>
                                        {fac.status}
                                      </span>
                                      <button
                                        onClick={() => openEditFacility(client, fac)}
                                        className="p-1 rounded-lg text-slate-400 hover:text-brand-500 hover:bg-brand-500/10 transition-colors"
                                      >
                                        <Pencil className="h-3 w-3" />
                                      </button>
                                    </div>
                                  </div>

                                  {/* Utilization Bar */}
                                  <div className="space-y-1">
                                    <div className="flex justify-between text-[10px] text-slate-400">
                                      <span>Utilized: {fac.currency} {fmtMoney(fac.utilizedAmount || 0)}</span>
                                      <span>{pct}%</span>
                                    </div>
                                    <div className="h-1.5 rounded-full dark:bg-slate-800 bg-slate-200 overflow-hidden">
                                      <div
                                        className={`h-full rounded-full transition-all ${pct > 80 ? 'bg-rose-500' : pct > 50 ? 'bg-amber-400' : 'bg-emerald-500'}`}
                                        style={{ width: `${pct}%` }}
                                      />
                                    </div>
                                  </div>

                                  {/* Expiry */}
                                  <div className="flex items-center gap-1 text-[10px] text-slate-400">
                                    <Calendar className="h-3 w-3" />
                                    Expires: {fac.expiryDate || 'N/A'}
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.div>
            );
          })}
        </div>
      )}

      {/* ═══════════════════════════════════════════════
          MODAL: Add / Edit Corporate Client
          ═══════════════════════════════════════════════ */}
      <AnimatePresence>
        {showClientModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
              onClick={() => setShowClientModal(false)}
            />
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="relative z-10 w-full max-w-lg rounded-3xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900 bg-white shadow-2xl p-6 space-y-5"
            >
              {/* Header */}
              <div className="flex items-center justify-between border-b dark:border-slate-800 pb-3">
                <h4 className="font-extrabold text-sm flex items-center gap-2">
                  <Building2 className="h-4 w-4 text-brand-500" />
                  {editClient ? 'Edit Corporate Client' : 'Add Corporate Client'}
                </h4>
                <button onClick={() => setShowClientModal(false)} className="p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800">
                  <X className="h-4.5 w-4.5" />
                </button>
              </div>

              {/* Form */}
              <form onSubmit={handleSaveClient} className="space-y-4 text-xs">
                {/* Company Name */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Company Name *</label>
                  <input
                    type="text"
                    value={clientForm.companyName}
                    onChange={e => setClientForm(p => ({ ...p, companyName: e.target.value }))}
                    placeholder="e.g. Acme Industrial Holdings Ltd"
                    className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-brand-500"
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  {/* Country */}
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Country *</label>
                    <input
                      type="text"
                      value={clientForm.country}
                      onChange={e => setClientForm(p => ({ ...p, country: e.target.value }))}
                      placeholder="e.g. United Arab Emirates"
                      className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-brand-500"
                      required
                    />
                  </div>

                  {/* Tax ID */}
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Tax ID / Registration No.</label>
                    <input
                      type="text"
                      value={clientForm.taxId}
                      onChange={e => setClientForm(p => ({ ...p, taxId: e.target.value }))}
                      placeholder="e.g. TRN-100234567"
                      className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-brand-500"
                    />
                  </div>

                  {/* Credit Limit */}
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Overall Credit Limit (USD) *</label>
                    <input
                      type="number"
                      value={clientForm.creditLimit}
                      onChange={e => setClientForm(p => ({ ...p, creditLimit: e.target.value }))}
                      placeholder="e.g. 5000000"
                      className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-brand-500"
                      required
                    />
                  </div>

                  {/* Status */}
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Status</label>
                    <select
                      value={clientForm.status}
                      onChange={e => setClientForm(p => ({ ...p, status: e.target.value }))}
                      className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-brand-500"
                    >
                      {CLIENT_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </div>
                </div>

                {/* Assigned Relationship Manager */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Assigned Relationship Manager</label>
                  <select
                    value={clientForm.relationshipManagerId || ''}
                    onChange={e => setClientForm(p => ({ ...p, relationshipManagerId: e.target.value ? Number(e.target.value) : null }))}
                    className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-brand-500"
                  >
                    <option value="">-- Unassigned / Select RM --</option>
                    {relationshipManagers.map(rm => (
                      <option key={rm.id} value={rm.id}>{rm.fullName} ({rm.email})</option>
                    ))}
                  </select>
                </div>

                {/* Error */}
                {formError && (
                  <div className="flex items-center gap-2 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-500 text-[11px]">
                    <AlertTriangle className="h-4 w-4 flex-shrink-0" /> {formError}
                  </div>
                )}

                {/* Submit */}
                <div className="flex gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowClientModal(false)}
                    className="flex-1 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 text-xs font-bold hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={saving}
                    className="flex-1 py-2.5 rounded-xl bg-brand-500 hover:bg-brand-600 text-white text-xs font-bold transition-all disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    {saving ? (
                      <div className="h-4 w-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
                    ) : (
                      <CheckCircle className="h-4 w-4" />
                    )}
                    {editClient ? 'Save Changes' : 'Create Client'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* ═══════════════════════════════════════════════
          MODAL: Add / Edit Credit Facility
          ═══════════════════════════════════════════════ */}
      <AnimatePresence>
        {showFacilityModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
              onClick={() => setShowFacilityModal(false)}
            />
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="relative z-10 w-full max-w-md rounded-3xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900 bg-white shadow-2xl p-6 space-y-5"
            >
              {/* Header */}
              <div className="flex items-center justify-between border-b dark:border-slate-800 pb-3">
                <div>
                  <h4 className="font-extrabold text-sm flex items-center gap-2">
                    <CreditCard className="h-4 w-4 text-emerald-500" />
                    {editFacility ? 'Edit Credit Facility' : 'Add Credit Facility'}
                  </h4>
                  {facilityForClient && (
                    <p className="text-[11px] text-slate-400 mt-0.5">
                      for <span className="text-brand-500 font-semibold">{facilityForClient.companyName}</span>
                    </p>
                  )}
                </div>
                <button onClick={() => setShowFacilityModal(false)} className="p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800">
                  <X className="h-4.5 w-4.5" />
                </button>
              </div>

              {/* Form */}
              <form onSubmit={handleSaveFacility} className="space-y-4 text-xs">
                {/* Facility Type */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Facility Type *</label>
                  <select
                    value={facilityForm.facilityType}
                    onChange={e => setFacilityForm(p => ({ ...p, facilityType: e.target.value }))}
                    className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  >
                    {FACILITY_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  {/* Limit Amount */}
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Facility Limit *</label>
                    <input
                      type="number"
                      value={facilityForm.limitAmount}
                      onChange={e => setFacilityForm(p => ({ ...p, limitAmount: e.target.value }))}
                      placeholder="e.g. 2000000"
                      className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-emerald-500"
                      required
                    />
                  </div>

                  {/* Currency */}
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Currency *</label>
                    <select
                      value={facilityForm.currency}
                      onChange={e => setFacilityForm(p => ({ ...p, currency: e.target.value }))}
                      className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-emerald-500"
                    >
                      {CURRENCIES.map(c => <option key={c} value={c}>{c}</option>)}
                    </select>
                  </div>

                  {/* Expiry Date */}
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Expiry Date *</label>
                    <input
                      type="date"
                      value={facilityForm.expiryDate}
                      onChange={e => setFacilityForm(p => ({ ...p, expiryDate: e.target.value }))}
                      className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-emerald-500"
                      required
                    />
                  </div>

                  {/* Status */}
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Status</label>
                    <select
                      value={facilityForm.status}
                      onChange={e => setFacilityForm(p => ({ ...p, status: e.target.value }))}
                      className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none focus:ring-1 focus:ring-emerald-500"
                    >
                      <option value="ACTIVE">ACTIVE</option>
                      <option value="INACTIVE">INACTIVE</option>
                      <option value="SUSPENDED">SUSPENDED</option>
                      <option value="EXPIRED">EXPIRED</option>
                    </select>
                  </div>
                </div>

                {/* Error */}
                {formError && (
                  <div className="flex items-center gap-2 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-500 text-[11px]">
                    <AlertTriangle className="h-4 w-4 flex-shrink-0" /> {formError}
                  </div>
                )}

                {/* Submit */}
                <div className="flex gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowFacilityModal(false)}
                    className="flex-1 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 text-xs font-bold hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={saving}
                    className="flex-1 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold transition-all disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    {saving ? (
                      <div className="h-4 w-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
                    ) : (
                      <CheckCircle className="h-4 w-4" />
                    )}
                    {editFacility ? 'Save Facility' : 'Create Facility'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
};

export default CorporateManagement;
