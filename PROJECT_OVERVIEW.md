# 🛡️ TradeVault Platform Overview & Module Workflows

TradeVault is a secure, enterprise-grade, multi-tenant Corporate Banking & Trade Finance platform. It is designed to manage complex trade finance pipelines, automate risk screening, and enforce strict role-based transaction audits.

---

## 📋 1. Project Definition

**TradeVault** is a full-stack platform that acts as the digital infrastructure for corporate trade finance. It facilitates the creation, screening, tracking, amendment, and settlement of standard trade finance instruments:
*   **Letters of Credit (LC)**: Guarantees issued by a buyer's bank to ensure payment to a seller once compliance conditions are met.
*   **Bank Guarantees (BG)**: Guarantees that a bank will cover a corporate client's liabilities if they default on a contract.
*   **Export Bills & Collections**: Trade instruments for dispatching and securing payment for exported goods.

---

## 🎯 2. Why and Where TradeVault is Used

### Why it is Used
International trade involves substantial counterparty and financial risks (e.g., currency changes, shipping delays, default risk, and regulatory non-compliance). TradeVault solves these challenges by providing:
1.  **Risk Mitigation**: Secures transactions using bank-backed financial guarantees.
2.  **Maker-Checker Compliance**: Enforces dual-authorization workflows to prevent rogue or incorrect transactions.
3.  **Automated Watchlist Auditing**: Screens entities against global sanctions watchlists (e.g., OFAC, UN) in real-time, preventing financial crimes.
4.  **Credit Limit Controls**: Tracks bank-wide and client-specific credit facilities to prevent credit limit breaches.
5.  **Audit Logs**: Provides a persistent audit log of all critical operations for internal and regulatory reporting.

### Where it is Used
TradeVault is deployed in commercial banks and corporate trade offices. The target users and their workspaces include:
*   **Corporate Clients**: Access the portal to apply for Letters of Credit, submit Bank Guarantees, file claims, and inspect their credit facility utilization.
*   **Trade Operations (Makers)**: Validate submitted documents, flags discrepancies, and verify amendments.
*   **Relationship Managers (Checkers)**: Manage client relationships, audit collateral, approve credit lines, and activate checked trade instruments.
*   **Compliance Officers**: Review flagged sanctions screens and resolve compliance cases.
*   **Treasury Managers**: Access high-level exposure charts and track liquidity requirements.
*   **System Administrators**: Manage user accounts and map users to corporate tenants.

---

## ⚙️ 3. Core Module Workflows & Lifecycles

### 3.1 Letter of Credit (LC) Lifecycle
The LC module utilizes a strict **Maker-Checker** pattern to verify financial limits:

```mermaid
graph TD
    A[DRAFT] -->|Client Submits| B[IN_REVIEW]
    B -->|Operations Maker Approves| C[APPROVED]
    B -->|Operations Maker Rejects| D[REJECTED]
    C -->|RM Checker Activates| E[ACTIVE]
    E -->|Amendment Requested| F[AMENDMENT_PENDING]
    F -->|Approved| G[AMENDED]
    E -->|Beneficiary Presents Docs| H[DRAWN / PAID]
    E -->|Expiry Date Passes| I[EXPIRED]
```

1.  **Draft Creation**: The Corporate Client drafts an LC. At this stage, no credit is utilized.
2.  **Maker Check**: A Trade Operations officer reviews the parameters. If valid, they mark it as `APPROVED`.
3.  **Checker Check**: A Relationship Manager verifies the client's collateral. When marked `ACTIVE`, the utilized credit amount is deducted from the credit facility.
4.  **Presentation & Drawing**: The beneficiary presents documents (e.g., Bill of Lading, Invoice). If documents are missing, the system automatically marks the drawing as `DISCREPANT`. Once validated, the funds are paid and facility limits are updated.

---

### 3.2 Bank Guarantee (BG) & Claims Flow
The BG module supports Bid Bonds, Performance Bonds, and payment guarantees:

```mermaid
stateDiagram-v2
    [*] --> DRAFT : Client Creates
    DRAFT --> PENDING_APPROVAL : Client Submits
    PENDING_APPROVAL --> ACTIVE : RM / Ops Approves & Utilizes Limit
    ACTIVE --> CLAIM_RECEIVED : Beneficiary Files Default Claim
    CLAIM_RECEIVED --> CLAIMED : Claim Approved & Settled
    ACTIVE --> RELEASED : BG Discharged / Limit Refunded
```

1.  **Issuance**: Client initiates a guarantee. Once approved, the utilized limit is blocked.
2.  **Claim Filing**: If the client defaults, the beneficiary files a `BGClaim` against the active guarantee.
3.  **Claim Settlement**: The bank reviews the claim. If approved, the guarantee status is set to `CLAIMED` and the utilized limit is refunded.

---

### 3.3 Sanctions Screening & Compliance Holds
To satisfy regulatory requirements, TradeVault embeds an automated real-time compliance screening watchdog:

```mermaid
sequenceDiagram
    autonumber
    Participant Client as Corporate Client
    Participant Engine as Screening Engine
    Participant CaseRepo as Compliance Case Repository
    Participant Service as LetterOfCreditService
    Participant CompOfficer as Compliance Officer

    Client->>Service: Create LC / BG
    Service->>Engine: Screen Applicant & Beneficiary
    alt Risk Match Found (e.g. contains 'SYRIA', 'IRAN', 'SUDAN')
        Engine-->>Service: FLAGGED status
        Service->>CaseRepo: Create OPEN ComplianceCase
        Note over Service: compliance lock active
        Client->>Service: Attempt Status Update (e.g. to ACTIVE)
        Service-->>Client: Blocks update (IllegalStateException)
    else Cleared
        Engine-->>Service: CLEARED status
    end

    CompOfficer->>CaseRepo: Resolve Case (Mark CLEARED)
    Note over Service: compliance lock released
    Client->>Service: Status Update (Succeeds)
```

*   **Trigger**: Any instrument creation automatically screens applicants and beneficiaries against watchlist patterns.
*   **Holds**: A `FLAGGED` status creates an open `ComplianceCase` and locks the instrument. No state changes are permitted.
*   **Resolution**: A Compliance Officer must explicitly review the case and resolve it to unlock the instrument.

---

### 3.4 Export Bills & Collections Flow
This module oversees the dispatch of trade shipping documents and collects payments from foreign buyers (drawees):

```mermaid
graph TD
    A[Bill INITIATED] -->|Operations Prepares Docs| B[DOCUMENTS_PREPARED]
    B -->|Dispatched to Foreign Bank| C[DOCUMENTS_SENT]
    C -->|Drawee Accepts Bill| D[ACCEPTED]
    D -->|Overseas Payout Received| E[PAID]
```

*   **Workflow Steps**:
    1.  **Initiation**: Corporate Client registers an Export Bill detailing invoice amounts, carrier tracking, and foreign drawee names.
    2.  **Compliance Screening**: The drawee's name is automatically screened for sanctions.
    3.  **Collection Instruction**: A companion collection instruction is generated to direct the correspondent bank to release shipping documents only against payment (D/P) or acceptance (D/A).
    4.  **Tracking & Settlement**: As the foreign buyer pays or accepts, status transitions from `DOCUMENTS_SENT` to `ACCEPTED`, and finally to `PAID`.

---

### 3.5 Identity & User Governance Onboarding Flow
To ensure data security, new registrations are subject to a strict admin verification loop:

```mermaid
flowchart TD
    Start([User Registration Request]) --> A[User Account Created: PENDING Status]
    A --> B[Admin Reviews Request Details]
    B -->|Denied| C[Account Blocked / Rejected]
    B -->|Approved| D[Account Status: ACTIVE]
    D --> E{User Role?}
    E -->|CLIENT| F[Admin Maps User to a CorporateClient Tenant]
    E -->|RM/Ops/Treasury| G[Access Scoped to Bank-Wide Desk]
    F --> End([User Onboarding Complete])
    G --> End
```

*   **Multitenancy Boundary**: A client user cannot view or edit any transaction unless they are successfully mapped to a `CorporateClient` record. All SQL queries are implicitly filtered by `corporate_client_id` for security.

---

### 3.6 Expiry Alerts & Audit Pipeline Flow
The platform automates instrument lifecycle warnings and maintains a tamper-evident audit history:

```mermaid
graph TD
    CronJob([Daily Scheduler 08:00 UTC]) --> A{Query Database}
    A -->|LC expiring <= 7 Days| B[Generate Alert Notification]
    A -->|BG expiring <= 14 Days| C[Generate Alert Notification]
    B & C --> D[Save to Notifications Table]
    D --> E[Notify Target Corporate Client User]
    
    UserAction([User Action: e.g. Approve BG / Resolve Case]) --> F[Execute Business Action]
    F --> G[Call AuditLogService]
    G --> H[Write Audit Entry: Action, Details, IP Address, Timestamp]
```

*   **Tamper Evidence**: All critical operations invoke the `AuditLogService`. Logs are persisted with a timestamp and IP address to guarantee a reliable audit trail for compliance desks.
