-- TradeVault Database Schema
-- Compatible with MySQL 8.0+

CREATE DATABASE IF NOT EXISTS `tradevault`;
USE `tradevault`;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) UNIQUE NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `email` VARCHAR(100) UNIQUE NOT NULL,
  `full_name` VARCHAR(100) NOT NULL,
  `role` VARCHAR(30) NOT NULL, -- CLIENT, OPERATIONS, RELATIONSHIP_MANAGER, TREASURY, COMPLIANCE, ADMIN
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `branch_id` VARCHAR(50) NULL,
  `corporate_client_id` BIGINT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_username` (`username`),
  INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Audit Logs Table
CREATE TABLE IF NOT EXISTS `audit_logs` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NULL,
  `username` VARCHAR(50) NOT NULL,
  `action` VARCHAR(100) NOT NULL,
  `details` TEXT,
  `ip_address` VARCHAR(45) NULL,
  `entity_type` VARCHAR(50) NULL,
  `record_id` BIGINT NULL,
  `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_username_audit` (`username`),
  INDEX `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Corporate Clients Table
CREATE TABLE IF NOT EXISTS `corporate_clients` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `company_name` VARCHAR(150) NOT NULL,
  `tax_id` VARCHAR(50) UNIQUE NOT NULL,
  `country` VARCHAR(50) NOT NULL,
  `status` VARCHAR(30) DEFAULT 'ACTIVE',
  `registration_number` VARCHAR(50) NULL,
  `industry` VARCHAR(100) NULL,
  `relationship_manager_id` BIGINT NULL,
  `kyc_status` VARCHAR(30) DEFAULT 'PENDING',
  `credit_limit` DECIMAL(15, 2) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_company_name` (`company_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Credit Facilities Table
CREATE TABLE IF NOT EXISTS `credit_facilities` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `client_id` BIGINT NOT NULL,
  `facility_type` VARCHAR(50) NOT NULL, -- TERM_LOAN, REVOLVING_LINE, LETTER_OF_CREDIT_FACILITY, GUARANTEE_FACILITY
  `limit_amount` DECIMAL(15, 2) NOT NULL,
  `utilized_amount` DECIMAL(15, 2) DEFAULT 0.00,
  `currency` VARCHAR(3) DEFAULT 'USD',
  `status` VARCHAR(30) DEFAULT 'ACTIVE',
  `expiry_date` DATE NOT NULL,
  `collateral_type` VARCHAR(100) NULL,
  `collateral_value` DECIMAL(15, 2) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`client_id`) REFERENCES `corporate_clients` (`id`) ON DELETE CASCADE,
  INDEX `idx_facility_client` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Letters of Credit Table
CREATE TABLE IF NOT EXISTS `letters_of_credit` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `client_id` BIGINT NOT NULL,
  `credit_facility_id` BIGINT NOT NULL,
  `lc_number` VARCHAR(50) UNIQUE NOT NULL,
  `lc_type` VARCHAR(30) NOT NULL, -- SIGHT, USANCE, REVOLVING, STANDBY
  `amount` DECIMAL(15, 2) NOT NULL,
  `currency` VARCHAR(3) DEFAULT 'USD',
  `applicant_name` VARCHAR(150) NOT NULL,
  `beneficiary_name` VARCHAR(150) NOT NULL,
  `beneficiary_country` VARCHAR(100) NULL,
  `issue_date` DATE NOT NULL,
  `expiry_date` DATE NOT NULL,
  `status` VARCHAR(30) DEFAULT 'DRAFT', -- DRAFT, IN_REVIEW, APPROVED, REJECTED, ACTIVE, AMENDED, DRAWN, EXPIRED, CLOSED
  `tolerance_percentage` DECIMAL(5, 2) DEFAULT 0.00,
  `port_of_loading` VARCHAR(100) NULL,
  `port_of_discharge` VARCHAR(100) NULL,
  `latest_shipment_date` DATE NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`client_id`) REFERENCES `corporate_clients` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`credit_facility_id`) REFERENCES `credit_facilities` (`id`) ON DELETE CASCADE,
  INDEX `idx_lc_number` (`lc_number`),
  INDEX `idx_lc_client` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. LC Amendments Table
CREATE TABLE IF NOT EXISTS `lc_amendments` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `lc_id` BIGINT NOT NULL,
  `amendment_number` INT NOT NULL,
  `previous_amount` DECIMAL(15, 2) NOT NULL,
  `new_amount` DECIMAL(15, 2) NOT NULL,
  `previous_expiry_date` DATE NOT NULL,
  `new_expiry_date` DATE NOT NULL,
  `status` VARCHAR(30) DEFAULT 'PENDING_APPROVAL', -- PENDING_APPROVAL, APPROVED, REJECTED
  `amendment_type` VARCHAR(30) DEFAULT 'OTHER',
  `requested_by_id` BIGINT NULL,
  `approved_by_id` BIGINT NULL,
  `justification` TEXT NULL,
  `created_by` VARCHAR(50) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`lc_id`) REFERENCES `letters_of_credit` (`id`) ON DELETE CASCADE,
  INDEX `idx_amendment_lc` (`lc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. LC Drawings Table
CREATE TABLE IF NOT EXISTS `lc_drawings` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `lc_id` BIGINT NOT NULL,
  `drawing_ref` VARCHAR(50) UNIQUE NOT NULL,
  `amount` DECIMAL(15, 2) NOT NULL,
  `currency` VARCHAR(3) DEFAULT 'USD',
  `status` VARCHAR(30) DEFAULT 'PENDING_REVIEW', -- PENDING_REVIEW, DISCREPANT, APPROVED, PAID, REJECTED
  `presentation_date` DATE NOT NULL,
  `documents_presented` TEXT NULL,
  `discrepancy_notes` TEXT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`lc_id`) REFERENCES `letters_of_credit` (`id`) ON DELETE CASCADE,
  INDEX `idx_drawing_lc` (`lc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Bank Guarantees Table
CREATE TABLE IF NOT EXISTS `bank_guarantees` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `client_id` BIGINT NOT NULL,
  `credit_facility_id` BIGINT NOT NULL,
  `bg_number` VARCHAR(50) UNIQUE NOT NULL,
  `bg_type` VARCHAR(30) NOT NULL, -- BID_BOND, PERFORMANCE_BOND, ADVANCE_PAYMENT, FINANCIAL
  `amount` DECIMAL(15, 2) NOT NULL,
  `currency` VARCHAR(3) DEFAULT 'USD',
  `beneficiary_name` VARCHAR(150) NOT NULL,
  `issue_date` DATE NOT NULL,
  `expiry_date` DATE NOT NULL,
  `claim_period_days` INT NULL,
  `status` VARCHAR(30) DEFAULT 'DRAFT', -- DRAFT, PENDING_APPROVAL, ACTIVE, CLAIMED, EXPIRED, RELEASED
  `terms_conditions` TEXT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`client_id`) REFERENCES `corporate_clients` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`credit_facility_id`) REFERENCES `credit_facilities` (`id`) ON DELETE CASCADE,
  INDEX `idx_bg_number` (`bg_number`),
  INDEX `idx_bg_client` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. BG Claims Table
CREATE TABLE IF NOT EXISTS `bg_claims` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `bg_id` BIGINT NOT NULL,
  `claim_ref` VARCHAR(50) UNIQUE NOT NULL,
  `amount` DECIMAL(15, 2) NOT NULL,
  `claim_date` DATE NOT NULL,
  `status` VARCHAR(30) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
  `claimant_details` VARCHAR(150) NULL,
  `reviewed_by_id` BIGINT NULL,
  `payment_details` TEXT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`bg_id`) REFERENCES `bank_guarantees` (`id`) ON DELETE CASCADE,
  INDEX `idx_claim_bg` (`bg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Export Bills Table
CREATE TABLE IF NOT EXISTS `export_bills` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `client_id` BIGINT NOT NULL,
  `lc_id` BIGINT NULL,
  `bill_number` VARCHAR(50) UNIQUE NOT NULL,
  `amount` DECIMAL(15, 2) NOT NULL,
  `currency` VARCHAR(3) DEFAULT 'USD',
  `status` VARCHAR(30) DEFAULT 'INITIATED', -- INITIATED, DOCUMENTS_SENT, ACCEPTED, PAID, OVERDUE
  `bill_type` VARCHAR(30) DEFAULT 'DocumentaryCollection',
  `bill_date` DATE NULL,
  `drawer_name` VARCHAR(150) NOT NULL,
  `drawee_name` VARCHAR(150) NOT NULL,
  `buyer_country` VARCHAR(100) NULL,
  `maturity_date` DATE NOT NULL,
  `collection_bank` VARCHAR(150) NOT NULL,
  `tracking_status` VARCHAR(100) DEFAULT 'DOCUMENTS_PREPARED',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`client_id`) REFERENCES `corporate_clients` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`lc_id`) REFERENCES `letters_of_credit` (`id`) ON DELETE SET NULL,
  INDEX `idx_bill_client` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Collection Instructions Table
CREATE TABLE IF NOT EXISTS `collection_instructions` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `client_id` BIGINT NOT NULL,
  `bill_id` BIGINT NULL,
  `instruction_ref` VARCHAR(50) UNIQUE NOT NULL,
  `collecting_bank_ref` VARCHAR(50) NULL,
  `amount` DECIMAL(15, 2) NOT NULL,
  `currency` VARCHAR(3) DEFAULT 'USD',
  `tenure_type` VARCHAR(30) NOT NULL, -- SIGHT, USANCE
  `instruction_type` VARCHAR(30) DEFAULT 'DP',
  `drawee_name` VARCHAR(150) NOT NULL,
  `instruction_date` DATE NULL,
  `response_date` DATE NULL,
  `remittance_amount` DECIMAL(15, 2) NULL,
  `status` VARCHAR(30) DEFAULT 'PENDING', -- PENDING, PROCESSING, COLLECTED, RETURNED
  `instruction_details` TEXT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`client_id`) REFERENCES `corporate_clients` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`bill_id`) REFERENCES `export_bills` (`id`) ON DELETE SET NULL,
  INDEX `idx_instruction_client` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Sanctions Screenings Table
CREATE TABLE IF NOT EXISTS `sanctions_screenings` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `entity_name` VARCHAR(150) NOT NULL,
  `entity_type` VARCHAR(50) NOT NULL, -- APPLICANT, BENEFICIARY, BANK
  `entity_country` VARCHAR(100) NULL,
  `transaction_type` VARCHAR(50) NOT NULL, -- LC, BG, EXPORT_BILL
  `transaction_id` VARCHAR(50) NOT NULL,
  `match_score` DECIMAL(5, 2) NOT NULL,
  `watchlist_source` VARCHAR(100) NOT NULL, -- OFAC, UN_SECURITY_COUNCIL, EU_WATCHLIST
  `status` VARCHAR(30) DEFAULT 'UNDER_REVIEW', -- CLEARED, FLAGGED, UNDER_REVIEW
  `screened_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `compliance_notes` TEXT NULL,
  INDEX `idx_entity_screening` (`entity_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Compliance Cases Table
CREATE TABLE IF NOT EXISTS `compliance_cases` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `screening_id` BIGINT NOT NULL,
  `case_status` VARCHAR(30) DEFAULT 'OPEN', -- OPEN, UNDER_INVESTIGATION, ESCALATED, RESOLVED_CLEARED, RESOLVED_BLOCKED
  `assigned_officer_id` BIGINT NULL,
  `assigned_to` VARCHAR(50) NULL,
  `opened_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `closed_date` TIMESTAMP NULL,
  `resolution_notes` TEXT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`screening_id`) REFERENCES `sanctions_screenings` (`id`) ON DELETE CASCADE,
  INDEX `idx_screening_case` (`screening_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Notifications Table
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(150) NOT NULL,
  `message` TEXT NOT NULL,
  `category` VARCHAR(30) DEFAULT 'INFO', -- INFO, WARNING, ALERT, APPROVAL
  `status` VARCHAR(30) DEFAULT 'UNREAD', -- UNREAD, READ, DISMISSED
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_notification` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Trade Reports Table
CREATE TABLE IF NOT EXISTS `trade_reports` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `scope` VARCHAR(50) NOT NULL,
  `metrics` TEXT NOT NULL,
  `generated_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- SEED DATA
-- =========================================================================

-- All mock users have password 'password' encrypted via BCrypt
-- BCrypt: $2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety
INSERT INTO `users` (`username`, `password`, `email`, `full_name`, `role`, `status`) VALUES
('client', '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 'client@tradevault.com', 'Acme Corp Corporate User', 'CLIENT', 'ACTIVE'),
('ops', '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 'ops@tradevault.com', 'Sarah Jenkins (Trade Operations)', 'OPERATIONS', 'ACTIVE'),
('relationship', '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 'rm@tradevault.com', 'David Miller (Relationship Manager)', 'RELATIONSHIP_MANAGER', 'ACTIVE'),
('treasury', '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 'treasury@tradevault.com', 'Elena Rostova (Treasury Director)', 'TREASURY', 'ACTIVE'),
('compliance', '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 'compliance@tradevault.com', 'Marcus Vance (Compliance Officer)', 'COMPLIANCE', 'ACTIVE'),
('admin', '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 'admin@tradevault.com', 'System Admin', 'ADMIN', 'ACTIVE');

-- Insert Corporate Clients
INSERT INTO `corporate_clients` (`company_name`, `tax_id`, `country`, `status`, `credit_limit`, `registration_number`, `industry`, `kyc_status`, `relationship_manager_id`) VALUES
('Acme Industrial Holdings', 'TX-99887766', 'United States', 'ACTIVE', 50000000.00, 'REG-1001', 'Manufacturing', 'VERIFIED', NULL),
('Global Trading Logistics LLC', 'TX-55443322', 'United Kingdom', 'ACTIVE', 25000000.00, 'REG-1002', 'Logistics', 'VERIFIED', NULL),
('Nexus Electronics Corp', 'TX-11223344', 'Singapore', 'ACTIVE', 80000000.00, 'REG-1003', 'Electronics', 'VERIFIED', 3);

-- Insert Credit Facilities
-- 1 for Acme Industrial Holdings
INSERT INTO `credit_facilities` (`client_id`, `facility_type`, `limit_amount`, `utilized_amount`, `currency`, `status`, `expiry_date`) VALUES
(1, 'LETTER_OF_CREDIT_FACILITY', 20000000.00, 4500000.00, 'USD', 'ACTIVE', '2027-12-31'),
(1, 'GUARANTEE_FACILITY', 10000000.00, 2000000.00, 'USD', 'ACTIVE', '2027-06-30'),
(1, 'REVOLVING_LINE', 20000000.00, 12000000.00, 'USD', 'ACTIVE', '2028-01-15');

-- 2 for Global Trading Logistics
INSERT INTO `credit_facilities` (`client_id`, `facility_type`, `limit_amount`, `utilized_amount`, `currency`, `status`, `expiry_date`) VALUES
(2, 'LETTER_OF_CREDIT_FACILITY', 15000000.00, 3200000.00, 'USD', 'ACTIVE', '2026-12-31'),
(2, 'GUARANTEE_FACILITY', 10000000.00, 0.00, 'USD', 'ACTIVE', '2027-08-30');

-- 3 for Nexus Electronics
INSERT INTO `credit_facilities` (`client_id`, `facility_type`, `limit_amount`, `utilized_amount`, `currency`, `status`, `expiry_date`) VALUES
(3, 'LETTER_OF_CREDIT_FACILITY', 50000000.00, 24000000.00, 'USD', 'ACTIVE', '2028-09-30'),
(3, 'REVOLVING_LINE', 30000000.00, 0.00, 'USD', 'ACTIVE', '2028-12-31');

-- Insert Letters of Credit
INSERT INTO `letters_of_credit` (`client_id`, `credit_facility_id`, `lc_number`, `lc_type`, `amount`, `currency`, `applicant_name`, `beneficiary_name`, `beneficiary_country`, `issue_date`, `expiry_date`, `status`, `tolerance_percentage`, `port_of_loading`, `port_of_discharge`, `latest_shipment_date`) VALUES
(1, 1, 'LC-2026-0001', 'SIGHT', 1500000.00, 'USD', 'Acme Industrial Holdings', 'Tokyo Steel Alloys Inc.', 'Japan', '2026-01-10', '2026-10-10', 'ACTIVE', 5.00, 'Port of Yokohama', 'Port of Los Angeles', '2026-09-15'),
(1, 1, 'LC-2026-0002', 'USANCE', 3000000.00, 'USD', 'Acme Industrial Holdings', 'Berlin Motor Parts GmbH', 'Germany', '2026-03-01', '2026-12-01', 'IN_REVIEW', 10.00, 'Port of Hamburg', 'Port of New York', '2026-11-01'),
(2, 4, 'LC-2026-0003', 'SIGHT', 3200000.00, 'USD', 'Global Trading Logistics LLC', 'Shanghai Shipping Conglomerate', 'China', '2026-02-15', '2026-09-15', 'ACTIVE', 0.00, 'Port of Shanghai', 'Port of London', '2026-08-30'),
(3, 6, 'LC-2026-0004', 'SIGHT', 12000000.00, 'USD', 'Nexus Electronics Corp', 'Shenzhen Semiconductor Co', 'China', '2026-04-01', '2027-04-01', 'APPROVED', 2.00, 'Port of Shenzhen', 'Port of Singapore', '2027-02-28'),
(3, 6, 'LC-2026-0005', 'USANCE', 12000000.00, 'USD', 'Nexus Electronics Corp', 'Taipei Silicon Foundry Ltd', 'Taiwan', '2026-05-10', '2027-05-10', 'ACTIVE', 5.00, 'Port of Keelung', 'Port of Singapore', '2027-04-15');

-- Insert LC Drawings
INSERT INTO `lc_drawings` (`lc_id`, `drawing_ref`, `amount`, `currency`, `status`, `presentation_date`, `documents_presented`, `discrepancy_notes`) VALUES
(1, 'DRW-LC001-A', 500000.00, 'USD', 'PAID', '2026-04-05', 'Bill of Lading, Commercial Invoice, Certificate of Origin', NULL),
(1, 'DRW-LC001-B', 1000000.00, 'USD', 'PENDING_REVIEW', '2026-05-25', 'Bill of Lading, Packing List, Insurance Document', 'Maturity date on invoice differs by 1 day from LC requirement'),
(3, 'DRW-LC003-A', 1200000.00, 'USD', 'DISCREPANT', '2026-05-12', 'Draft, Bill of Lading, Commercial Invoice', 'Port of loading on Bill of Lading shows Ningbo instead of Shanghai');

-- Insert LC Amendments
INSERT INTO `lc_amendments` (`lc_id`, `amendment_number`, `previous_amount`, `new_amount`, `previous_expiry_date`, `new_expiry_date`, `status`, `justification`, `created_by`) VALUES
(1, 1, 1200000.00, 1500000.00, '2026-08-10', '2026-10-10', 'APPROVED', 'Increase in order size and logistics delays', 'ops'),
(5, 1, 10000000.00, 12000000.00, '2027-05-10', '2027-05-10', 'PENDING_APPROVAL', 'Additional shipment of fabrication machines requested', 'client');

-- Insert Bank Guarantees
INSERT INTO `bank_guarantees` (`client_id`, `credit_facility_id`, `bg_number`, `bg_type`, `amount`, `currency`, `beneficiary_name`, `issue_date`, `expiry_date`, `status`, `terms_conditions`) VALUES
(1, 2, 'BG-2026-0001', 'PERFORMANCE_BOND', 1500000.00, 'USD', 'Texas Department of Transportation', '2026-01-15', '2027-01-15', 'ACTIVE', 'Guarantees the high-fidelity execution of Phase 3 highway structural supply contract.'),
(1, 2, 'BG-2026-0002', 'BID_BOND', 500000.00, 'USD', 'Federal Energy Infrastructure Group', '2026-05-01', '2026-11-01', 'PENDING_APPROVAL', 'Tender guarantee for wind-farm construction logistics support.');

-- Insert BG Claims
INSERT INTO `bg_claims` (`bg_id`, `claim_ref`, `amount`, `claim_date`, `status`, `payment_details`) VALUES
(1, 'CLM-BG001-01', 300000.00, '2026-05-20', 'PENDING', 'Claims partial breach of project delivery milestones under Article 4.');

-- Insert Export Bills
INSERT INTO `export_bills` (`client_id`, `bill_number`, `amount`, `currency`, `status`, `drawer_name`, `drawee_name`, `maturity_date`, `collection_bank`, `tracking_status`) VALUES
(1, 'EXP-BILL-5001', 750000.00, 'USD', 'DOCUMENTS_SENT', 'Acme Industrial Holdings', 'EuroDistribution SA', '2026-08-20', 'Deutsche Bank Frankfurt', 'COURIER_DISPATCHED'),
(3, 'EXP-BILL-5002', 2200000.00, 'USD', 'ACCEPTED', 'Nexus Electronics Corp', 'Shenzhen Semiconductor Co', '2026-07-15', 'Standard Chartered HK', 'DOCUMENTS_ACCEPTED_BY_DRAWEE');

-- Insert Collection Instructions
INSERT INTO `collection_instructions` (`client_id`, `instruction_ref`, `amount`, `currency`, `tenure_type`, `drawee_name`, `status`, `instruction_details`) VALUES
(1, 'COL-INST-101', 450000.00, 'USD', 'SIGHT', 'Ontario Heavy Metals Corp', 'PROCESSING', 'Deliver documents against payment (DP) standard sight draft procedure.'),
(2, 'COL-INST-102', 800000.00, 'USD', 'USANCE', 'Gulf Petrochemicals Dubai', 'PENDING', 'Deliver documents against acceptance (DA) 90 days after bill of lading date.');

-- Insert Sanctions Screenings
INSERT INTO `sanctions_screenings` (`entity_name`, `entity_type`, `transaction_type`, `transaction_id`, `match_score`, `watchlist_source`, `status`, `compliance_notes`) VALUES
('Acme Industrial Holdings', 'APPLICANT', 'LC', 'LC-2026-0001', 0.00, 'OFAC_SDN', 'CLEARED', 'Entity completely cleared. No watchlist match found.'),
('Tokyo Steel Alloys Inc.', 'BENEFICIARY', 'LC', 'LC-2026-0001', 5.00, 'UN_SECURITY_COUNCIL', 'CLEARED', 'Minor name similarity with non-sanctioned entity in list. Manually verified and cleared.'),
('Vance Ironworks Syria', 'BENEFICIARY', 'LC', 'LC-2026-0002', 89.50, 'OFAC_SDN', 'FLAGGED', 'High match score with sanctioned entity under Syrian Trade Ban Regulation.'),
('Gulf Petrochemicals Dubai', 'DRAWER', 'COLLECTION', 'COL-INST-102', 12.00, 'EU_WATCHLIST', 'UNDER_REVIEW', 'Routine screening triggered by origin country matching warning list; awaiting manual sign-off.');

-- Insert Compliance Cases
INSERT INTO `compliance_cases` (`screening_id`, `case_status`, `assigned_to`, `resolution_notes`) VALUES
(3, 'OPEN', 'compliance', 'High match alert triggered for Vance Ironworks Syria. Pending full background check of corporate registries.'),
(4, 'UNDER_INVESTIGATION', 'compliance', 'Analyzing corporate shareholding certificates to verify Ultimate Beneficial Owner (UBO) status.');

-- Insert Notifications
INSERT INTO `notifications` (`user_id`, `title`, `message`, `category`, `status`) VALUES
(1, 'LC Approved', 'Your Letter of Credit LC-2026-0004 has been approved by the Relationship Manager.', 'INFO', 'UNREAD'),
(1, 'Drawing Discrepancy Found', 'A discrepancy has been reported on your drawing DRW-LC003-A. Please review notes.', 'ALERT', 'UNREAD'),
(5, 'Sanctions Alert!', 'A high match sanctions screening alert (89.5% score) has been triggered for LC-2026-0002.', 'ALERT', 'UNREAD'),
(2, 'Credit Limit Approaching', 'Global Trading Logistics revolving limit is 80% utilized. Please consider extension.', 'WARNING', 'UNREAD'),
(2, 'LC Amendment Approval Required', 'Corporate client has requested an amendment on LC-2026-0005. Action required.', 'APPROVAL', 'UNREAD');

-- Insert Audit Logs
INSERT INTO `audit_logs` (`user_id`, `username`, `action`, `details`, `ip_address`) VALUES
(1, 'client', 'LC_CREATION_DRAFT', 'Created draft Letter of Credit LC-2026-0002 for $3,000,000.00', '192.168.1.100'),
(2, 'ops', 'LC_DRAWING_STATUS_UPDATE', 'Updated drawing DRW-LC001-A to PAID. Utilizing credit facility LETTER_OF_CREDIT_FACILITY.', '192.168.2.14'),
(5, 'compliance', 'COMPLIANCE_CASE_OPENED', 'Opened investigation compliance case for high match score on Vance Ironworks Syria.', '192.168.5.5');

-- Link seed client user to Corporate Client 1
UPDATE `users` SET `corporate_client_id` = 1 WHERE `username` = 'client';

-- Guarantee that all default seeded users have password 'password' and status 'ACTIVE'
UPDATE `users` 
SET `password` = '$2a$10$8.UnVuG9HHgffUDAlk8GPuRGTwRKBt18aRjN.GJCp4e9.1z40.Ety', 
    `status` = 'ACTIVE' 
WHERE `username` IN ('client', 'ops', 'relationship', 'treasury', 'compliance', 'admin');
