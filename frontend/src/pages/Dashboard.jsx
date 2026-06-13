import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, BarChart, Bar, Legend
} from 'recharts';
import { 
  TrendingUp, ShieldAlert, Award, FileText, FileSpreadsheet, 
  Activity, Users, ShieldCheck, Database, Landmark, 
  CheckCircle, ArrowUpRight, Clock, Percent, DollarSign, X
} from 'lucide-react';
import { Link } from 'react-router-dom';

const Dashboard = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState({
    totalExposure: 0,
    lcExposure: 0,
    bgExposure: 0,
    billExposure: 0,
    totalLimit: 0,
    totalUtilized: 0,
    utilizationRate: 0,
    activeLcsCount: 0,
    activeBgsCount: 0,
    activeBillsCount: 0,
    totalScreenings: 0,
    openComplianceCases: 0
  });
  const [recentLcs, setRecentLcs] = useState([]);
  const [recentBgs, setRecentBgs] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [cases, setCases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [portfolio, setPortfolio] = useState([
    { id: 1, name: 'Acme Industrial Holdings', country: 'United States', limit: 50000000, exposure: 18500000, status: 'ACTIVE' },
    { id: 2, name: 'Nexus Electronics Corp', country: 'Singapore', limit: 80000000, exposure: 24000000, status: 'ACTIVE' },
  ]);
  const [isEvalOpen, setIsEvalOpen] = useState(false);
  const [evalStep, setEvalStep] = useState('review'); // 'review' | 'success-limit' | 'success-verify'

  const [facilities, setFacilities] = useState([]);
  const [exposureTrend, setExposureTrend] = useState([
    { month: 'Jan', LCs: 8.5, BGs: 1.0, Bills: 1.2 },
    { month: 'Feb', LCs: 9.8, BGs: 1.2, Bills: 1.5 },
    { month: 'Mar', LCs: 11.2, BGs: 1.1, Bills: 1.8 },
    { month: 'Apr', LCs: 12.5, BGs: 1.4, Bills: 2.0 },
    { month: 'May', LCs: 14.5, BGs: 1.5, Bills: 2.2 },
  ]);
  const [complianceMatchStats, setComplianceMatchStats] = useState([
    { range: '0-20%', count: 18, fill: '#10b981' },
    { range: '20-50%', count: 4, fill: '#3b82f6' },
    { range: '50-80%', count: 1, fill: '#f59e0b' },
    { range: '80-100%', count: 2, fill: '#ef4444' },
  ]);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        // Fetch global stats
        const statsRes = await api.get('/analytics/summary');
        setStats(prev => ({
          ...prev,
          ...(statsRes.data.data || {})
        }));

        // Fetch other contextual summaries
        const lcsRes = await api.get('/lcs');
        const allLcs = lcsRes.data.data || [];
        setRecentLcs(allLcs.slice(0, 5));

        const bgsRes = await api.get('/bgs');
        const allBgs = bgsRes.data.data || [];
        setRecentBgs(allBgs.slice(0, 4));

        let allBills = [];
        try {
          const billsRes = await api.get('/bills');
          allBills = billsRes.data.data || [];
        } catch (e) {
          console.error(e);
        }

        // Fetch facilities
        let allFacilities = [];
        try {
          const facilitiesRes = await api.get('/corporates/facilities');
          allFacilities = facilitiesRes.data.data || [];
          setFacilities(allFacilities);
        } catch (e) {
          console.error(e);
        }

        // Build RM/Staff portfolio dynamically
        if (user?.role === 'RELATIONSHIP_MANAGER' || user?.role === 'ADMIN' || user?.role === 'OPERATIONS' || user?.role === 'TREASURY') {
          try {
            const corporatesRes = await api.get('/corporates');
            const corporates = corporatesRes.data.data || [];
            
            const dynamicPortfolio = corporates.map(c => {
              const clientFacilities = allFacilities.filter(f => f.client?.id === c.id);
              const totalLimit = clientFacilities.reduce((sum, f) => sum + Number(f.limitAmount), 0);
              const totalUtilized = clientFacilities.reduce((sum, f) => sum + Number(f.utilizedAmount), 0);
              return {
                id: c.id,
                name: c.companyName,
                country: c.country,
                limit: totalLimit || c.creditLimit || 0,
                exposure: totalUtilized,
                status: c.status || 'ACTIVE'
              };
            });
            if (dynamicPortfolio.length > 0) {
              setPortfolio(dynamicPortfolio);
            }
          } catch (e) {
            console.error(e);
          }
        }

        // Fetch compliance screenings for match score distribution
        if (user?.role === 'COMPLIANCE' || user?.role === 'ADMIN') {
          try {
            const screeningsRes = await api.get('/compliance/screenings');
            const screenings = screeningsRes.data.data || [];
            let r1 = 0, r2 = 0, r3 = 0, r4 = 0;
            screenings.forEach(s => {
              const score = Number(s.matchScore) || 0;
              if (score <= 20) r1++;
              else if (score <= 50) r2++;
              else if (score <= 80) r3++;
              else r4++;
            });
            setComplianceMatchStats([
              { range: '0-20%', count: r1, fill: '#10b981' },
              { range: '20-50%', count: r2, fill: '#3b82f6' },
              { range: '50-80%', count: r3, fill: '#f59e0b' },
              { range: '80-100%', count: r4, fill: '#ef4444' },
            ]);
          } catch (e) {
            console.error(e);
          }
        }

        // Build dynamic trend data for last 5 months
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        const currentMonthIdx = new Date().getMonth();
        const trendMonths = [];
        for (let i = 4; i >= 0; i--) {
          const idx = (currentMonthIdx - i + 12) % 12;
          trendMonths.push({ name: months[idx], index: idx, year: new Date().getFullYear() - (currentMonthIdx - i < 0 ? 1 : 0) });
        }

        const dynamicTrend = trendMonths.map(tm => {
          const getMonthVal = (dateStr) => {
            if (!dateStr) return null;
            const d = new Date(dateStr);
            return { month: d.getMonth(), year: d.getFullYear() };
          };

          const sumAmount = (list) => {
            return list.filter(item => {
              const d = getMonthVal(item.issueDate || item.createdAt);
              if (!d) return false;
              return (d.year < tm.year) || (d.year === tm.year && d.month <= tm.index);
            }).reduce((sum, item) => sum + (Number(item.amount) || 0), 0) / 1000000;
          };

          return {
            month: tm.name,
            LCs: Number(sumAmount(allLcs).toFixed(2)),
            BGs: Number(sumAmount(allBgs).toFixed(2)),
            Bills: Number(sumAmount(allBills).toFixed(2))
          };
        });
        setExposureTrend(dynamicTrend);

        if (user?.role === 'ADMIN') {
          const auditRes = await api.get('/audit-logs');
          setAuditLogs((auditRes.data.data || []).slice(0, 5));
        }

        if (user?.role === 'COMPLIANCE' || user?.role === 'ADMIN') {
          const casesRes = await api.get('/compliance/cases');
          setCases((casesRes.data.data || []).slice(0, 5));
        }
      } catch (e) {
        console.error('Error fetching dashboard summaries', e);
      } finally {
        setLoading(false);
      }
    };

    if (user) {
      fetchDashboardData();
    }
  }, [user]);

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="h-8 w-64 bg-slate-200 dark:bg-slate-900 rounded-lg animate-pulse"></div>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          {[1, 2, 3, 4].map(n => (
            <div key={n} className="h-32 bg-slate-200 dark:bg-slate-900 rounded-3xl animate-pulse"></div>
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="h-96 lg:col-span-2 bg-slate-200 dark:bg-slate-900 rounded-3xl animate-pulse"></div>
          <div className="h-96 bg-slate-200 dark:bg-slate-900 rounded-3xl animate-pulse"></div>
        </div>
      </div>
    );
  }

  // ----------------------------------------------------
  // CHART DATA
  // ----------------------------------------------------
  const instrumentDistribution = [
    { name: 'Letters of Credit', value: stats?.lcExposure || 14500000, color: '#4a6be9' },
    { name: 'Bank Guarantees', value: stats?.bgExposure || 1500000, color: '#10b981' },
    { name: 'Export Bills', value: stats?.billExposure || 2200000, color: '#f59e0b' },
  ];

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(val);
  };

  const isDark = document.body.classList.contains('dark');

  // Render role layout
  return (
    <div className="space-y-8 pb-10">
      {/* Dashboard Top Greeting & Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight dark:text-white">
            Welcome back, <span className="bg-gradient-to-r from-brand-500 to-brand-300 bg-clip-text text-transparent">{user?.fullName.split(' ')[0]}</span>
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            System Status: Secured. Node Session verified. Role: <span className="font-semibold">{user?.role}</span>
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="text-right hidden md:block">
            <div className="text-xs text-slate-400 font-semibold tracking-wider">SECURE CONSOLE IP</div>
            <div className="text-sm font-mono text-brand-500 dark:text-brand-400">192.168.21.144</div>
          </div>
          <div className="h-10 w-10 rounded-xl bg-brand-500/10 flex items-center justify-center border border-brand-500/20 text-brand-500 animate-pulse">
            <Activity className="h-5 w-5" />
          </div>
        </div>
      </div>

      {/* CLIENT ONBOARDING BANNER — shown only for admitted but unmapped clients */}
      {user?.role === 'CLIENT' && !user?.corporateClientId && (
        <div className="flex items-start gap-4 p-5 rounded-2xl bg-amber-500/10 border border-amber-500/20">
          <div className="h-10 w-10 rounded-xl bg-amber-500/20 flex items-center justify-center flex-shrink-0">
            <ShieldAlert className="h-5 w-5 text-amber-500" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-bold text-amber-500">Pending Corporate Client Mapping</p>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 leading-relaxed">
              Your account has been admitted and activated, but has not yet been linked to a Corporate Client entity.
              Please ask your <strong className="text-slate-700 dark:text-slate-200">System Administrator</strong> to open <em>User Management</em> and assign your account to a corporate client.
              Once linked, all workspace features — Letters of Credit, Bank Guarantees, and Export Bills — will be unlocked.
            </p>
          </div>
        </div>
      )}

      {/* ----------------------------------------------------
          1. STATS METRICS (4 WIDGETS)
         ---------------------------------------------------- */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <motion.div whileHover={{ y: -4 }} className="glass-card-light dark:glass-card-dark p-6 rounded-3xl relative overflow-hidden">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Total Exposure</p>
              <h3 className="text-2xl font-black mt-2 tracking-tight">{formatCurrency(stats?.totalExposure)}</h3>
            </div>
            <div className="h-10 w-10 rounded-2xl bg-brand-500/10 flex items-center justify-center text-brand-500">
              <TrendingUp className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-4 flex items-center gap-1.5 text-xs text-emerald-500 font-semibold">
            <Percent className="h-3 w-3" />
            <span>12.4% increase month-on-month</span>
          </div>
        </motion.div>

        <motion.div whileHover={{ y: -4 }} className="glass-card-light dark:glass-card-dark p-6 rounded-3xl relative overflow-hidden">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Facility Limits Utilization</p>
              <h3 className="text-2xl font-black mt-2 tracking-tight">{stats?.utilizationRate.toFixed(2)}%</h3>
            </div>
            <div className="h-10 w-10 rounded-2xl bg-indigo-500/10 flex items-center justify-center text-indigo-500">
              <Landmark className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="w-full bg-slate-200 dark:bg-slate-800 h-1.5 rounded-full overflow-hidden">
              <div className="bg-indigo-500 h-full" style={{ width: `${stats?.utilizationRate}%` }}></div>
            </div>
            <div className="flex justify-between text-[10px] text-slate-400 mt-1">
              <span>{formatCurrency(stats?.totalUtilized)} Utilized</span>
              <span>{formatCurrency(stats?.totalLimit)} Total</span>
            </div>
          </div>
        </motion.div>

        <motion.div whileHover={{ y: -4 }} className="glass-card-light dark:glass-card-dark p-6 rounded-3xl relative overflow-hidden">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Active Letters of Credit</p>
              <h3 className="text-2xl font-black mt-2 tracking-tight">{stats?.activeLcsCount} Active LCs</h3>
            </div>
            <div className="h-10 w-10 rounded-2xl bg-amber-500/10 flex items-center justify-center text-amber-500">
              <FileText className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-4 flex items-center gap-1.5 text-xs text-slate-400">
            <Clock className="h-3.5 w-3.5 text-brand-500" />
            <span>2 drawings pending review</span>
          </div>
        </motion.div>

        <motion.div whileHover={{ y: -4 }} className="glass-card-light dark:glass-card-dark p-6 rounded-3xl relative overflow-hidden">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Compliance Registry</p>
              <h3 className="text-2xl font-black mt-2 tracking-tight">{stats?.openComplianceCases} Flagged Alerts</h3>
            </div>
            <div className="h-10 w-10 rounded-2xl bg-rose-500/10 flex items-center justify-center text-rose-500">
              <ShieldAlert className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-4 flex items-center gap-1.5 text-xs text-rose-500 font-semibold badge-pulse">
            <span>High match case investigation triggered</span>
          </div>
        </motion.div>
      </div>

      {/* ----------------------------------------------------
          2. ROLE-BASED WORKSPACES
         ---------------------------------------------------- */}

      {/* CORPORATE CLIENT DASHBOARD VIEW */}
      {user?.role === 'CLIENT' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="glass-card-light dark:glass-card-dark p-6 rounded-3xl lg:col-span-2 space-y-6">
            <div className="flex items-center justify-between border-b dark:border-slate-800 pb-4">
              <div>
                <h4 className="font-bold text-lg">Your Trade Finance Facilities</h4>
                <p className="text-xs text-slate-400">Onboarded credit limits &amp; utilizing instruments</p>
              </div>
              <div className="flex gap-2">
                <Link to="/lcs" className="px-3 py-1.5 rounded-xl bg-brand-500 text-white text-xs font-bold hover:bg-brand-600 transition-colors">
                  + Apply Letter of Credit
                </Link>
                <Link to="/bgs" className="px-3 py-1.5 rounded-xl bg-emerald-500 text-white text-xs font-bold hover:bg-emerald-600 transition-colors">
                  + Issue Guarantee
                </Link>
              </div>
            </div>

            <div className="space-y-4">
              {facilities.length === 0 ? (
                <div className="py-10 text-center text-slate-400 text-xs">No active credit facilities assigned.</div>
              ) : (
                facilities.map(f => {
                  const rate = f.limitAmount > 0 
                    ? ((f.utilizedAmount / f.limitAmount) * 100)
                    : 0;
                  const themeColor = f.facilityType === 'LETTER_OF_CREDIT_FACILITY' 
                    ? 'brand' 
                    : f.facilityType === 'GUARANTEE_FACILITY'
                    ? 'emerald'
                    : 'indigo';
                  const dotClass = f.facilityType === 'LETTER_OF_CREDIT_FACILITY' 
                    ? 'bg-brand-500' 
                    : f.facilityType === 'GUARANTEE_FACILITY'
                    ? 'bg-emerald-500'
                    : 'bg-indigo-500';
                  const progressClass = f.facilityType === 'LETTER_OF_CREDIT_FACILITY' 
                    ? 'bg-brand-500' 
                    : f.facilityType === 'GUARANTEE_FACILITY'
                    ? 'bg-emerald-500'
                    : 'bg-indigo-500';
                  return (
                    <div key={f.id} className="p-4 rounded-2xl dark:bg-slate-900/40 bg-slate-50 border dark:border-slate-800 border-slate-100">
                      <div className="flex justify-between items-center mb-2">
                        <div className="flex items-center gap-2">
                          <span className={`h-2 w-2 rounded-full ${dotClass}`}></span>
                          <span className="text-xs font-bold uppercase tracking-wider">
                            {f.facilityType?.replace('_FACILITY', '').replace('_', ' ')}
                          </span>
                        </div>
                        <span className="text-xs font-semibold">{rate.toFixed(1)}% Utilized</span>
                      </div>
                      <div className="w-full bg-slate-200 dark:bg-slate-800 h-1.5 rounded-full overflow-hidden">
                        <div className={`h-full ${progressClass}`} style={{ width: `${rate}%` }}></div>
                      </div>
                      <div className="flex justify-between text-[10px] text-slate-400 mt-1">
                        <span>{formatCurrency(f.utilizedAmount)} Utilized</span>
                        <span>{formatCurrency(f.limitAmount)} Active Facility Limit</span>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          <div className="glass-card-light dark:glass-card-dark p-6 rounded-3xl space-y-4">
            <h4 className="font-bold text-lg border-b dark:border-slate-800 pb-3">Quick Actions</h4>
            <div className="grid grid-cols-1 gap-2.5">
              <Link to="/lcs" className="p-3 rounded-2xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950/20 bg-white hover:border-brand-500/50 transition-colors flex items-center justify-between text-xs font-semibold group">
                <span>Presentation of drawings</span>
                <ArrowUpRight className="h-4 w-4 text-slate-400 group-hover:text-brand-500" />
              </Link>
              <Link to="/bills" className="p-3 rounded-2xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950/20 bg-white hover:border-brand-500/50 transition-colors flex items-center justify-between text-xs font-semibold group">
                <span>Register documentary collection</span>
                <ArrowUpRight className="h-4 w-4 text-slate-400 group-hover:text-brand-500" />
              </Link>
            </div>
            
            <div className="p-4 rounded-2xl bg-brand-500/5 border border-brand-500/10 text-xs">
              <h5 className="font-bold text-brand-500 dark:text-brand-400 mb-1">Treasury Advisory</h5>
              <p className="text-slate-500 leading-relaxed">
                Your revolving line expiry of 2028-01-15 requires regulatory review within 6 months.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* OPERATIONS OFFICER VIEW */}
      {user?.role === 'OPERATIONS' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="glass-card-light dark:glass-card-dark p-6 rounded-3xl lg:col-span-2 space-y-4">
            <div className="flex items-center justify-between border-b dark:border-slate-800 pb-3">
              <div>
                <h4 className="font-bold text-lg">Operations Processing Queue</h4>
                <p className="text-xs text-slate-400">Maker-Checker approvals for active drawings &amp; amendments</p>
              </div>
              <span className="h-6 px-2.5 rounded-full dark:bg-slate-900 bg-slate-100 flex items-center justify-center text-[10px] font-bold text-brand-500">2 Actions Required</span>
            </div>

            <div className="space-y-2.5">
              <div className="p-3 rounded-2xl border border-amber-500/20 bg-amber-500/5 flex flex-col md:flex-row md:items-center justify-between gap-3 text-xs">
                <div>
                  <div className="flex items-center gap-2 mb-0.5">
                    <span className="font-bold">Drawing: DRW-LC001-B</span>
                    <span className="h-5 px-2 rounded-full bg-amber-500/10 text-amber-500 text-[9px] font-black">DISCREPANCY DETECTED</span>
                  </div>
                  <p className="text-slate-400">Acme Industrial Sight draft commercial invoice discrepancy check.</p>
                </div>
                <Link to="/lcs" className="px-3.5 py-1.5 rounded-xl bg-amber-500 text-white font-bold text-[11px] self-start md:self-auto hover:bg-amber-600 transition-colors">
                  Investigate
                </Link>
              </div>

              <div className="p-3 rounded-2xl border border-brand-500/20 bg-brand-500/5 flex flex-col md:flex-row md:items-center justify-between gap-3 text-xs">
                <div>
                  <div className="flex items-center gap-2 mb-0.5">
                    <span className="font-bold">Amendment: LC-2026-0005</span>
                    <span className="h-5 px-2 rounded-full bg-brand-500/10 text-brand-500 text-[9px] font-black">PENDING APPROVAL</span>
                  </div>
                  <p className="text-slate-400">Client requested machine supply order extension to $12,000,000.</p>
                </div>
                <Link to="/lcs" className="px-3.5 py-1.5 rounded-xl bg-brand-500 text-white font-bold text-[11px] self-start md:self-auto hover:bg-brand-600 transition-colors">
                  Maker Check
                </Link>
              </div>
            </div>
          </div>

          <div className="glass-card-light dark:glass-card-dark p-6 rounded-3xl space-y-4">
            <h4 className="font-bold text-lg border-b dark:border-slate-800 pb-3">Quick Navigation</h4>
            <div className="grid grid-cols-2 gap-2 text-center">
              <Link to="/lcs" className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-900 border dark:border-slate-800 hover:border-brand-500 transition-colors text-xs font-bold flex flex-col items-center gap-2">
                <FileText className="h-5 w-5 text-brand-500" />
                <span>Review LCs</span>
              </Link>
              <Link to="/bgs" className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-900 border dark:border-slate-800 hover:border-brand-500 transition-colors text-xs font-bold flex flex-col items-center gap-2">
                <Award className="h-5 w-5 text-emerald-500" />
                <span>Review BGs</span>
              </Link>
            </div>
          </div>
        </div>
      )}

      {/* RELATIONSHIP MANAGER VIEW */}
      {user?.role === 'RELATIONSHIP_MANAGER' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="glass-card-light dark:glass-card-dark p-6 rounded-3xl lg:col-span-2 space-y-4">
            <h4 className="font-bold text-lg border-b dark:border-slate-800 pb-3">Client Portfolio Limits</h4>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="text-slate-400 font-bold border-b dark:border-slate-800 pb-2">
                    <th className="py-2">Client Company</th>
                    <th>Region</th>
                    <th>Credit limit</th>
                    <th>Active Exposure</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {portfolio.map(client => (
                    <tr key={client.id} className="border-b dark:border-slate-900/60 py-2">
                      <td className="font-bold py-3">{client.name}</td>
                      <td>{client.country}</td>
                      <td>{formatCurrency(client.limit)}</td>
                      <td>{formatCurrency(client.exposure)}</td>
                      <td>
                        <span className="px-2 py-0.5 rounded-md bg-emerald-500/10 text-emerald-500 text-[10px] font-bold">{client.status}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="glass-card-light dark:glass-card-dark p-6 rounded-3xl space-y-4">
            <h4 className="font-bold text-lg border-b dark:border-slate-800 pb-3">Portfolio Controls</h4>
            <div className="p-3.5 rounded-2xl bg-indigo-500/5 border border-indigo-500/10 text-xs space-y-2">
              <h5 className="font-bold text-indigo-500 dark:text-indigo-400">Limit Increase Triggered</h5>
              <p className="text-slate-400 leading-relaxed">
                Nexus Electronics is utilizing 30% of their overall letter of credit limit. Trigger collateral assessment?
              </p>
              <button 
                onClick={() => { setIsEvalOpen(true); setEvalStep('review'); }}
                className="px-3 py-1 bg-indigo-500 hover:bg-indigo-600 text-white font-bold rounded-lg text-[10px] transition-colors"
              >
                Run Evaluation
              </button>
            </div>
          </div>
        </div>
      )}

      {/* COMPLIANCE OFFICER VIEW */}
      {user?.role === 'COMPLIANCE' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="glass-card-light dark:glass-card-dark p-6 rounded-3xl lg:col-span-2 space-y-4">
            <div className="flex items-center justify-between border-b dark:border-slate-800 pb-3">
              <div>
                <h4 className="font-bold text-lg">Active Watchlist Case Cases</h4>
                <p className="text-xs text-slate-400">Sanctions screenings triggered on high-risk name variations</p>
              </div>
              <Link to="/compliance" className="text-xs text-brand-500 hover:underline">View Registry</Link>
            </div>

            <div className="space-y-3">
              {cases.map(item => (
                <div key={item.id} className="p-3 rounded-2xl border dark:border-slate-800 border-slate-100 dark:bg-slate-950/20 bg-slate-50 flex items-center justify-between text-xs gap-3">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-extrabold text-[12px]">{item.screening?.entityName}</span>
                      <span className="h-5 px-1.5 rounded bg-rose-500/15 text-rose-500 text-[9px] font-black">MATCH SCORE: {item.screening?.matchScore}%</span>
                    </div>
                    <p className="text-slate-400 font-medium">Flagged source: {item.screening?.watchlistSource}. Case status: {item.caseStatus}</p>
                  </div>
                  <Link to="/compliance" className="px-3 py-1.5 rounded-xl bg-rose-500 hover:bg-rose-600 text-white text-[11px] font-bold transition-colors">
                    Investigate
                  </Link>
                </div>
              ))}
            </div>
          </div>

          <div className="glass-card-light dark:glass-card-dark p-6 rounded-3xl space-y-4">
            <h4 className="font-bold text-lg border-b dark:border-slate-800 pb-3">Watchlist Match Score Distribution</h4>
            <div className="h-44">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={complianceMatchStats}>
                  <XAxis dataKey="range" stroke="#94a3b8" fontSize={10} />
                  <Tooltip cursor={{ fill: 'transparent' }} />
                  <Bar dataKey="count" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}

      {/* SYSTEM ADMIN VIEW */}
      {user?.role === 'ADMIN' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="glass-card-light dark:glass-card-dark p-6 rounded-3xl lg:col-span-2 space-y-4">
            <div className="flex items-center justify-between border-b dark:border-slate-800 pb-3">
              <div>
                <h4 className="font-bold text-lg">System Audit Feed Ledger</h4>
                <p className="text-xs text-slate-400">Chronological system events and action tags</p>
              </div>
              <Link to="/audit-logs" className="text-xs text-brand-500 hover:underline">Full Ledger</Link>
            </div>

            <div className="space-y-3 font-mono">
              {auditLogs.map((log) => (
                <div key={log.id} className="p-3 rounded-2xl border dark:border-slate-800 border-slate-100 dark:bg-slate-950/20 bg-slate-50 flex items-center justify-between text-xs gap-3">
                  <div className="overflow-hidden">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-bold text-slate-600 dark:text-brand-400">[{log.username}]</span>
                      <span className="font-bold dark:text-slate-200 truncate">{log.action}</span>
                    </div>
                    <p className="text-slate-400 font-sans text-[11px] truncate">{log.details}</p>
                  </div>
                  <span className="text-[10px] text-slate-400">{new Date(log.timestamp).toLocaleTimeString()}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="glass-card-light dark:glass-card-dark p-6 rounded-3xl space-y-4">
            <h4 className="font-bold text-lg border-b dark:border-slate-800 pb-3">Vault System Health</h4>
            <div className="space-y-3.5 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-slate-400 font-semibold flex items-center gap-1.5">
                  <Database className="h-4 w-4 text-emerald-500" /> MySQL Database Connection
                </span>
                <span className="h-5 px-2 rounded-full bg-emerald-500/10 text-emerald-500 font-bold text-[10px]">CONNECTED</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-400 font-semibold flex items-center gap-1.5">
                  <ShieldCheck className="h-4 w-4 text-brand-500" /> Security Provider JWT HS512
                </span>
                <span className="h-5 px-2 rounded-full bg-brand-500/10 text-brand-500 font-bold text-[10px]">ENCRYPTED</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TREASURY AND GLOBAL VISUALIZATIONS (SHOWN BY DEFAULT FOR OTHER ROLES E.G. TREASURY / GENERAL VIEWER) */}
      {(user?.role === 'TREASURY' || user?.role === 'ADMIN' || user?.role === 'OPERATIONS') && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Chart Area */}
          <div className="glass-card-light dark:glass-card-dark p-6 rounded-3xl lg:col-span-2 space-y-4">
            <div>
              <h4 className="font-bold text-lg">Active Exposure Trajectory</h4>
              <p className="text-xs text-slate-400">Total utilized banking exposure in millions (USD)</p>
            </div>
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={exposureTrend} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorLcs" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#4a6be9" stopOpacity={0.4}/>
                      <stop offset="95%" stopColor="#4a6be9" stopOpacity={0}/>
                    </linearGradient>
                    <linearGradient id="colorBgs" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.4}/>
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke={isDark ? '#1e293b' : '#e2e8f0'} />
                  <XAxis dataKey="month" stroke="#94a3b8" fontSize={11} />
                  <YAxis stroke="#94a3b8" fontSize={11} />
                  <Tooltip contentStyle={{ backgroundColor: isDark ? '#0b1329' : '#ffffff', borderColor: isDark ? '#1e293b' : '#cbd5e1' }} />
                  <Legend verticalAlign="top" height={36}/>
                  <Area type="monotone" dataKey="LCs" stroke="#4a6be9" fillOpacity={1} fill="url(#colorLcs)" strokeWidth={2} />
                  <Area type="monotone" dataKey="BGs" stroke="#10b981" fillOpacity={1} fill="url(#colorBgs)" strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Instrument Distribution Chart */}
          <div className="glass-card-light dark:glass-card-dark p-6 rounded-3xl space-y-4">
            <div>
              <h4 className="font-bold text-lg">Instrument Exposure Breakdown</h4>
              <p className="text-xs text-slate-400">Percentage distribution of active instruments</p>
            </div>
            <div className="h-56 relative flex items-center justify-center">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={instrumentDistribution}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={80}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {instrumentDistribution.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => formatCurrency(value)} />
                </PieChart>
              </ResponsiveContainer>
              <div className="absolute flex flex-col items-center justify-center text-center">
                <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-widest">Active Exposure</span>
                <span className="text-lg font-black tracking-tight mt-0.5">{formatCurrency(stats?.totalExposure)}</span>
              </div>
            </div>

            <div className="space-y-2 text-[11px]">
              {instrumentDistribution.map(item => (
                <div key={item.name} className="flex justify-between items-center font-bold">
                  <div className="flex items-center gap-1.5">
                    <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: item.color }}></span>
                    <span className="text-slate-400 font-semibold">{item.name}</span>
                  </div>
                  <span>{formatCurrency(item.value)}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* EVALUATION MODAL */}
      <AnimatePresence>
        {isEvalOpen && (
          <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ scale: 0.95, opacity: 0, y: 10 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.95, opacity: 0, y: 10 }}
              transition={{ duration: 0.2, ease: 'easeOut' }}
              className="glass-card-light dark:glass-card-dark max-w-lg w-full rounded-3xl p-6 relative overflow-hidden border border-slate-200 dark:border-slate-800 shadow-2xl space-y-6"
            >
              {/* Header */}
              <div className="flex justify-between items-center border-b dark:border-slate-800 pb-3">
                <div className="flex items-center gap-2">
                  <Landmark className="h-5 w-5 text-indigo-500" />
                  <h3 className="font-extrabold text-lg tracking-tight">Collateral Risk Evaluation</h3>
                </div>
                <button
                  onClick={() => setIsEvalOpen(false)}
                  className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              {evalStep === 'review' && (
                <div className="space-y-4">
                  {/* Client Snapshot */}
                  <div className="bg-slate-50 dark:bg-slate-900/60 p-4 rounded-2xl border dark:border-slate-800/80 border-slate-100 space-y-3">
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-400 font-semibold">Client Company:</span>
                      <span className="font-bold">Nexus Electronics Corp (Singapore)</span>
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-400 font-semibold">Current Credit Limit:</span>
                      <span className="font-bold">{formatCurrency(portfolio.find(c => c.id === 2)?.limit || 80000000)}</span>
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-400 font-semibold">Active Exposure:</span>
                      <span className="font-bold text-indigo-500">{formatCurrency(24000000)}</span>
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-400 font-semibold">Utilization Rate:</span>
                      <span className="font-bold text-amber-500">30.0%</span>
                    </div>
                  </div>

                  {/* Collateral Metrics */}
                  <div className="space-y-2">
                    <h5 className="font-bold text-xs uppercase tracking-wider text-slate-400">Collateral Coverage Breakdown</h5>
                    <div className="grid grid-cols-2 gap-3 text-xs">
                      <div className="p-3 bg-emerald-500/5 border border-emerald-500/10 rounded-xl">
                        <span className="text-slate-400 font-medium">Cash Reserves</span>
                        <div className="font-bold text-emerald-500 mt-0.5">$12,500,000</div>
                      </div>
                      <div className="p-3 bg-brand-500/5 border border-brand-500/10 rounded-xl">
                        <span className="text-slate-400 font-medium">Accounts Receivable</span>
                        <div className="font-bold text-brand-500 mt-0.5">$45,000,000</div>
                      </div>
                    </div>

                    <div className="p-3 bg-slate-50 dark:bg-slate-900/40 border dark:border-slate-800 rounded-xl flex justify-between items-center text-xs">
                      <div>
                        <span className="text-slate-400 font-medium">Total Collateral Value</span>
                        <div className="font-black text-sm mt-0.5">$57,500,000</div>
                      </div>
                      <div className="text-right">
                        <span className="text-slate-400 font-medium">Coverage Ratio</span>
                        <div className="font-black text-emerald-500 text-sm mt-0.5">2.39x</div>
                      </div>
                    </div>
                  </div>

                  {/* System Recommendation */}
                  <div className="p-3.5 rounded-2xl bg-emerald-500/10 dark:bg-emerald-500/5 border border-emerald-500/20 text-xs flex gap-2">
                    <ShieldCheck className="h-5 w-5 text-emerald-500 shrink-0" />
                    <div>
                      <h6 className="font-bold text-emerald-600 dark:text-emerald-400">System Recommendation: 🟢 LOW RISK</h6>
                      <p className="text-slate-500 dark:text-slate-400 leading-relaxed mt-0.5">
                        Nexus Electronics maintains collateral coverage well above the 1.50x threshold. Credit quality is stable; recommend approving up to a 15% limit extension if requested.
                      </p>
                    </div>
                  </div>

                  {/* Footer Actions */}
                  <div className="flex flex-col sm:flex-row gap-2 pt-2">
                    <button
                      onClick={() => {
                        setPortfolio(prev => prev.map(c => c.id === 2 ? { ...c, limit: c.limit * 1.1 } : c));
                        setEvalStep('success-limit');
                      }}
                      className="flex-1 py-2 px-3 rounded-xl bg-indigo-500 hover:bg-indigo-600 text-white font-bold text-xs transition-colors flex items-center justify-center gap-1.5"
                    >
                      <TrendingUp className="h-4 w-4" /> Approve Temp 10% Increase
                    </button>
                    <button
                      onClick={() => {
                        setEvalStep('success-verify');
                      }}
                      className="flex-1 py-2 px-3 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950/20 bg-white hover:bg-slate-50 dark:hover:bg-slate-800 font-bold text-xs transition-colors flex items-center justify-center gap-1.5"
                    >
                      <CheckCircle className="h-4 w-4 text-emerald-500" /> Log Collateral Verified
                    </button>
                  </div>
                </div>
              )}

              {(evalStep === 'success-limit' || evalStep === 'success-verify') && (
                <div className="text-center py-6 space-y-4">
                  <div className="h-16 w-16 mx-auto rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-500">
                    <CheckCircle className="h-8 w-8" />
                  </div>
                  <div>
                    <h4 className="font-extrabold text-lg">
                      {evalStep === 'success-limit' ? 'Credit Limit Increased!' : 'Evaluation Completed!'}
                    </h4>
                    <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto leading-relaxed">
                      {evalStep === 'success-limit' 
                        ? 'Temporary credit limit increase of 10% has been successfully applied to Nexus Electronics Corp. New limit is $88,000,000.'
                        : 'Collateral evaluation has been successfully logged. Accounts Receivable and Cash reserves are verified as compliant.'
                      }
                    </p>
                  </div>
                  <button
                    onClick={() => setIsEvalOpen(false)}
                    className="px-6 py-2 bg-indigo-500 hover:bg-indigo-600 text-white font-bold text-xs rounded-xl transition-colors"
                  >
                    Done
                  </button>
                </div>
              )}
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default Dashboard;
