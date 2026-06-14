import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, FileText, ShieldAlert, Award, FileSpreadsheet, 
  BarChart3, History, Bell, LogOut, Sun, Moon, ShieldCheck, User2, Menu, X, UserCheck, Building2
} from 'lucide-react';
import api from '../services/api';

const Layout = ({ children }) => {
  const { user, logout, isAdmin, isCompliance, isTreasury, isOps, isClient } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  
  const [darkMode, setDarkMode] = useState(() => {
    return localStorage.getItem('theme') === 'dark' || 
      (!localStorage.getItem('theme') && window.matchMedia('(prefers-color-scheme: dark)').matches);
  });
  
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [notifications, setNotifications] = useState([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  // Apply Theme
  useEffect(() => {
    if (darkMode) {
      document.body.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      document.body.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }
  }, [darkMode]);

  // Fetch Notifications
  useEffect(() => {
    if (user) {
      const fetchAlerts = async () => {
        try {
          const res = await api.get(`/notifications/user/${user.id}`);
          setNotifications(res.data.data || []);
          setUnreadCount(res.data.data?.filter(n => !n.isRead).length || 0);
        } catch (e) {
          console.error(e);
        }
      };
      fetchAlerts();
      const interval = setInterval(fetchAlerts, 10000);
      return () => clearInterval(interval);
    }
  }, [user]);

  const markAllAsRead = async () => {
    try {
      const unread = notifications.filter(n => !n.isRead);
      for (const n of unread) {
        await api.put(`/notifications/${n.id}/read`);
      }
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (e) {
      console.error(e);
    }
  };

  const menuItems = [
    { name: 'Dashboard',           icon: LayoutDashboard, path: '/',                roles: ['CLIENT', 'OPERATIONS', 'RELATIONSHIP_MANAGER', 'TREASURY', 'COMPLIANCE', 'ADMIN'] },
    { name: 'Letters of Credit',   icon: FileText,        path: '/lcs',             roles: ['CLIENT', 'OPERATIONS', 'RELATIONSHIP_MANAGER', 'TREASURY', 'COMPLIANCE', 'ADMIN'] },
    { name: 'Bank Guarantees',     icon: Award,           path: '/bgs',             roles: ['CLIENT', 'OPERATIONS', 'RELATIONSHIP_MANAGER', 'TREASURY', 'COMPLIANCE', 'ADMIN'] },
    { name: 'Export & Collections',icon: FileSpreadsheet, path: '/bills',           roles: ['CLIENT', 'OPERATIONS', 'ADMIN', 'RELATIONSHIP_MANAGER'] },
    { name: 'Compliance Registry', icon: ShieldAlert,     path: '/compliance',      roles: ['COMPLIANCE', 'ADMIN'] },
    { name: 'Reports & Analytics', icon: BarChart3,       path: '/reports',         roles: ['CLIENT', 'TREASURY', 'OPERATIONS', 'ADMIN'] },
    { name: 'Notifications',       icon: Bell,            path: '/notifications',   roles: ['CLIENT', 'OPERATIONS', 'RELATIONSHIP_MANAGER', 'TREASURY', 'COMPLIANCE', 'ADMIN'] },
    { name: 'Audit Ledger',        icon: History,         path: '/audit-logs',      roles: ['ADMIN'] },
    { name: 'Corporate Clients',   icon: Building2,       path: '/corporates',      roles: ['ADMIN', 'RELATIONSHIP_MANAGER'] },
    { name: 'User Control',        icon: UserCheck,       path: '/user-management', roles: ['ADMIN'] },
  ];

  const filteredMenuItems = menuItems.filter(item => item.roles.includes(user?.role));

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen flex transition-colors duration-300 dark:bg-slate-950 bg-slate-50">
      {/* Background Decorative Blur Gradients */}
      <div className="glow-bg top-0 left-0 bg-brand-400 opacity-10 dark:opacity-20"></div>
      <div className="glow-bg bottom-0 right-0 bg-blue-500 opacity-10 dark:opacity-10"></div>

      {/* Sidebar navigation */}
      <aside className={`fixed lg:static inset-y-0 left-0 z-40 w-64 transform ${sidebarOpen ? 'translate-x-0' : '-translate-x-0'} transition-transform duration-300 lg:translate-x-0 border-r dark:border-slate-900 border-slate-200 dark:bg-slate-900/80 bg-white/80 backdrop-blur-md flex flex-col`}>
        {/* Sidebar Header Title */}
        <div className="h-16 flex items-center justify-between px-6 border-b dark:border-slate-900 border-slate-200">
          <Link to="/" className="flex items-center gap-2">
            <ShieldCheck className="h-8 w-8 text-brand-500 animate-pulse" />
            <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-brand-600 to-brand-400 bg-clip-text text-transparent dark:from-white dark:to-brand-300">
              TRADEVAULT
            </span>
          </Link>
          <button className="lg:hidden" onClick={() => setSidebarOpen(false)}>
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* User Card inside Sidebar */}
        <Link 
          to="/profile"
          className="p-4 border-b dark:border-slate-900 border-slate-200 bg-slate-50/50 dark:bg-slate-900/50 hover:bg-slate-100/50 dark:hover:bg-slate-800/50 transition-colors flex items-center justify-between group"
        >
          <div className="flex items-center gap-3 overflow-hidden">
            <div className="h-10 w-10 rounded-xl bg-brand-500/10 flex items-center justify-center border border-brand-500/30 group-hover:bg-brand-500/20 transition-colors">
              <User2 className="h-5 w-5 text-brand-500" />
            </div>
            <div className="overflow-hidden">
              <h4 className="font-semibold text-sm truncate group-hover:text-brand-500 transition-colors">{user?.fullName}</h4>
              <p className="text-xs dark:text-brand-400 text-brand-600 font-medium tracking-wide truncate">{user?.role}</p>
            </div>
          </div>
        </Link>

        {/* Menu Items */}
        <nav className="flex-1 px-4 py-4 space-y-1 overflow-y-auto">
          {filteredMenuItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.name}
                to={item.path}
                className={`flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-200 ${
                  isActive 
                    ? 'bg-brand-500 text-white shadow-lg shadow-brand-500/25 dark:shadow-brand-500/10'
                    : 'dark:text-slate-400 dark:hover:bg-slate-800/60 dark:hover:text-white text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                }`}
              >
                <Icon className="h-5 w-5" />
                {item.name}
              </Link>
            );
          })}
        </nav>

        {/* Sidebar Footer: Theme Toggle & Logout */}
        <div className="p-4 border-t dark:border-slate-900 border-slate-200 space-y-2">
          <button 
            onClick={() => setDarkMode(!darkMode)}
            className="w-full flex items-center justify-between px-4 py-2.5 rounded-xl text-sm font-medium dark:text-slate-400 text-slate-600 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <div className="flex items-center gap-3">
              {darkMode ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
              <span>{darkMode ? 'Light Theme' : 'Dark Theme'}</span>
            </div>
          </button>
          
          <button 
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium text-rose-500 hover:bg-rose-500/5 transition-colors"
          >
            <LogOut className="h-5 w-5" />
            Logout
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden relative">
        {/* Top Navbar */}
        <header className="h-16 border-b dark:border-slate-900 border-slate-200 dark:bg-slate-950/80 bg-white/80 backdrop-blur-md flex items-center justify-between px-6 z-30">
          <div className="flex items-center gap-4">
            <button className="lg:hidden p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800" onClick={() => setSidebarOpen(true)}>
              <Menu className="h-6 w-6" />
            </button>
            <h2 className="hidden md:block font-bold text-lg tracking-wide text-slate-700 dark:text-slate-300">
              Enterprise Operations Portal
            </h2>
          </div>

          <div className="flex items-center gap-4 relative">
            {/* Bell icon — navigates to Notifications page */}
            <button 
              onClick={() => navigate('/notifications')}
              className="p-2 rounded-xl relative border dark:border-slate-900 border-slate-200 dark:hover:bg-slate-900 hover:bg-slate-100 transition-colors"
              title="View Notifications"
            >
              <Bell className="h-5 w-5 text-slate-600 dark:text-slate-400" />
              {unreadCount > 0 && (
                <span className="absolute -top-1.5 -right-1.5 h-5 w-5 bg-rose-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center animate-bounce border-2 border-white dark:border-slate-950">
                  {unreadCount}
                </span>
              )}
            </button>
          </div>
        </header>

        {/* Nested Content Pages */}
        <main className="flex-1 overflow-y-auto p-6 md:p-8 relative z-10">
          {children}
        </main>
      </div>
    </div>
  );
};

export default Layout;
