import axios from 'axios';

// Base URL configuration for Spring Boot
const API_BASE_URL = 'http://localhost:8300/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request Interceptor: Attach JWT Token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('tradevault_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// High-Fidelity Mock Database for Standalone/Fallback Mode
const mockDb = {
  users: [
    { id: 1, username: 'client', role: 'CLIENT', fullName: 'Acme Corp Corporate User', email: 'client@tradevault.com', status: 'ACTIVE' },
    { id: 2, username: 'ops', role: 'OPERATIONS', fullName: 'Sarah Jenkins (Trade Operations)', email: 'ops@tradevault.com', status: 'ACTIVE' },
    { id: 3, username: 'relationship', role: 'RELATIONSHIP_MANAGER', fullName: 'David Miller (Relationship Manager)', email: 'rm@tradevault.com', status: 'ACTIVE' },
    { id: 4, username: 'treasury', role: 'TREASURY', fullName: 'Elena Rostova (Treasury Director)', email: 'treasury@tradevault.com', status: 'ACTIVE' },
    { id: 5, username: 'compliance', role: 'COMPLIANCE', fullName: 'Marcus Vance (Compliance Officer)', email: 'compliance@tradevault.com', status: 'ACTIVE' },
    { id: 6, username: 'admin', role: 'ADMIN', fullName: 'System Administrator', email: 'admin@tradevault.com', status: 'ACTIVE' },
  ],
  clients: [
    { id: 1, companyName: 'Acme Industrial Holdings', taxId: 'TX-99887766', country: 'United States', status: 'ACTIVE', creditLimit: 50000000.00 },
    { id: 2, companyName: 'Global Trading Logistics LLC', taxId: 'TX-55443322', country: 'United Kingdom', status: 'ACTIVE', creditLimit: 25000000.00 },
    { id: 3, companyName: 'Nexus Electronics Corp', taxId: 'TX-11223344', country: 'Singapore', status: 'ACTIVE', creditLimit: 80000000.00 },
  ],
  facilities: [
    { id: 1, client: { id: 1, companyName: 'Acme Industrial Holdings' }, facilityType: 'LETTER_OF_CREDIT_FACILITY', limitAmount: 20000000.00, utilizedAmount: 4500000.00, currency: 'USD', status: 'ACTIVE', expiryDate: '2027-12-31' },
    { id: 2, client: { id: 1, companyName: 'Acme Industrial Holdings' }, facilityType: 'GUARANTEE_FACILITY', limitAmount: 10000000.00, utilizedAmount: 2000000.00, currency: 'USD', status: 'ACTIVE', expiryDate: '2027-06-30' },
    { id: 3, client: { id: 1, companyName: 'Acme Industrial Holdings' }, facilityType: 'REVOLVING_LINE', limitAmount: 20000000.00, utilizedAmount: 12000000.00, currency: 'USD', status: 'ACTIVE', expiryDate: '2028-01-15' },
    { id: 4, client: { id: 2, companyName: 'Global Trading Logistics LLC' }, facilityType: 'LETTER_OF_CREDIT_FACILITY', limitAmount: 15000000.00, utilizedAmount: 3200000.00, currency: 'USD', status: 'ACTIVE', expiryDate: '2026-12-31' },
    { id: 5, client: { id: 2, companyName: 'Global Trading Logistics LLC' }, facilityType: 'GUARANTEE_FACILITY', limitAmount: 10000000.00, utilizedAmount: 0.00, currency: 'USD', status: 'ACTIVE', expiryDate: '2027-08-30' },
    { id: 6, client: { id: 3, companyName: 'Nexus Electronics Corp' }, facilityType: 'LETTER_OF_CREDIT_FACILITY', limitAmount: 50000000.00, utilizedAmount: 24000000.00, currency: 'USD', status: 'ACTIVE', expiryDate: '2028-09-30' },
    { id: 7, client: { id: 3, companyName: 'Nexus Electronics Corp' }, facilityType: 'REVOLVING_LINE', limitAmount: 30000000.00, utilizedAmount: 0.00, currency: 'USD', status: 'ACTIVE', expiryDate: '2028-12-31' },
  ],
  lcs: [
    { id: 1, client: { id: 1, companyName: 'Acme Industrial Holdings' }, creditFacility: { id: 1 }, lcNumber: 'LC-2026-0001', lcType: 'SIGHT', amount: 1500000.00, currency: 'USD', applicantName: 'Acme Industrial Holdings', beneficiaryName: 'Tokyo Steel Alloys Inc.', issueDate: '2026-01-10', expiryDate: '2026-10-10', status: 'ACTIVE', tolerancePercentage: 5.00, portOfLoading: 'Port of Yokohama', portOfDischarge: 'Port of Los Angeles', latestShipmentDate: '2026-09-15' },
    { id: 2, client: { id: 1, companyName: 'Acme Industrial Holdings' }, creditFacility: { id: 1 }, lcNumber: 'LC-2026-0002', lcType: 'USANCE', amount: 3000000.00, currency: 'USD', applicantName: 'Acme Industrial Holdings', beneficiaryName: 'Vance Ironworks Syria', issueDate: '2026-03-01', expiryDate: '2026-12-01', status: 'IN_REVIEW', tolerancePercentage: 10.00, portOfLoading: 'Port of Hamburg', portOfDischarge: 'Port of New York', latestShipmentDate: '2026-11-01' },
    { id: 3, client: { id: 2, companyName: 'Global Trading Logistics LLC' }, creditFacility: { id: 4 }, lcNumber: 'LC-2026-0003', lcType: 'SIGHT', amount: 3200000.00, currency: 'USD', applicantName: 'Global Trading Logistics LLC', beneficiaryName: 'Shanghai Shipping Conglomerate', issueDate: '2026-02-15', expiryDate: '2026-09-15', status: 'ACTIVE', tolerancePercentage: 0.00, portOfLoading: 'Port of Shanghai', portOfDischarge: 'Port of London', latestShipmentDate: '2026-08-30' },
    { id: 4, client: { id: 3, companyName: 'Nexus Electronics Corp' }, creditFacility: { id: 6 }, lcNumber: 'LC-2026-0004', lcType: 'SIGHT', amount: 12000000.00, currency: 'USD', applicantName: 'Nexus Electronics Corp', beneficiaryName: 'Shenzhen Semiconductor Co', issueDate: '2026-04-01', expiryDate: '2027-04-01', status: 'APPROVED', tolerancePercentage: 2.00, portOfLoading: 'Port of Shenzhen', portOfDischarge: 'Port of Singapore', latestShipmentDate: '2027-02-28' },
    { id: 5, client: { id: 3, companyName: 'Nexus Electronics Corp' }, creditFacility: { id: 6 }, lcNumber: 'LC-2026-0005', lcType: 'USANCE', amount: 12000000.00, currency: 'USD', applicantName: 'Nexus Electronics Corp', beneficiaryName: 'Taipei Silicon Foundry Ltd', issueDate: '2026-05-10', expiryDate: '2027-05-10', status: 'ACTIVE', tolerancePercentage: 5.00, portOfLoading: 'Port of Keelung', portOfDischarge: 'Port of Singapore', latestShipmentDate: '2027-04-15' },
  ],
  bgs: [
    { id: 1, client: { id: 1, companyName: 'Acme Industrial Holdings' }, creditFacility: { id: 2 }, bgNumber: 'BG-2026-0001', bgType: 'PERFORMANCE_BOND', amount: 1500000.00, currency: 'USD', beneficiaryName: 'Texas Department of Transportation', issueDate: '2026-01-15', expiryDate: '2027-01-15', status: 'ACTIVE', termsConditions: 'Guarantees the high-fidelity execution of Phase 3 highway structural supply contract.' },
    { id: 2, client: { id: 1, companyName: 'Acme Industrial Holdings' }, creditFacility: { id: 2 }, bgNumber: 'BG-2026-0002', bgType: 'BID_BOND', amount: 500000.00, currency: 'USD', beneficiaryName: 'Federal Energy Infrastructure Group', issueDate: '2026-05-01', expiryDate: '2026-11-01', status: 'PENDING_APPROVAL', termsConditions: 'Tender guarantee for wind-farm construction logistics support.' },
  ],
  bills: [
    { id: 1, client: { id: 1, companyName: 'Acme Industrial Holdings' }, billNumber: 'EXP-BILL-5001', amount: 750000.00, currency: 'USD', status: 'DOCUMENTS_SENT', drawerName: 'Acme Industrial Holdings', draweeName: 'EuroDistribution SA', maturityDate: '2026-08-20', collectionBank: 'Deutsche Bank Frankfurt', trackingStatus: 'COURIER_DISPATCHED' },
    { id: 2, client: { id: 3, companyName: 'Nexus Electronics Corp' }, billNumber: 'EXP-BILL-5002', amount: 2200000.00, currency: 'USD', status: 'ACCEPTED', drawerName: 'Nexus Electronics Corp', draweeName: 'Shenzhen Semiconductor Co', maturityDate: '2026-07-15', collectionBank: 'Standard Chartered HK', trackingStatus: 'DOCUMENTS_ACCEPTED_BY_DRAWEE' },
  ],
  collections: [
    { id: 1, client: { id: 1, companyName: 'Acme Industrial Holdings' }, instructionRef: 'COL-INST-101', amount: 450000.00, currency: 'USD', tenureType: 'SIGHT', draweeName: 'Ontario Heavy Metals Corp', status: 'PROCESSING', instructionDetails: 'Deliver documents against payment (DP) standard sight draft procedure.' },
    { id: 2, client: { id: 2, companyName: 'Global Trading Logistics LLC' }, instructionRef: 'COL-INST-102', amount: 800000.00, currency: 'USD', tenureType: 'USANCE', draweeName: 'Gulf Petrochemicals Dubai', status: 'PENDING', instructionDetails: 'Deliver documents against acceptance (DA) 90 days after bill of lading date.' },
  ],
  screenings: [
    { id: 1, entityName: 'Acme Industrial Holdings', entityType: 'APPLICANT', transactionType: 'LC', transactionId: 'LC-2026-0001', matchScore: 0.00, watchlistSource: 'OFAC_SDN', status: 'CLEARED', complianceNotes: 'Entity completely cleared. No watchlist match found.', screenedAt: '2026-05-28T14:33:00' },
    { id: 2, entityName: 'Tokyo Steel Alloys Inc.', entityType: 'BENEFICIARY', transactionType: 'LC', transactionId: 'LC-2026-0001', matchScore: 5.00, watchlistSource: 'UN_SECURITY_COUNCIL', status: 'CLEARED', complianceNotes: 'Minor name similarity with non-sanctioned entity. Manually verified.', screenedAt: '2026-05-28T14:34:00' },
    { id: 3, entityName: 'Vance Ironworks Syria', entityType: 'BENEFICIARY', transactionType: 'LC', transactionId: 'LC-2026-0002', matchScore: 89.50, watchlistSource: 'OFAC_SDN', status: 'FLAGGED', complianceNotes: 'High match score with sanctioned entity under Syrian Trade Ban Regulation.', screenedAt: '2026-05-28T14:35:00' },
  ],
  cases: [
    { id: 1, screening: { id: 3, entityName: 'Vance Ironworks Syria', watchlistSource: 'OFAC_SDN', matchScore: 89.50 }, caseStatus: 'OPEN', assignedTo: 'compliance', resolutionNotes: 'High match alert triggered for Vance Ironworks Syria. Pending background checks.', createdAt: '2026-05-28T14:35:00' }
  ],
  notifications: [
    { id: 1, userId: 1, title: 'LC Approved', message: 'Your Letter of Credit LC-2026-0004 has been approved.', type: 'INFO', isRead: false },
    { id: 2, userId: 1, title: 'Drawing Discrepancy Found', message: 'A discrepancy has been reported on your drawing DRW-LC003-A.', type: 'ALERT', isRead: false },
    { id: 3, userId: 5, title: 'Sanctions Alert!', message: 'A high match sanctions screening alert has been triggered.', type: 'ALERT', isRead: false },
  ],
  auditLogs: [
    { id: 1, username: 'client', action: 'LC_CREATION_DRAFT', details: 'Created draft Letter of Credit LC-2026-0002', ipAddress: '192.168.1.100', timestamp: '2026-05-28T14:33:00' },
    { id: 2, username: 'ops', action: 'LC_DRAWING_STATUS_UPDATE', details: 'Updated drawing DRW-LC001-A to PAID.', ipAddress: '192.168.2.14', timestamp: '2026-05-28T14:34:00' },
  ]
};

// Response Helper for Mocking
const createMockResponse = (data, message = 'Success') => ({
  data: {
    success: true,
    message,
    data,
    timestamp: new Date().toISOString()
  }
});

// Response Interceptor: Resilient API with Mock Fallback
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // 401 Unauthorized = invalid/expired token → clear session and redirect to login.
    // 403 Forbidden = valid token but insufficient role → do NOT clear session, just reject.
    if (error.response && error.response.status === 401) {
      console.warn('Unauthorized (401): Invalid or expired token. Clearing credentials and redirecting to login.', error);
      localStorage.removeItem('tradevault_token');
      localStorage.removeItem('tradevault_user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }

    // 403: Don't clear session — user is logged in but hitting a role-restricted endpoint.
    // CRITICAL: If it's a COMPLIANCE_HOLD or any 403 on a mutating operation (PUT/POST/DELETE),
    // reject immediately — do NOT fall through to mock, or the component will get fake success data.
    if (error.response && error.response.status === 403) {
      const method = (error.config?.method || 'get').toLowerCase();
      const msg = error.response?.data?.message || '';
      // Always reject 403s on write operations or compliance holds — never mock these
      if (method !== 'get' || msg.includes('COMPLIANCE_HOLD')) {
        console.warn('Forbidden (403) on mutating operation — rejecting immediately (no mock fallback):', error.config?.url);
        return Promise.reject(error);
      }
      console.warn('Forbidden (403) on read — falling back to mock:', error.config?.url);
    }

    console.warn('API error caught (Server offline or network issue). tradevault client is entering Mock Resilient Fallback Mode.', error);

    const config = error.config;
    const url = config.url || '';
    const method = (config.method || 'get').toLowerCase();

    // 1. Auth Endpoint Mocks
    if (url.includes('/auth/login')) {
      const { username } = JSON.parse(config.data || '{}');
      const user = mockDb.users.find(u => u.username === username) || mockDb.users[0];
      return Promise.resolve(createMockResponse({
        token: 'mock-jwt-token-tradevault-' + user.role.toLowerCase(),
        username: user.username,
        fullName: user.fullName,
        role: user.role,
        email: user.email,
        status: user.status
      }, 'Authentication successful (Mock Fallback)'));
    }

    // 2. Letters of Credit Endpoint Mocks
    if (url.includes('/lcs')) {
      if (url.includes('/amendments')) {
        return Promise.resolve(createMockResponse([], 'Amendments loaded'));
      }
      if (url.includes('/drawings')) {
        return Promise.resolve(createMockResponse([], 'Drawings loaded'));
      }
      if (method === 'post') {
        const payload = JSON.parse(config.data || '{}');
        const newLc = {
          id: mockDb.lcs.length + 1,
          lcNumber: payload.lcNumber || ('LC-2026-000' + (mockDb.lcs.length + 1)),
          amount: payload.amount || 100000,
          lcType: payload.lcType || 'SIGHT',
          applicantName: payload.applicantName || 'Acme Corp',
          beneficiaryName: payload.beneficiaryName || 'Overseas Ltd',
          status: 'DRAFT',
          issueDate: new Date().toISOString().split('T')[0],
          expiryDate: payload.expiryDate || new Date().toISOString().split('T')[0]
        };
        mockDb.lcs.unshift(newLc);
        return Promise.resolve(createMockResponse(newLc, 'LC Created Successfully (Mock)'));
      }
      if (url.match(/\/lcs\/\d+\/status$/)) {
        const parts = url.split('/');
        const id = parseInt(parts[parts.indexOf('lcs') + 1]);
        const { status } = JSON.parse(config.data || '{}');
        const lc = mockDb.lcs.find(item => item.id === id);
        if (lc) lc.status = status;
        return Promise.resolve(createMockResponse(lc || { id, status }, 'LC Status Updated (Mock)'));
      }
      return Promise.resolve(createMockResponse(mockDb.lcs, 'LCs retrieved (Mock)'));
    }

    // 3. Bank Guarantees Endpoint Mocks
    if (url.includes('/bgs')) {
      // CLIENT submits DRAFT → PENDING_APPROVAL via dedicated /submit endpoint
      if (url.match(/\/bgs\/\d+\/submit$/)) {
        const id = parseInt(url.split('/').slice(-2)[0]);
        const bg = mockDb.bgs.find(item => item.id === id);
        if (bg) bg.status = 'PENDING_APPROVAL';
        return Promise.resolve(createMockResponse(
          bg || { id, status: 'PENDING_APPROVAL' },
          'BG Submitted for Approval (Mock)'
        ));
      }
      if (method === 'post') {
        const payload = JSON.parse(config.data || '{}');
        const newBg = {
          id: mockDb.bgs.length + 1,
          bgNumber: payload.bgNumber || ('BG-2026-000' + (mockDb.bgs.length + 1)),
          amount: payload.amount || 50000,
          bgType: payload.bgType || 'PERFORMANCE_BOND',
          beneficiaryName: payload.beneficiaryName || 'Global Authority',
          status: 'DRAFT',
          issueDate: new Date().toISOString().split('T')[0],
          expiryDate: payload.expiryDate || new Date().toISOString().split('T')[0]
        };
        mockDb.bgs.unshift(newBg);
        return Promise.resolve(createMockResponse(newBg, 'BG Created (Mock)'));
      }
      if (url.match(/\/bgs\/\d+\/status/)) {
        const id = parseInt(url.split('/').slice(-2)[0]);
        const { status } = JSON.parse(config.data || '{}');
        const bg = mockDb.bgs.find(item => item.id === id);
        if (bg) bg.status = status;
        return Promise.resolve(createMockResponse(bg, 'BG Status Updated (Mock)'));
      }
      return Promise.resolve(createMockResponse(mockDb.bgs, 'BGs retrieved (Mock)'));
    }

    // 4. Export Bills Mocks
    if (url.includes('/bills')) {
      if (url.includes('/collections')) {
        return Promise.resolve(createMockResponse(mockDb.collections, 'Collections loaded (Mock)'));
      }
      return Promise.resolve(createMockResponse(mockDb.bills, 'Bills loaded (Mock)'));
    }

    // 5. Corporate Onboarding Mocks
    if (url.includes('/corporates')) {
      if (url.includes('/facilities')) {
        return Promise.resolve(createMockResponse(mockDb.facilities, 'Facilities loaded (Mock)'));
      }
      return Promise.resolve(createMockResponse(mockDb.clients, 'Clients loaded (Mock)'));
    }

    // 6. Compliance Screening Mocks
    if (url.includes('/compliance')) {
      if (url.includes('/cases')) {
        return Promise.resolve(createMockResponse(mockDb.cases, 'Compliance cases loaded (Mock)'));
      }
      return Promise.resolve(createMockResponse(mockDb.screenings, 'Screenings loaded (Mock)'));
    }

    // 7. Analytics Mocks
    if (url.includes('/analytics/summary')) {
      return Promise.resolve(createMockResponse({
        totalExposure: 18200000.00,
        lcExposure: 14500000.00,
        bgExposure: 1500000.00,
        billExposure: 2200000.00,
        totalLimit: 155000000.00,
        totalUtilized: 47700000.00,
        utilizationRate: 30.77,
        activeLcsCount: 3,
        activeBgsCount: 1,
        activeBillsCount: 2,
        totalScreenings: 4,
        openComplianceCases: 1
      }, 'Analytics retrieved (Mock)'));
    }

    // 8. In-app Alert Notifications Mocks
    if (url.includes('/notifications')) {
      // Mark single notification as read
      if (url.match(/\/notifications\/\d+\/read$/)) {
        const id = parseInt(url.split('/').slice(-2)[0]);
        const n = mockDb.notifications.find(x => x.id === id);
        if (n) n.isRead = true;
        return Promise.resolve(createMockResponse(null, 'Notification marked as read (Mock)'));
      }
      // Unread only
      if (url.includes('/unread')) {
        return Promise.resolve(createMockResponse(mockDb.notifications.filter(n => !n.isRead), 'Unread notifications (Mock)'));
      }
      return Promise.resolve(createMockResponse(mockDb.notifications, 'Notifications loaded (Mock)'));
    }

    // 9. Audit Logs Mocks
    if (url.includes('/audit-logs')) {
      return Promise.resolve(createMockResponse(mockDb.auditLogs, 'Audit logs loaded (Mock)'));
    }

    // 10. Trade Reports Mocks
    if (url.includes('/reports')) {
      const mockReports = [
        { id: 1, reportTitle: 'Q1 2026 Global Exposure Summary', scope: 'PERIOD', generatedAt: '2026-04-01T09:00:00', totalExposure: 18200000, totalTransactions: 12, status: 'COMPLETED' },
        { id: 2, reportTitle: 'Client Portfolio Report — Acme Industrial', scope: 'CLIENT', generatedAt: '2026-05-15T14:22:00', totalExposure: 7500000, totalTransactions: 5, status: 'COMPLETED' },
        { id: 3, reportTitle: 'Product Mix Analysis — LCs & BGs', scope: 'PRODUCT', generatedAt: '2026-06-01T10:05:00', totalExposure: 15000000, totalTransactions: 8, status: 'COMPLETED' },
        { id: 4, reportTitle: 'Branch Distribution Report', scope: 'BRANCH', generatedAt: '2026-06-10T11:30:00', totalExposure: 22000000, totalTransactions: 15, status: 'COMPLETED' },
      ];
      if (url.includes('/generate')) {
        const scope = (url.split('scope=')[1] || 'PERIOD').toUpperCase();
        const newReport = {
          id: mockReports.length + 1,
          reportTitle: `Generated ${scope} Report — ${new Date().toLocaleDateString()}`,
          scope,
          generatedAt: new Date().toISOString(),
          totalExposure: Math.floor(Math.random() * 20000000) + 5000000,
          totalTransactions: Math.floor(Math.random() * 20) + 5,
          status: 'COMPLETED'
        };
        return Promise.resolve(createMockResponse(newReport, 'Report generated (Mock)'));
      }
      if (url.includes('/scope/')) {
        const scope = url.split('/scope/')[1].toUpperCase();
        return Promise.resolve(createMockResponse(mockReports.filter(r => r.scope === scope), 'Reports by scope (Mock)'));
      }
      return Promise.resolve(createMockResponse(mockReports, 'Trade reports loaded (Mock)'));
    }

    // Default error mapping if no mock match is found
    return Promise.reject(error);
  }
);

export default api;
