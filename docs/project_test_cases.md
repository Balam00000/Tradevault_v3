# Project Test Cases Reference Guide

This document catalogs all the **105 active test cases** implemented in the TradeVault project. Use this as a reference guide to locate specific verification logic.

---

## 1. Controller & Integration Tests (`com.tradevault.controller`)

These tests verify endpoint access control, Spring Security rules, and end-to-end database/API integration.

### AuthControllerTest
*   **`register_success`**: Verifies new user registration completes successfully and returns `200 OK`.
*   **`register_duplicateUsername_returnsBadRequest`**: Verifies duplicate username registration is blocked and returns `400 Bad Request`.
*   **`register_duplicateEmail_returnsBadRequest`**: Verifies duplicate email registration is blocked and returns `400 Bad Request`.
*   **`login_success_returnsJwt`**: Verifies successful login returns a JWT bearer token.
*   **`login_pendingUser_returns403`**: Verifies user login is blocked if their onboarding status is pending.
*   **`getCurrentUser_success`**: Verifies retrieving the logged-in user details.

### CorporateControllerTest
*   **`getAllClients_adminUser_returnsAllClients`**: Verifies admins see all clients.
*   **`getAllClients_operationsUser_returnsClients`**: Verifies operations staff see all clients.
*   **`getAllClients_clientUser_returnsOwnClientOnly`**: Verifies client users see only their own company data.
*   **`getAllClients_relationshipManager_returnsAssignedClientsOnly`**: Verifies RM users can only fetch clients assigned to them.
*   **`getClientById_relationshipManager_deniedForNonAssigned`**: Verifies access denial when RM tries to retrieve a client outside their portfolio.
*   **`createClient_adminUser_success`**: Verifies clients can be registered by admins.
*   **`updateClient_adminUser_canAssignRelationshipManager`**: Verifies admins can successfully assign/map an RM to a client.
*   **`getAllFacilities_adminUser_returnsAll`**: Verifies fetching all credit facilities.
*   **`createFacility_adminUser_success`**: Verifies creating credit facilities.
*   **`getAllClients_noAuth_returns403`** / **`createClient_operationsUser_returns403`**: Verifies general security controls.

### ComplianceControllerTest
*   **`getScreenings_complianceUser_returns200`**: Verifies compliance officers can fetch sanctions screening logs.
*   **`getScreenings_operationsUser_returns403`** / **`getScreenings_noToken_returns403`**: Verifies role-based access denial.
*   **`getCases_complianceUser_returns200`**: Verifies compliance officers can fetch open/closed cases.
*   **`resolveCase_complianceUser_success`**: Verifies compliance officers can resolve sanctions screening holds.
*   **`resolveCase_operationsUser_returns403`**: Verifies ops users cannot resolve compliance holds.

### RelationshipManagerScopeIntegrationTest
This suite checks the transactional and visibility boundaries for Relationship Managers:
*   **`getCorporates_filtersByRelationshipManager`**: Verifies RM client list filtering.
*   **`getCorporateById_RM1_deniedForClientB`**: Blocks RM from viewing unassigned client details.
*   **`getFacilities_filtersByRelationshipManager`**: Filters credit facility listings by RM assignments.
*   **`getLCs_filtersByRelationshipManager`**: Filters Letter of Credit (LC) listings by RM assignments.
*   **`getLcById_RM1_deniedForLcB`**: Blocks viewing details of unassigned LCs.
*   **`createLc_RM1_deniedForClientB`**: Blocks RM from issuing LCs for unassigned clients.
*   **`getBGs_filtersByRelationshipManager`**: Filters Bank Guarantee (BG) listings by RM assignments.
*   **`getBgById_RM1_deniedForBgB`**: Blocks viewing unassigned BG details.
*   **`getBills_filtersByRelationshipManager`**: Filters Export Bills by RM assignments.
*   **`getCollections_filtersByRelationshipManager`**: Filters Collection Instructions by RM.
*   **`getAnalyticsSummary_RM1_returnsClientAStats`**: Restricts RM dashboard statistics to their own client portfolio.
*   **`processAmendment_RM1_deniedForLcB`**: Blocks RM from approving/processing unassigned LC amendments.
*   **`updateBgStatus_RM1_deniedForBgB`**: Blocks RM from updating status of unassigned BGs.
*   **`updateLcStatus_RM1_deniedForLcB`**: Blocks RM from updating status of unassigned LCs.
*   **`requestAmendment_RM1_deniedForLcB`**: Blocks requesting amendments on unassigned LCs.
*   **`getAmendments_RM1_deniedForLcB`** / **`getDrawings_RM1_deniedForLcB`**: Blocks accessing sub-resources.
*   **`presentDrawing_RM1_deniedForLcB`**: Blocks drawing presentations on unassigned LCs.
*   **`getClaims_RM1_deniedForBgB`** / **`fileClaim_RM1_deniedForBgB`**: Blocks claims activity on unassigned BGs.
*   **`createBill_RM1_deniedForClientB`**: Blocks creating Export Bills for unassigned clients.
*   **`createCollection_RM1_deniedForClientB`**: Blocks creating Collection Instructions for unassigned clients.
*   **`getBillsByClientId_RM1_deniedForClientB`** / **`getCollectionsByClientId_RM1_deniedForClientB`**: Blocks client-scoped listings.

---

## 2. Service Unit Tests (`com.tradevault.service`)

These tests verify complex core business computations, limit controls, and data manipulation using Mockito unit tests.

### LetterOfCreditServiceTest
*   **`getAllLCs_returnsAllLCs`**: Verifies fetching all letters of credit.
*   **`getLCById_found_returnsLC`** / **`getLCById_notFound_throwsException`**: Verifies lookup behavior.
*   **`createLC_success`**: Checks LC creation under draft status with sanctions screening.
*   **`createLC_insufficientLimit_throwsBadRequest`**: Blocks LC creation if requested amount exceeds the active credit facility limit.
*   **`createLC_facilityClientMismatch_throwsBadRequest`**: Prevents facility usage if it doesn't belong to the selected client.
*   **`updateStatus_toActive_blocksFacilityLimit`**: Confirms that transitioning an LC to `ACTIVE` blocks (deducts) the amount from the credit facility.
*   **`updateStatus_complianceHold_throwsIllegalState`**: Blocks transition if sanctions screening matches a flagged watch list.
*   **`requestAmendment_success`**: Verifies amendment registration.
*   **`processAmendment_approved_adjustsFacilityLimit`**: Dynamically updates facility limits on approval.
*   **`presentDrawing_success_allDocuments`**: Verifies drawing transitions.
*   **`presentDrawing_missingDocs_markedDiscrepant`**: Marks drawings discrepant if required documentation is missing.
*   **`processDrawing_paid_releasesLimit`**: Releases limits on payment.
*   **`getLCsByRelationshipManagerId_returnsLCs`**: Verifies RM portfolio retrieval.

### BankGuaranteeServiceTest
*   **`getAllBGs_returnsAll`**: Verifies retrieving all BGs.
*   **`getBGById_notFound_throwsException`**: Verifies error handling.
*   **`createBG_success`** / **`createBG_insufficientLimit_throwsBadRequest`**: Validates limit checks on BG creation.
*   **`updateStatus_toActive_blocksFacility`** / **`updateStatus_released_refundsFacility`**: Controls limit allocation and return.
*   **`updateStatus_complianceHold_throws`**: Blocks operations on flagged BGs.
*   **`submitForApproval_success`** / **`submitForApproval_notDraft_throwsBadRequest`**: Checks state machine transitions.
*   **`fileClaim_success`** / **`processClaim_approved_releasesLimit`**: Handles claim submissions and limit releases.
*   **`getBGsByRelationshipManagerId_returnsBGs`**: Verifies RM-scoping.

### ExportBillServiceTest
*   **`getAllBills_returnsAll`** / **`getBillById_notFound_throwsException`**: Lookup validation.
*   **`createBill_success`** / **`createBill_clientNotFound_throwsException`**: Verifies Bill creation and sanctions checks.
*   **`updateBillStatus_success`** / **`updateBillStatus_complianceHold_throws`**: Regulates status updates.
*   **`createInstruction_success`** / **`updateInstructionStatus_success`**: Verifies Collection Instruction flows.
*   **`getBillsByRelationshipManagerId_returnsBills`** / **`getInstructionsByRelationshipManagerId_returnsInstructions`**: RM portfolio queries.

### TradeAnalyticsServiceTest
*   **`getAnalyticsSummary_emptyData_returnsZeros`**: Handles edge case with empty database tables.
*   **`getAnalyticsSummary_withActiveLCs`**: Confirms global statistics calculate exposure and totals correctly.
*   **`getAnalyticsSummary_utilizationRateCalculation`**: Verifies percentage math for credit facilities.
*   **`getAnalyticsSummary_onlyCountsActiveFacilities`**: Ignores expired facility limits.
*   **`getAnalyticsSummaryForClient_success`**: Scopes stats to a specific corporate client.
*   **`getAnalyticsSummaryForRelationshipManager_success`**: Scopes stats to an RM.

### SanctionsScreeningServiceTest
*   **`screenEntity_cleared_safeEntity`**: Clears safe entity names.
*   **`screenEntity_flagged_iranEntity_createsComplianceCase`**: Flag high risk matches (e.g. IRAN) and generates compliance cases automatically.
*   **`screenEntity_flagged_syriaEntity`**: Flags high risk matches (e.g. SYRIA).
*   **`screenEntity_cleared_shanghaiLowScore`**: Ignores low watchlist match scores.
*   **`resolveCase_resolvedCleared_updatesScreening`**: Unblocks screening status when case is cleared.
*   **`resolveCase_resolvedBlocked_keepsFlagged`**: Holds screening status as flagged on confirmation.
*   **`resolveCase_notFound_throws`** / **`getAllScreenings_returnsOrdered`** / **`getAllCases_returnsOrdered`**: Lookup and list operations.

### AuditLogServiceTest
*   **`log_persistsAuditEntry`**: Asserts logs are correctly formatted and written.
*   **`log_repositoryFailure_doesNotPropagate`**: Ensures audit log repository failures do not crash the primary transaction (fail-safe).
*   **`getAllLogs_returnsOrderedLogs`** / **`log_nullUserId_doesNotThrow`**: Edge-case testing.

### NotificationServiceTest (Added for Reference)
*   **`sendNotification_success`**: Verifies single notification issuance.
*   **`broadcastNotification_success`**: Verifies broadcast issuance to multiple users.
*   **`getNotificationsForUser_success`** / **`getUnreadNotifications_success`**: Validates inbox lists.
*   **`markAsRead_notificationExists_updatesStatus`** / **`markAsRead_notificationNotFound_doesNothing`**: Read flag handlers.
