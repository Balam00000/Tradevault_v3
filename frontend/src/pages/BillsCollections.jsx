import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  FileSpreadsheet, Plus, Search, RefreshCw, X, CheckCircle, 
  MapPin, Truck, AlertCircle, FileText, ArrowRight, ShieldCheck, 
  TrendingUp, Activity, Inbox, ChevronRight, Send, RotateCcw,
  Clock, PackageCheck, Ban, Loader2
} from 'lucide-react';

const BillsCollections = () => {
  const { user, isClient, isOps, isRM, isAdmin, corporateClientId } = useAuth();
  const canUpdateStatus = isOps || isAdmin;
  
  // Data State
  const [bills, setBills] = useState([]);
  const [collections, setCollections] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('bills'); // bills | collections
  const [selectedItem, setSelectedItem] = useState(null);
  const [updatingStatus, setUpdatingStatus] = useState(false);
  const [complianceHoldMsg, setComplianceHoldMsg] = useState(null);

  // Modals & Panels State
  const [showCreateBillModal, setShowCreateBillModal] = useState(false);
  const [showCreateColModal, setShowCreateColModal] = useState(false);

  // Search/Filters State
  const [search, setSearch] = useState('');

  // 1. Create Export Bill Form State
  const [newBill, setNewBill] = useState({
    billNumber: '',
    amount: '',
    currency: 'USD',
    drawerName: 'Acme Industrial Holdings',
    draweeName: '',
    maturityDate: '',
    collectionBank: ''
  });

  // 2. Create Collection Instruction Form State
  const [newCol, setNewCol] = useState({
    instructionRef: '',
    amount: '',
    currency: 'USD',
    tenureType: 'SIGHT',
    draweeName: '',
    instructionDetails: ''
  });

  // Fetch Bills and Collections
  const fetchData = async () => {
    try {
      setLoading(true);
      const billsRes = await api.get('/bills');
      setBills(billsRes.data.data || []);

      const colRes = await api.get('/bills/collections');
      setCollections(colRes.data.data || []);

      if (corporateClientId) {
        const clientRes = await api.get('/corporates/clients');
        const clientList = clientRes.data.data || [];
        if (clientList.length > 0) {
          setNewBill(prev => ({ ...prev, drawerName: clientList[0].companyName }));
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  // ── Status Update Handlers ──────────────────────────────────
  const BILL_WORKFLOW = [
    { status: 'INITIATED', tracking: 'DOCUMENTS_PREPARED', label: 'Initiated', icon: Clock },
    { status: 'DOCUMENTS_SENT', tracking: 'COURIER_DISPATCHED', label: 'Documents Sent', icon: Send },
    { status: 'ACCEPTED', tracking: 'DRAWEE_ACCEPTED', label: 'Accepted', icon: CheckCircle },
    { status: 'PAID', tracking: 'REMITTANCE_COMPLETE', label: 'Paid', icon: PackageCheck },
  ];

  const COL_WORKFLOW = [
    { status: 'PENDING', label: 'Pending', icon: Clock },
    { status: 'PROCESSING', label: 'Processing', icon: Activity },
    { status: 'COLLECTED', label: 'Collected', icon: PackageCheck },
  ];

  const handleUpdateBillStatus = async (billId, newStatus, trackingStatus) => {
    try {
      setUpdatingStatus(true);
      setComplianceHoldMsg(null);
      const res = await api.put(`/bills/${billId}/status`, { status: newStatus, trackingStatus });
      const updated = res.data.data;
      setBills(prev => prev.map(b => b.id === billId ? updated : b));
      setSelectedItem({ ...updated, type: 'bill' });
    } catch (e) {
      console.error(e);
      const msg = e.response?.data?.message || e.message;
      if (e.response?.status === 403 && msg.includes('COMPLIANCE_HOLD')) {
        setComplianceHoldMsg(msg);
      } else {
        alert('Failed to update bill status: ' + msg);
      }
    } finally {
      setUpdatingStatus(false);
    }
  };

  const handleMarkBillOverdue = async (billId) => {
    try {
      setUpdatingStatus(true);
      setComplianceHoldMsg(null);
      const res = await api.put(`/bills/${billId}/status`, { status: 'OVERDUE', trackingStatus: 'OVERDUE' });
      const updated = res.data.data;
      setBills(prev => prev.map(b => b.id === billId ? updated : b));
      setSelectedItem({ ...updated, type: 'bill' });
    } catch (e) {
      console.error(e);
      const msg = e.response?.data?.message || e.message;
      if (e.response?.status === 403 && msg.includes('COMPLIANCE_HOLD')) {
        setComplianceHoldMsg(msg);
      } else {
        alert('Failed to mark bill as overdue: ' + msg);
      }
    } finally {
      setUpdatingStatus(false);
    }
  };

  const handleUpdateColStatus = async (colId, newStatus) => {
    try {
      setUpdatingStatus(true);
      setComplianceHoldMsg(null);
      const res = await api.put(`/bills/collections/${colId}`, { status: newStatus });
      const updated = res.data.data;
      setCollections(prev => prev.map(c => c.id === colId ? updated : c));
      setSelectedItem({ ...updated, type: 'collection' });
    } catch (e) {
      console.error(e);
      const msg = e.response?.data?.message || e.message;
      if (e.response?.status === 403 && msg.includes('COMPLIANCE_HOLD')) {
        setComplianceHoldMsg(msg);
      } else {
        alert('Failed to update collection status: ' + msg);
      }
    } finally {
      setUpdatingStatus(false);
    }
  };

  const getNextBillStep = (currentStatus) => {
    const idx = BILL_WORKFLOW.findIndex(s => s.status === currentStatus);
    if (idx >= 0 && idx < BILL_WORKFLOW.length - 1) return BILL_WORKFLOW[idx + 1];
    return null;
  };

  const getNextColStep = (currentStatus) => {
    const idx = COL_WORKFLOW.findIndex(s => s.status === currentStatus);
    if (idx >= 0 && idx < COL_WORKFLOW.length - 1) return COL_WORKFLOW[idx + 1];
    return null;
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreateBill = async (e) => {
    e.preventDefault();
    if (!corporateClientId) {
      alert('Your account is not associated with any corporate client company. Please contact your administrator.');
      return;
    }
    try {
      const res = await api.post(`/bills?clientId=${corporateClientId}`, newBill);
      setBills(prev => [res.data.data, ...prev]);
      alert(`Export Bill registered successfully under transaction id ${res.data.data.billNumber}.`);
      setShowCreateBillModal(false);
      resetBillForm();
      fetchData();
    } catch (e) {
      console.error(e);
      alert('Error registering export bill');
    }
  };

  const handleCreateCol = async (e) => {
    e.preventDefault();
    if (!corporateClientId) {
      alert('Your account is not associated with any corporate client company. Please contact your administrator.');
      return;
    }
    try {
      const res = await api.post(`/bills/collections?clientId=${corporateClientId}`, newCol);
      setCollections(prev => [res.data.data, ...prev]);
      alert(`Documentary Collection Instruction ${res.data.data.instructionRef} recorded successfully.`);
      setShowCreateColModal(false);
      resetColForm();
      fetchData();
    } catch (e) {
      console.error(e);
      alert('Error issuing collection instruction');
    }
  };

  const resetBillForm = () => {
    setNewBill(prev => ({
      ...prev,
      billNumber: 'EXP-BILL-' + Math.floor(Math.random() * 9000 + 1000),
      amount: '',
      draweeName: '',
      maturityDate: '',
      collectionBank: ''
    }));
  };

  const resetColForm = () => {
    setNewCol({
      instructionRef: 'COL-INST-' + Math.floor(Math.random() * 900 + 100),
      amount: '',
      currency: 'USD',
      tenureType: 'SIGHT',
      draweeName: '',
      instructionDetails: ''
    });
  };

  const getBillBadge = (status) => {
    const badges = {
      INITIATED: 'bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
      DOCUMENTS_SENT: 'bg-blue-500/10 text-blue-500 border border-blue-500/20',
      ACCEPTED: 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20',
      PAID: 'bg-teal-500/10 text-teal-500 border border-teal-500/20',
      OVERDUE: 'bg-rose-500/10 text-rose-500 border border-rose-500/20',
    };
    return badges[status] || badges.INITIATED;
  };

  const getColBadge = (status) => {
    const badges = {
      PENDING: 'bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
      PROCESSING: 'bg-amber-500/10 text-amber-500 border border-amber-500/20',
      COLLECTED: 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20',
      RETURNED: 'bg-rose-500/10 text-rose-500 border border-rose-500/20',
    };
    return badges[status] || badges.PENDING;
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b dark:border-slate-900 pb-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight">Export Bills &amp; Collections</h1>
          <p className="text-xs text-slate-400 mt-1">Manage corporate exports tracking, DP/DA collection instructions, remittance mapping</p>
        </div>
        <div className="flex gap-2">
          <button onClick={fetchData} className="p-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950/20 bg-white hover:bg-slate-100 transition-colors">
            <RefreshCw className="h-4 w-4" />
          </button>
          {isClient && corporateClientId && (
            <>
              <button 
                onClick={() => { resetBillForm(); setShowCreateBillModal(true); }}
                className="px-4 py-2.5 rounded-xl bg-brand-500 hover:bg-brand-600 text-white font-bold text-xs transition-all"
              >
                + Register Export Bill
              </button>
              <button 
                onClick={() => { resetColForm(); setShowCreateColModal(true); }}
                className="px-4 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-xs transition-all"
              >
                + Issue Documentary Collection
              </button>
            </>
          )}
        </div>
      </div>

      {/* CLIENT ACCOUNT STATUS BANNER */}
      {isClient && !corporateClientId && (
        <div className="flex items-start gap-3 p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-xs">
          <AlertCircle className="h-5 w-5 text-amber-500 flex-shrink-0 mt-0.5" />
          <div className="flex-1 space-y-1">
            <p className="font-bold text-amber-500">Account Pending Corporate Client Assignment</p>
            <p className="text-slate-500 dark:text-slate-400">
              Your account has been admitted but has not been mapped to a corporate client company yet.
              Please contact your <strong className="text-slate-700 dark:text-slate-200">System Administrator</strong> to assign your corporate client profile in User Management.
              Once mapped, you will be able to register Export Bills and issue Documentary Collections.
            </p>
          </div>
        </div>
      )}

      {/* SECURE TAB CONTROLS */}
      <div className="flex border-b dark:border-slate-900 border-slate-200 max-w-sm">
        <button
          onClick={() => { setActiveTab('bills'); setSelectedItem(null); }}
          className={`flex-1 py-3 text-xs font-bold transition-all border-b-2 text-center ${activeTab === 'bills' ? 'border-brand-500 text-brand-500 dark:text-white' : 'border-transparent text-slate-400'}`}
        >
          Export Bills Registry
        </button>
        <button
          onClick={() => { setActiveTab('collections'); setSelectedItem(null); }}
          className={`flex-1 py-3 text-xs font-bold transition-all border-b-2 text-center ${activeTab === 'collections' ? 'border-brand-500 text-brand-500 dark:text-white' : 'border-transparent text-slate-400'}`}
        >
          Documentary Collections
        </button>
      </div>

      {/* ACTIVE PORTFOLIO GRID */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Table list */}
        <div className="glass-card-light dark:glass-card-dark rounded-3xl p-6 lg:col-span-2 space-y-4">
          <h3 className="font-bold text-base uppercase tracking-wider text-slate-500">{activeTab === 'bills' ? 'Export Bills' : 'Collection Orders'}</h3>
          
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="text-slate-400 font-bold border-b dark:border-slate-800">
                  <th className="py-3">{activeTab === 'bills' ? 'Bill Number' : 'Instruction Ref'}</th>
                  <th>Drawee partner</th>
                  <th>Amount</th>
                  <th>{activeTab === 'bills' ? 'Maturity' : 'Tenure'}</th>
                  <th>Status</th>
                  <th className="text-right">Inspect</th>
                </tr>
              </thead>
              <tbody>
                {activeTab === 'bills' ? (
                  bills.length === 0 ? (
                    <tr><td colSpan={6} className="py-8 text-center text-slate-400">No export bills logged.</td></tr>
                  ) : (
                    bills.map(item => (
                      <tr key={item.id} className="border-b dark:border-slate-900/60 hover:bg-slate-500/5 transition-colors">
                        <td className="py-3.5 font-bold tracking-tight text-brand-500 dark:text-brand-400">{item.billNumber}</td>
                        <td className="font-semibold">{item.draweeName}</td>
                        <td className="font-bold">USD {item.amount.toLocaleString()}</td>
                        <td>{item.maturityDate}</td>
                        <td>
                          <span className={`px-2 py-0.5 rounded-full text-[9px] font-bold ${getBillBadge(item.status)}`}>{item.status}</span>
                        </td>
                        <td className="text-right">
                          <button onClick={() => { setSelectedItem({ ...item, type: 'bill' }); setComplianceHoldMsg(null); }} className="p-1.5 rounded-lg dark:hover:bg-slate-800 hover:bg-slate-100 text-slate-400 hover:text-slate-700">
                            <Plus className="h-4 w-4" />
                          </button>
                        </td>
                      </tr>
                    ))
                  )
                ) : (
                  collections.length === 0 ? (
                    <tr><td colSpan={6} className="py-8 text-center text-slate-400 font-semibold">No collection instructions logged.</td></tr>
                  ) : (
                    collections.map(item => (
                      <tr key={item.id} className="border-b dark:border-slate-900/60 hover:bg-slate-500/5 transition-colors">
                        <td className="py-3.5 font-bold tracking-tight text-emerald-500 dark:text-emerald-400">{item.instructionRef}</td>
                        <td className="font-semibold">{item.draweeName}</td>
                        <td className="font-bold">USD {item.amount.toLocaleString()}</td>
                        <td>{item.tenureType}</td>
                        <td>
                          <span className={`px-2 py-0.5 rounded-full text-[9px] font-bold ${getColBadge(item.status)}`}>{item.status}</span>
                        </td>
                        <td className="text-right">
                          <button onClick={() => { setSelectedItem({ ...item, type: 'collection' }); setComplianceHoldMsg(null); }} className="p-1.5 rounded-lg dark:hover:bg-slate-800 hover:bg-slate-100 text-slate-400 hover:text-slate-700">
                            <Plus className="h-4 w-4" />
                          </button>
                        </td>
                      </tr>
                    ))
                  )
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Audit pipeline tracking details */}
        <div className="glass-card-light dark:glass-card-dark rounded-3xl p-6 space-y-6">
          <h3 className="font-bold text-base border-b dark:border-slate-800 pb-3">Remittance Tracker Pipeline</h3>

          {selectedItem ? (
            <div className="space-y-6">
              <div className="space-y-1">
                <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">TRACKING KEY: {selectedItem.id}</span>
                <h4 className="text-xl font-black">{selectedItem.type === 'bill' ? selectedItem.billNumber : selectedItem.instructionRef}</h4>
                <div className="flex gap-2 items-center mt-1">
                  <span className={`px-2 py-0.5 rounded-full text-[9px] font-bold ${selectedItem.type === 'bill' ? getBillBadge(selectedItem.status) : getColBadge(selectedItem.status)}`}>{selectedItem.status}</span>
                  <span className="text-xs text-slate-400 font-semibold">{selectedItem.type === 'bill' ? 'Export Bill' : 'Collection Order'}</span>
                </div>
              </div>

              {/* Progress Courier Pipeline — Export Bills */}
              {selectedItem.type === 'bill' && (
                <div className="space-y-4 p-4 rounded-2xl dark:bg-slate-950/40 bg-slate-50 border dark:border-slate-900 border-slate-200">
                  <h5 className="font-bold text-xs uppercase tracking-wider text-slate-400 flex items-center gap-1">
                    <Truck className="h-4 w-4 text-brand-500" /> Dispatch Milestones
                  </h5>
                  
                  <div className="relative border-l dark:border-slate-800 border-slate-200 pl-4 ml-2 space-y-4">
                    {BILL_WORKFLOW.map((step, i) => {
                      const currentIdx = BILL_WORKFLOW.findIndex(s => s.status === selectedItem.status);
                      const isCompleted = i <= currentIdx;
                      const StepIcon = step.icon;
                      return (
                        <div key={step.status} className="relative">
                          <span className={`absolute -left-6 top-0 h-4.5 w-4.5 rounded-full flex items-center justify-center text-[9px] font-bold border-2 border-white dark:border-slate-950 transition-all duration-300 ${
                            isCompleted 
                              ? 'bg-brand-500 text-white shadow-lg shadow-brand-500/30' 
                              : 'dark:bg-slate-950 bg-white dark:border-slate-800 border-slate-300 text-slate-400'
                          }`}>{i + 1}</span>
                          <h6 className={`font-bold text-xs ${isCompleted ? '' : 'text-slate-400'}`}>{step.label}</h6>
                          <p className="text-[10px] text-slate-400">
                            {i === 0 && 'Export Invoice & Bills of Lading structured cleanly.'}
                            {i === 1 && 'Shipped via secure banking dispatch courier.'}
                            {i === 2 && 'Maturity locked cleanly. Awaiting remittance.'}
                            {i === 3 && 'Remittance received. Bill closed successfully.'}
                          </p>
                        </div>
                      );
                    })}
                    {/* Overdue indicator */}
                    {selectedItem.status === 'OVERDUE' && (
                      <div className="relative">
                        <span className="absolute -left-6 top-0 h-4.5 w-4.5 rounded-full bg-rose-500 text-white flex items-center justify-center text-[9px] font-bold border-2 border-white dark:border-slate-950 shadow-lg shadow-rose-500/30">!</span>
                        <h6 className="font-bold text-xs text-rose-500">Overdue</h6>
                        <p className="text-[10px] text-rose-400">Bill has passed maturity without payment.</p>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Progress Pipeline — Collections */}
              {selectedItem.type === 'collection' && (
                <div className="space-y-4 p-4 rounded-2xl dark:bg-slate-950/40 bg-slate-50 border dark:border-slate-900 border-slate-200">
                  <h5 className="font-bold text-xs uppercase tracking-wider text-slate-400 flex items-center gap-1">
                    <Inbox className="h-4 w-4 text-emerald-500" /> Collection Milestones
                  </h5>
                  
                  <div className="relative border-l dark:border-slate-800 border-slate-200 pl-4 ml-2 space-y-4">
                    {COL_WORKFLOW.map((step, i) => {
                      const currentIdx = COL_WORKFLOW.findIndex(s => s.status === selectedItem.status);
                      const isCompleted = i <= currentIdx;
                      const StepIcon = step.icon;
                      return (
                        <div key={step.status} className="relative">
                          <span className={`absolute -left-6 top-0 h-4.5 w-4.5 rounded-full flex items-center justify-center text-[9px] font-bold border-2 border-white dark:border-slate-950 transition-all duration-300 ${
                            isCompleted 
                              ? 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/30' 
                              : 'dark:bg-slate-950 bg-white dark:border-slate-800 border-slate-300 text-slate-400'
                          }`}>{i + 1}</span>
                          <h6 className={`font-bold text-xs ${isCompleted ? '' : 'text-slate-400'}`}>{step.label}</h6>
                          <p className="text-[10px] text-slate-400">
                            {i === 0 && 'Collection instruction received, awaiting processing.'}
                            {i === 1 && 'Documents being routed to drawee bank for collection.'}
                            {i === 2 && 'Payment collected successfully from drawee.'}
                          </p>
                        </div>
                      );
                    })}
                    {/* Returned indicator */}
                    {selectedItem.status === 'RETURNED' && (
                      <div className="relative">
                        <span className="absolute -left-6 top-0 h-4.5 w-4.5 rounded-full bg-rose-500 text-white flex items-center justify-center text-[9px] font-bold border-2 border-white dark:border-slate-950 shadow-lg shadow-rose-500/30">!</span>
                        <h6 className="font-bold text-xs text-rose-500">Returned</h6>
                        <p className="text-[10px] text-rose-400">Documents returned — drawee refused payment/acceptance.</p>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Financial Metrics */}
              <div className="grid grid-cols-2 gap-4 text-xs">
                <div className="p-3 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900/30 bg-slate-50/50">
                  <span className="text-[10px] text-slate-400 uppercase font-semibold">Covenant Value</span>
                  <div className="font-extrabold text-base mt-1">USD {selectedItem.amount?.toLocaleString()}</div>
                </div>
                <div className="p-3 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900/30 bg-slate-50/50">
                  <span className="text-[10px] text-slate-400 uppercase font-semibold">Drawee Buyer Partner</span>
                  <div className="font-extrabold text-xs mt-1.5 truncate">{selectedItem.draweeName}</div>
                </div>
              </div>

              {/* ── COMPLIANCE HOLD BANNER ─────────────────────── */}
              {complianceHoldMsg && (
                <motion.div
                  initial={{ opacity: 0, scale: 0.97 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="p-4 rounded-2xl border-2 border-rose-500/40 bg-rose-500/10 space-y-2"
                >
                  <div className="flex items-center gap-2">
                    <AlertCircle className="h-5 w-5 text-rose-500 flex-shrink-0" />
                    <span className="font-black text-xs text-rose-500 uppercase tracking-wider">🔒 Compliance Hold Active</span>
                  </div>
                  <p className="text-[11px] text-rose-400 leading-relaxed font-semibold">
                    This transaction is blocked by a sanctions screening flag. A <span className="text-rose-300 font-black">Compliance Manager</span> must resolve the case in the <span className="text-rose-300 font-black">Compliance &amp; Watchlist</span> module before this status can be advanced.
                  </p>
                  <p className="text-[10px] text-rose-500/70 font-mono break-all">{complianceHoldMsg.replace('COMPLIANCE_HOLD: ', '')}</p>
                </motion.div>
              )}

              {/* ── OPS/ADMIN STATUS ACTION PANEL ─────────────── */}
              {canUpdateStatus && (
                <motion.div 
                  initial={{ opacity: 0, y: 8 }} 
                  animate={{ opacity: 1, y: 0 }} 
                  className="space-y-3 p-4 rounded-2xl border-2 border-dashed dark:border-slate-700 border-slate-300 dark:bg-slate-900/40 bg-slate-50/80"
                >
                  <h5 className="font-bold text-xs uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                    <ShieldCheck className="h-4 w-4 text-amber-500" /> Operations Actions
                  </h5>

                  {/* Export Bill Actions */}
                  {selectedItem.type === 'bill' && selectedItem.status !== 'PAID' && selectedItem.status !== 'OVERDUE' && (
                    <div className="space-y-2">
                      {(() => {
                        const next = getNextBillStep(selectedItem.status);
                        if (!next) return null;
                        const NextIcon = next.icon;
                        return (
                          <button
                            disabled={updatingStatus}
                            onClick={() => handleUpdateBillStatus(selectedItem.id, next.status, next.tracking)}
                            className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-brand-500 hover:bg-brand-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold text-xs transition-all shadow-lg shadow-brand-500/20 hover:shadow-brand-500/40"
                          >
                            {updatingStatus ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                            ) : (
                              <>
                                <NextIcon className="h-4 w-4" />
                                Advance to: {next.label}
                                <ChevronRight className="h-3.5 w-3.5" />
                              </>
                            )}
                          </button>
                        );
                      })()}

                      {/* Mark Overdue (available when not already PAID) */}
                      <button
                        disabled={updatingStatus}
                        onClick={() => handleMarkBillOverdue(selectedItem.id)}
                        className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl border dark:border-rose-900/50 border-rose-200 dark:bg-rose-950/20 bg-rose-50 text-rose-500 hover:bg-rose-100 dark:hover:bg-rose-950/40 disabled:opacity-50 disabled:cursor-not-allowed font-bold text-xs transition-all"
                      >
                        {updatingStatus ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <>
                            <AlertCircle className="h-4 w-4" />
                            Mark as Overdue
                          </>
                        )}
                      </button>
                    </div>
                  )}

                  {/* Bill is terminal */}
                  {selectedItem.type === 'bill' && (selectedItem.status === 'PAID' || selectedItem.status === 'OVERDUE') && (
                    <div className={`text-center py-3 rounded-xl text-xs font-bold ${
                      selectedItem.status === 'PAID' 
                        ? 'bg-teal-500/10 text-teal-500 border border-teal-500/20' 
                        : 'bg-rose-500/10 text-rose-500 border border-rose-500/20'
                    }`}>
                      {selectedItem.status === 'PAID' ? '✓ Bill Fully Settled' : '⚠ Bill Marked Overdue'}
                    </div>
                  )}

                  {/* Collection Actions */}
                  {selectedItem.type === 'collection' && selectedItem.status !== 'COLLECTED' && selectedItem.status !== 'RETURNED' && (
                    <div className="space-y-2">
                      {(() => {
                        const next = getNextColStep(selectedItem.status);
                        if (!next) return null;
                        const NextIcon = next.icon;
                        return (
                          <button
                            disabled={updatingStatus}
                            onClick={() => handleUpdateColStatus(selectedItem.id, next.status)}
                            className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold text-xs transition-all shadow-lg shadow-emerald-500/20 hover:shadow-emerald-500/40"
                          >
                            {updatingStatus ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                            ) : (
                              <>
                                <NextIcon className="h-4 w-4" />
                                Advance to: {next.label}
                                <ChevronRight className="h-3.5 w-3.5" />
                              </>
                            )}
                          </button>
                        );
                      })()}

                      {/* Mark Returned */}
                      <button
                        disabled={updatingStatus}
                        onClick={() => handleUpdateColStatus(selectedItem.id, 'RETURNED')}
                        className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl border dark:border-rose-900/50 border-rose-200 dark:bg-rose-950/20 bg-rose-50 text-rose-500 hover:bg-rose-100 dark:hover:bg-rose-950/40 disabled:opacity-50 disabled:cursor-not-allowed font-bold text-xs transition-all"
                      >
                        {updatingStatus ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <>
                            <RotateCcw className="h-4 w-4" />
                            Mark as Returned
                          </>
                        )}
                      </button>
                    </div>
                  )}

                  {/* Collection is terminal */}
                  {selectedItem.type === 'collection' && (selectedItem.status === 'COLLECTED' || selectedItem.status === 'RETURNED') && (
                    <div className={`text-center py-3 rounded-xl text-xs font-bold ${
                      selectedItem.status === 'COLLECTED' 
                        ? 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20' 
                        : 'bg-rose-500/10 text-rose-500 border border-rose-500/20'
                    }`}>
                      {selectedItem.status === 'COLLECTED' ? '✓ Collection Complete' : '⚠ Documents Returned'}
                    </div>
                  )}
                </motion.div>
              )}
            </div>
          ) : (
            <div className="py-20 text-center text-slate-400 text-xs font-semibold">
              Select an Export Bill or Collection to view shipping milestones and tracking status.
            </div>
          )}
        </div>

      </div>

      {/* ----------------------------------------------------
          MODALS SECTION
         ---------------------------------------------------- */}

      {/* 1. REGISTER EXPORT BILL MODAL */}
      <AnimatePresence>
        {showCreateBillModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setShowCreateBillModal(false)} className="absolute inset-0 bg-slate-950/60 backdrop-blur-sm"></motion.div>
            <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }} className="w-full max-w-md rounded-3xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900 bg-white shadow-2xl relative z-10 p-6 space-y-4">
              <div className="flex justify-between items-center border-b dark:border-slate-800 pb-3">
                <h4 className="font-extrabold text-sm flex items-center gap-2"><FileSpreadsheet className="h-4.5 w-4.5 text-brand-500" /> Register Export Bill</h4>
                <button onClick={() => setShowCreateBillModal(false)} className="p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800"><X className="h-4.5 w-4.5" /></button>
              </div>

              <form onSubmit={handleCreateBill} className="space-y-4 text-xs">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400">Bill Invoice Number</label>
                    <input type="text" disabled value={newBill.billNumber} className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-slate-50/50 font-mono focus:outline-none" />
                  </div>
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400">Invoice Amount (USD)</label>
                    <input type="number" placeholder="Enter bill amount" value={newBill.amount} onChange={(e) => setNewBill(prev => ({ ...prev, amount: parseFloat(e.target.value) }))} className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none" required />
                  </div>
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400">Drawee Buyer Corporate</label>
                    <input type="text" placeholder="e.g. EuroDistribution SA" value={newBill.draweeName} onChange={(e) => setNewBill(prev => ({ ...prev, draweeName: e.target.value }))} className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none" required />
                  </div>
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400">Collection Bank</label>
                    <input type="text" placeholder="e.g. Deutsche Bank Frankfurt" value={newBill.collectionBank} onChange={(e) => setNewBill(prev => ({ ...prev, collectionBank: e.target.value }))} className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none" required />
                  </div>
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">Maturity Date</label>
                  <input type="date" value={newBill.maturityDate} onChange={(e) => setNewBill(prev => ({ ...prev, maturityDate: e.target.value }))} className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none" required />
                </div>

                <button type="submit" className="w-full py-3 rounded-xl bg-brand-500 hover:bg-brand-600 text-white font-bold transition-all">Submit Registry (Initiated)</button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 2. REGISTER COLLECTION MODAL */}
      <AnimatePresence>
        {showCreateColModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setShowCreateColModal(false)} className="absolute inset-0 bg-slate-950/60 backdrop-blur-sm"></motion.div>
            <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }} className="w-full max-w-md rounded-3xl border dark:border-slate-800 border-slate-200 dark:bg-slate-900 bg-white shadow-2xl relative z-10 p-6 space-y-4">
              <div className="flex justify-between items-center border-b dark:border-slate-800 pb-3">
                <h4 className="font-extrabold text-sm flex items-center gap-2"><Inbox className="h-4.5 w-4.5 text-emerald-500" /> Issue Documentary Collection</h4>
                <button onClick={() => setShowCreateColModal(false)} className="p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800"><X className="h-4.5 w-4.5" /></button>
              </div>

              <form onSubmit={handleCreateCol} className="space-y-4 text-xs">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400">Instruction Reference</label>
                    <input type="text" disabled value={newCol.instructionRef} className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-slate-50/50 font-mono focus:outline-none" />
                  </div>
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400">Collection Value (USD)</label>
                    <input type="number" placeholder="Enter amount" value={newCol.amount} onChange={(e) => setNewCol(prev => ({ ...prev, amount: parseFloat(e.target.value) }))} className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none" required />
                  </div>
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400">Tenure Draft Type</label>
                    <select value={newCol.tenureType} onChange={(e) => setNewCol(prev => ({ ...prev, tenureType: e.target.value }))} className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none">
                      <option value="SIGHT">SIGHT (DP - Delivery Against Payment)</option>
                      <option value="USANCE">USANCE (DA - Delivery Against Acceptance)</option>
                    </select>
                  </div>
                  <div className="space-y-1">
                    <label className="font-bold text-slate-400">Drawee Buyer Corporate</label>
                    <input type="text" placeholder="e.g. Ontario Heavy Metals" value={newCol.draweeName} onChange={(e) => setNewCol(prev => ({ ...prev, draweeName: e.target.value }))} className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none" required />
                  </div>
                </div>

                <div className="space-y-1">
                  <label className="font-bold text-slate-400">Special Instructions details</label>
                  <textarea rows={3} placeholder="Provide specific draft routing directives..." value={newCol.instructionDetails} onChange={(e) => setNewCol(prev => ({ ...prev, instructionDetails: e.target.value }))} className="w-full px-4 py-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950 bg-white text-xs focus:outline-none" required></textarea>
                </div>

                <button type="submit" className="w-full py-3 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-bold transition-all">Submit Collection Instruction (Pending)</button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
};

export default BillsCollections;
