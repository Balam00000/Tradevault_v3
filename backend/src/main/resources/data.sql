-- =========================================================================
-- TradeVault Seed Data
-- Executed automatically by Spring Boot on startup via spring.sql.init.mode=always
-- spring.jpa.defer-datasource-initialization=true ensures this runs AFTER
-- Hibernate has created all tables via ddl-auto=update
-- =========================================================================

-- All mock users have password 'password' encrypted via BCrypt
-- BCrypt: $2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety
INSERT IGNORE INTO `users` (`username`, `password`, `email`, `full_name`, `role`, `status`) VALUES
('client', '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 'client@tradevault.com', 'Acme Corp Corporate User', 'CLIENT', 'ACTIVE'),
('ops', '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 'ops@tradevault.com', 'Sarah Jenkins (Trade Operations)', 'OPERATIONS', 'ACTIVE'),
('relationship', '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 'rm@tradevault.com', 'David Miller (Relationship Manager)', 'RELATIONSHIP_MANAGER', 'ACTIVE'),
('treasury', '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 'treasury@tradevault.com', 'Elena Rostova (Treasury Director)', 'TREASURY', 'ACTIVE'),
('compliance', '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 'compliance@tradevault.com', 'Marcus Vance (Compliance Officer)', 'COMPLIANCE', 'ACTIVE'),
('admin', '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 'admin@tradevault.com', 'System Admin', 'ADMIN', 'ACTIVE');

-- Insert Corporate Clients
INSERT IGNORE INTO `corporate_clients` (`id`, `company_name`, `tax_id`, `country`, `status`, `credit_limit`, `registration_number`, `industry`, `kyc_status`) VALUES
(1, 'Acme Industrial Holdings', 'TX-99887766', 'United States', 'ACTIVE', 50000000.00, 'REG-1001', 'Manufacturing', 'VERIFIED'),
(2, 'Global Trading Logistics LLC', 'TX-55443322', 'United Kingdom', 'ACTIVE', 25000000.00, 'REG-1002', 'Logistics', 'VERIFIED'),
(3, 'Nexus Electronics Corp', 'TX-11223344', 'Singapore', 'ACTIVE', 80000000.00, 'REG-1003', 'Electronics', 'VERIFIED');

-- Insert Credit Facilities (Acme Industrial Holdings)
INSERT IGNORE INTO `credit_facilities` (`id`, `client_id`, `facility_type`, `limit_amount`, `utilized_amount`, `currency`, `status`, `expiry_date`) VALUES
(1, 1, 'LETTER_OF_CREDIT_FACILITY', 20000000.00, 4500000.00, 'USD', 'ACTIVE', '2027-12-31'),
(2, 1, 'GUARANTEE_FACILITY', 10000000.00, 2000000.00, 'USD', 'ACTIVE', '2027-06-30'),
(3, 1, 'REVOLVING_LINE', 20000000.00, 12000000.00, 'USD', 'ACTIVE', '2028-01-15');

-- Insert Credit Facilities (Global Trading Logistics)
INSERT IGNORE INTO `credit_facilities` (`id`, `client_id`, `facility_type`, `limit_amount`, `utilized_amount`, `currency`, `status`, `expiry_date`) VALUES
(4, 2, 'LETTER_OF_CREDIT_FACILITY', 15000000.00, 3200000.00, 'USD', 'ACTIVE', '2026-12-31'),
(5, 2, 'GUARANTEE_FACILITY', 10000000.00, 0.00, 'USD', 'ACTIVE', '2027-08-30');

-- Insert Credit Facilities (Nexus Electronics)
INSERT IGNORE INTO `credit_facilities` (`id`, `client_id`, `facility_type`, `limit_amount`, `utilized_amount`, `currency`, `status`, `expiry_date`) VALUES
(6, 3, 'LETTER_OF_CREDIT_FACILITY', 50000000.00, 24000000.00, 'USD', 'ACTIVE', '2028-09-30'),
(7, 3, 'REVOLVING_LINE', 30000000.00, 0.00, 'USD', 'ACTIVE', '2028-12-31');

-- Insert Letters of Credit
INSERT IGNORE INTO `letters_of_credit` (`id`, `client_id`, `credit_facility_id`, `lc_number`, `lc_type`, `amount`, `currency`, `applicant_name`, `beneficiary_name`, `beneficiary_country`, `issue_date`, `expiry_date`, `status`, `tolerance_percentage`, `port_of_loading`, `port_of_discharge`, `latest_shipment_date`) VALUES
(1, 1, 1, 'LC-2026-0001', 'SIGHT', 1500000.00, 'USD', 'Acme Industrial Holdings', 'Tokyo Steel Alloys Inc.', 'Japan', '2026-01-10', '2026-10-10', 'ACTIVE', 5.00, 'Port of Yokohama', 'Port of Los Angeles', '2026-09-15'),
(2, 1, 1, 'LC-2026-0002', 'USANCE', 3000000.00, 'USD', 'Acme Industrial Holdings', 'Berlin Motor Parts GmbH', 'Germany', '2026-03-01', '2026-12-01', 'IN_REVIEW', 10.00, 'Port of Hamburg', 'Port of New York', '2026-11-01'),
(3, 2, 4, 'LC-2026-0003', 'SIGHT', 3200000.00, 'USD', 'Global Trading Logistics LLC', 'Shanghai Shipping Conglomerate', 'China', '2026-02-15', '2026-09-15', 'ACTIVE', 0.00, 'Port of Shanghai', 'Port of London', '2026-08-30'),
(4, 3, 6, 'LC-2026-0004', 'SIGHT', 12000000.00, 'USD', 'Nexus Electronics Corp', 'Shenzhen Semiconductor Co', 'China', '2026-04-01', '2027-04-01', 'APPROVED', 2.00, 'Port of Shenzhen', 'Port of Singapore', '2027-02-28'),
(5, 3, 6, 'LC-2026-0005', 'USANCE', 12000000.00, 'USD', 'Nexus Electronics Corp', 'Taipei Silicon Foundry Ltd', 'Taiwan', '2026-05-10', '2027-05-10', 'ACTIVE', 5.00, 'Port of Keelung', 'Port of Singapore', '2027-04-15');

-- Insert LC Drawings
INSERT IGNORE INTO `lc_drawings` (`id`, `lc_id`, `drawing_ref`, `amount`, `currency`, `status`, `presentation_date`, `documents_presented`, `discrepancy_notes`) VALUES
(1, 1, 'DRW-LC001-A', 500000.00, 'USD', 'PAID', '2026-04-05', 'Bill of Lading, Commercial Invoice, Certificate of Origin', NULL),
(2, 1, 'DRW-LC001-B', 1000000.00, 'USD', 'PENDING_REVIEW', '2026-05-25', 'Bill of Lading, Packing List, Insurance Document', 'Maturity date on invoice differs by 1 day from LC requirement'),
(3, 3, 'DRW-LC003-A', 1200000.00, 'USD', 'DISCREPANT', '2026-05-12', 'Draft, Bill of Lading, Commercial Invoice', 'Port of loading on Bill of Lading shows Ningbo instead of Shanghai');

-- Insert LC Amendments
INSERT IGNORE INTO `lc_amendments` (`id`, `lc_id`, `amendment_number`, `previous_amount`, `new_amount`, `previous_expiry_date`, `new_expiry_date`, `status`, `justification`, `created_by`) VALUES
(1, 1, 1, 1200000.00, 1500000.00, '2026-08-10', '2026-10-10', 'APPROVED', 'Increase in order size and logistics delays', 'ops'),
(5, 1, 2, 10000000.00, 12000000.00, '2027-05-10', '2027-05-10', 'PENDING_APPROVAL', 'Additional shipment of fabrication machines requested', 'client');

-- Insert Bank Guarantees
INSERT IGNORE INTO `bank_guarantees` (`id`, `client_id`, `credit_facility_id`, `bg_number`, `bg_type`, `amount`, `currency`, `beneficiary_name`, `issue_date`, `expiry_date`, `status`, `terms_conditions`) VALUES
(1, 1, 2, 'BG-2026-0001', 'PERFORMANCE_BOND', 1500000.00, 'USD', 'Texas Department of Transportation', '2026-01-15', '2027-01-15', 'ACTIVE', 'Guarantees the high-fidelity execution of Phase 3 highway structural supply contract.'),
(2, 1, 2, 'BG-2026-0002', 'BID_BOND', 500000.00, 'USD', 'Federal Energy Infrastructure Group', '2026-05-01', '2026-11-01', 'PENDING_APPROVAL', 'Tender guarantee for wind-farm construction logistics support.');

-- Insert BG Claims
INSERT IGNORE INTO `bg_claims` (`id`, `bg_id`, `claim_ref`, `amount`, `claim_date`, `status`, `payment_details`) VALUES
(1, 1, 'CLM-BG001-01', 300000.00, '2026-05-20', 'PENDING', 'Claims partial breach of project delivery milestones under Article 4.');

-- Insert Export Bills
INSERT IGNORE INTO `export_bills` (`id`, `client_id`, `bill_number`, `amount`, `currency`, `status`, `drawer_name`, `drawee_name`, `maturity_date`, `collection_bank`, `tracking_status`) VALUES
(1, 1, 'EXP-BILL-5001', 750000.00, 'USD', 'DOCUMENTS_SENT', 'Acme Industrial Holdings', 'EuroDistribution SA', '2026-08-20', 'Deutsche Bank Frankfurt', 'COURIER_DISPATCHED'),
(2, 3, 'EXP-BILL-5002', 2200000.00, 'USD', 'ACCEPTED', 'Nexus Electronics Corp', 'Shenzhen Semiconductor Co', '2026-07-15', 'Standard Chartered HK', 'DOCUMENTS_ACCEPTED_BY_DRAWEE');

-- Insert Collection Instructions
INSERT IGNORE INTO `collection_instructions` (`id`, `client_id`, `instruction_ref`, `amount`, `currency`, `tenure_type`, `drawee_name`, `status`, `instruction_details`) VALUES
(1, 1, 'COL-INST-101', 450000.00, 'USD', 'SIGHT', 'Ontario Heavy Metals Corp', 'PROCESSING', 'Deliver documents against payment (DP) standard sight draft procedure.'),
(2, 2, 'COL-INST-102', 800000.00, 'USD', 'USANCE', 'Gulf Petrochemicals Dubai', 'PENDING', 'Deliver documents against acceptance (DA) 90 days after bill of lading date.');

-- Insert Sanctions Screenings
INSERT IGNORE INTO `sanctions_screenings` (`id`, `entity_name`, `entity_type`, `transaction_type`, `transaction_id`, `match_score`, `watchlist_source`, `status`, `compliance_notes`) VALUES
(1, 'Acme Industrial Holdings', 'APPLICANT', 'LC', 'LC-2026-0001', 0.00, 'OFAC_SDN', 'CLEARED', 'Entity completely cleared. No watchlist match found.'),
(2, 'Tokyo Steel Alloys Inc.', 'BENEFICIARY', 'LC', 'LC-2026-0001', 5.00, 'UN_SECURITY_COUNCIL', 'CLEARED', 'Minor name similarity with non-sanctioned entity in list. Manually verified and cleared.'),
(3, 'Vance Ironworks Syria', 'BENEFICIARY', 'LC', 'LC-2026-0002', 89.50, 'OFAC_SDN', 'FLAGGED', 'High match score with sanctioned entity under Syrian Trade Ban Regulation.'),
(4, 'Gulf Petrochemicals Dubai', 'DRAWER', 'COLLECTION', 'COL-INST-102', 12.00, 'EU_WATCHLIST', 'UNDER_REVIEW', 'Routine screening triggered by origin country matching warning list; awaiting manual sign-off.');

-- Insert Compliance Cases
INSERT IGNORE INTO `compliance_cases` (`id`, `screening_id`, `case_status`, `assigned_to`, `resolution_notes`) VALUES
(1, 3, 'OPEN', 'compliance', 'High match alert triggered for Vance Ironworks Syria. Pending full background check of corporate registries.'),
(2, 4, 'UNDER_INVESTIGATION', 'compliance', 'Analyzing corporate shareholding certificates to verify Ultimate Beneficial Owner (UBO) status.');

-- Insert Notifications
INSERT IGNORE INTO `notifications` (`id`, `user_id`, `title`, `message`, `category`, `status`) VALUES
(1, 1, 'LC Approved', 'Your Letter of Credit LC-2026-0004 has been approved by the Relationship Manager.', 'INFO', 'UNREAD'),
(2, 1, 'Drawing Discrepancy Found', 'A discrepancy has been reported on your drawing DRW-LC003-A. Please review notes.', 'ALERT', 'UNREAD'),
(3, 5, 'Sanctions Alert!', 'A high match sanctions screening alert (89.5% score) has been triggered for LC-2026-0002.', 'ALERT', 'UNREAD'),
(4, 2, 'Credit Limit Approaching', 'Global Trading Logistics revolving limit is 80% utilized. Please consider extension.', 'WARNING', 'UNREAD'),
(5, 2, 'LC Amendment Approval Required', 'Corporate client has requested an amendment on LC-2026-0005. Action required.', 'APPROVAL', 'UNREAD');

-- Insert Audit Logs
INSERT IGNORE INTO `audit_logs` (`id`, `user_id`, `username`, `action`, `details`, `ip_address`) VALUES
(1, 1, 'client', 'LC_CREATION_DRAFT', 'Created draft Letter of Credit LC-2026-0002 for $3,000,000.00', '192.168.1.100'),
(2, 2, 'ops', 'LC_DRAWING_STATUS_UPDATE', 'Updated drawing DRW-LC001-A to PAID. Utilizing credit facility LETTER_OF_CREDIT_FACILITY.', '192.168.2.14'),
(3, 5, 'compliance', 'COMPLIANCE_CASE_OPENED', 'Opened investigation compliance case for high match score on Vance Ironworks Syria.', '192.168.5.5');

-- Link seed client user to Corporate Client 1
UPDATE `users` SET `corporate_client_id` = 1 WHERE `username` = 'client';

-- Guarantee that all default seeded users have password 'password' and status 'ACTIVE'
UPDATE `users` 
SET `password` = '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 
    `status` = 'ACTIVE' 
WHERE `username` IN ('client', 'ops', 'relationship', 'treasury', 'compliance', 'admin');
