import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Bell, BellOff, CheckCheck, Check, Filter,
  Info, AlertTriangle, AlertCircle, CheckCircle2,
  RefreshCw, Inbox, Clock
} from 'lucide-react';

// ── Category config ────────────────────────────────────────────────────────
const CATEGORY_CONFIG = {
  INFO:    { icon: Info,          color: 'text-blue-400',   bg: 'bg-blue-500/10',   border: 'border-blue-500/30',   dot: 'bg-blue-400'   },
  ALERT:   { icon: AlertCircle,   color: 'text-rose-400',   bg: 'bg-rose-500/10',   border: 'border-rose-500/30',   dot: 'bg-rose-400'   },
  WARNING: { icon: AlertTriangle, color: 'text-amber-400',  bg: 'bg-amber-500/10',  border: 'border-amber-500/30',  dot: 'bg-amber-400'  },
  SUCCESS: { icon: CheckCircle2,  color: 'text-emerald-400',bg: 'bg-emerald-500/10',border: 'border-emerald-500/30',dot: 'bg-emerald-400' },
};

const DEFAULT_CAT = CATEGORY_CONFIG.INFO;

const getConfig = (type) => CATEGORY_CONFIG[type?.toUpperCase()] || DEFAULT_CAT;

// ── Relative time helper ───────────────────────────────────────────────────
const timeAgo = (dateStr) => {
  if (!dateStr) return '';
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins  = Math.floor(diff / 60000);
  const hours = Math.floor(mins  / 60);
  const days  = Math.floor(hours / 24);
  if (days  > 0) return `${days}d ago`;
  if (hours > 0) return `${hours}h ago`;
  if (mins  > 0) return `${mins}m ago`;
  return 'Just now';
};

// ── Filter tabs ────────────────────────────────────────────────────────────
const FILTERS = ['All', 'Unread', 'Read'];

// ── Main Component ─────────────────────────────────────────────────────────
const Notifications = () => {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading]             = useState(true);
  const [markingId, setMarkingId]         = useState(null);
  const [markingAll, setMarkingAll]       = useState(false);
  const [filter, setFilter]               = useState('All');
  const [refreshing, setRefreshing]       = useState(false);

  // ── Fetch ────────────────────────────────────────────────────────────────
  const fetchNotifications = useCallback(async (silent = false) => {
    if (!user?.id) return;
    if (!silent) setLoading(true);
    else         setRefreshing(true);
    try {
      const res = await api.get(`/notifications/user/${user.id}`);
      setNotifications(res.data.data || []);
    } catch (e) {
      console.error('Failed to load notifications', e);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [user?.id]);

  useEffect(() => { fetchNotifications(); }, [fetchNotifications]);

  // ── Mark single as read ──────────────────────────────────────────────────
  const markRead = async (id) => {
    setMarkingId(id);
    try {
      await api.put(`/notifications/${id}/read`);
      setNotifications(prev =>
        prev.map(n => n.id === id ? { ...n, isRead: true } : n)
      );
    } catch (e) {
      console.error('Failed to mark notification as read', e);
    } finally {
      setMarkingId(null);
    }
  };

  // ── Mark all as read ─────────────────────────────────────────────────────
  const markAllRead = async () => {
    setMarkingAll(true);
    try {
      const unread = notifications.filter(n => !n.isRead);
      await Promise.all(unread.map(n => api.put(`/notifications/${n.id}/read`)));
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    } catch (e) {
      console.error('Failed to mark all as read', e);
    } finally {
      setMarkingAll(false);
    }
  };

  // ── Filtered list ────────────────────────────────────────────────────────
  const displayed = notifications.filter(n => {
    if (filter === 'Unread') return !n.isRead;
    if (filter === 'Read')   return  n.isRead;
    return true;
  });

  const unreadCount = notifications.filter(n => !n.isRead).length;

  // ── Skeleton loader ──────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="space-y-4 animate-pulse">
        <div className="h-10 w-64 rounded-xl dark:bg-slate-800 bg-slate-200" />
        {[...Array(5)].map((_, i) => (
          <div key={i} className="h-20 rounded-2xl dark:bg-slate-800/60 bg-slate-200/60" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-3xl mx-auto">

      {/* ── Header ── */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight flex items-center gap-2">
            <Bell className="h-6 w-6 text-brand-500" />
            Notification Centre
            {unreadCount > 0 && (
              <span className="ml-1 inline-flex items-center justify-center h-6 min-w-6 px-1.5 rounded-full bg-rose-500 text-white text-xs font-bold animate-bounce">
                {unreadCount}
              </span>
            )}
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            System alerts, trade events, and compliance notifications
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => fetchNotifications(true)}
            disabled={refreshing}
            className="p-2.5 rounded-xl border dark:border-slate-800 border-slate-200 dark:bg-slate-950/20 bg-white hover:bg-slate-100 transition-colors disabled:opacity-50"
            title="Refresh"
          >
            <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
          </button>
          {unreadCount > 0 && (
            <button
              onClick={markAllRead}
              disabled={markingAll}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-brand-500 hover:bg-brand-600 text-white font-bold text-xs transition-all disabled:opacity-60"
            >
              <CheckCheck className="h-4 w-4" />
              {markingAll ? 'Marking…' : 'Mark All Read'}
            </button>
          )}
        </div>
      </div>

      {/* ── Stats Row ── */}
      <div className="grid grid-cols-3 gap-4">
        {[
          { label: 'Total',  value: notifications.length,                    color: 'text-slate-400' },
          { label: 'Unread', value: unreadCount,                             color: 'text-rose-400'  },
          { label: 'Read',   value: notifications.length - unreadCount,      color: 'text-emerald-400' },
        ].map(stat => (
          <div key={stat.label} className="glass-card-light dark:glass-card-dark p-4 rounded-2xl text-center">
            <div className={`text-2xl font-black ${stat.color}`}>{stat.value}</div>
            <div className="text-[10px] uppercase font-bold text-slate-400 mt-0.5">{stat.label}</div>
          </div>
        ))}
      </div>

      {/* ── Filter Tabs ── */}
      <div className="flex items-center gap-1 p-1 rounded-xl dark:bg-slate-900 bg-slate-100 w-fit">
        <Filter className="h-3.5 w-3.5 text-slate-400 ml-2 mr-1" />
        {FILTERS.map(f => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              filter === f
                ? 'bg-brand-500 text-white shadow'
                : 'text-slate-500 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            {f}
          </button>
        ))}
      </div>

      {/* ── Notification List ── */}
      <AnimatePresence mode="popLayout">
        {displayed.length === 0 ? (
          <motion.div
            key="empty"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            className="flex flex-col items-center justify-center py-20 text-slate-400 gap-3"
          >
            <Inbox className="h-12 w-12 opacity-30" />
            <p className="text-sm font-medium">
              {filter === 'All' ? 'No notifications yet' : `No ${filter.toLowerCase()} notifications`}
            </p>
          </motion.div>
        ) : (
          displayed.map((n, idx) => {
            const cfg = getConfig(n.category || n.type);
            const Icon = cfg.icon;
            const isUnread = !n.isRead;

            return (
              <motion.div
                key={n.id}
                layout
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ delay: idx * 0.04, duration: 0.3 }}
                className={`relative p-4 rounded-2xl border transition-all duration-300 ${
                  isUnread
                    ? `${cfg.bg} ${cfg.border}`
                    : 'dark:bg-slate-900/30 bg-white border-slate-100 dark:border-slate-800/50'
                }`}
              >
                {/* Unread indicator bar */}
                {isUnread && (
                  <span className={`absolute left-0 top-4 bottom-4 w-1 rounded-full ${cfg.dot}`} />
                )}

                <div className="flex items-start gap-4 pl-1">
                  {/* Icon */}
                  <div className={`mt-0.5 p-2 rounded-xl ${cfg.bg} border ${cfg.border} flex-shrink-0`}>
                    <Icon className={`h-4 w-4 ${cfg.color}`} />
                  </div>

                  {/* Content */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <h4 className={`font-bold text-sm leading-tight ${isUnread ? '' : 'text-slate-500 dark:text-slate-400'}`}>
                        {n.title}
                      </h4>
                      <div className="flex items-center gap-2 flex-shrink-0">
                        {/* Timestamp */}
                        {(n.createdAt || n.timestamp) && (
                          <span className="flex items-center gap-1 text-[10px] text-slate-400">
                            <Clock className="h-3 w-3" />
                            {timeAgo(n.createdAt || n.timestamp)}
                          </span>
                        )}
                        {/* Type badge */}
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wide ${cfg.bg} ${cfg.color}`}>
                          {n.category || n.type || 'INFO'}
                        </span>
                      </div>
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 leading-relaxed">
                      {n.message}
                    </p>
                  </div>

                  {/* Mark as Read button */}
                  {isUnread && (
                    <button
                      onClick={() => markRead(n.id)}
                      disabled={markingId === n.id}
                      className="flex-shrink-0 p-1.5 rounded-lg dark:hover:bg-slate-800 hover:bg-slate-100 transition-colors disabled:opacity-50"
                      title="Mark as read"
                    >
                      {markingId === n.id
                        ? <RefreshCw className="h-4 w-4 animate-spin text-slate-400" />
                        : <Check className="h-4 w-4 text-slate-400 hover:text-emerald-400" />
                      }
                    </button>
                  )}
                </div>
              </motion.div>
            );
          })
        )}
      </AnimatePresence>

      {/* ── Footer note ── */}
      {displayed.length > 0 && (
        <p className="text-center text-[11px] text-slate-400 pb-4">
          Showing {displayed.length} of {notifications.length} notifications
          {filter !== 'All' && ` · filtered by "${filter}"`}
        </p>
      )}
    </div>
  );
};

export default Notifications;
