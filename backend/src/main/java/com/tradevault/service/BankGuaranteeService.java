package com.tradevault.service;

import com.tradevault.entity.*;
import com.tradevault.entity.enums.BankGuaranteeStatus;
import com.tradevault.entity.enums.BGClaimStatus;
import com.tradevault.entity.enums.BGType;
import com.tradevault.exception.BadRequestException;
import com.tradevault.exception.ResourceNotFoundException;
import com.tradevault.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class BankGuaranteeService {

    private static final Logger logger = LoggerFactory.getLogger(BankGuaranteeService.class);

    @Autowired
    private BankGuaranteeRepository bgRepository;

    @Autowired
    private CreditFacilityRepository facilityRepository;

    @Autowired
    private CorporateClientRepository clientRepository;

    @Autowired
    private BGClaimRepository claimRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SanctionsScreeningService sanctionsScreeningService;

    @Autowired
    private SanctionsScreeningRepository sanctionsScreeningRepository;

    // ─── Read Operations ─────────────────────────────────────────────────────

    public List<BankGuarantee> getAllBGs() {
        logger.debug("Fetching all Bank Guarantees ordered by creation date");
        List<BankGuarantee> bgs = bgRepository.findAllByOrderByCreatedAtDesc();
        logger.info("Retrieved {} Bank Guarantees", bgs.size());
        return bgs;
    }

    public List<BankGuarantee> getBGsByClientId(Long clientId) {
        logger.debug("Fetching Bank Guarantees for clientId={}", clientId);
        List<BankGuarantee> bgs = bgRepository.findByClientId(clientId);
        logger.info("Retrieved {} Bank Guarantees for clientId={}", bgs.size(), clientId);
        return bgs;
    }

    public BankGuarantee getBGById(Long id) {
        logger.debug("Fetching Bank Guarantee with id={}", id);
        return bgRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Bank Guarantee not found with id={}", id);
                    return new ResourceNotFoundException("Bank Guarantee not found with id: " + id);
                });
    }

    // ─── Create BG ───────────────────────────────────────────────────────────

    @Transactional
    public BankGuarantee createBG(BankGuarantee bg, Long clientId, Long facilityId, String username) {
        logger.info("BG creation initiated by user='{}' for clientId={}, facilityId={}", username, clientId, facilityId);

        CorporateClient client = clientRepository.findById(clientId)
                .orElseThrow(() -> {
                    logger.error("Corporate Client not found: clientId={}", clientId);
                    return new ResourceNotFoundException("Corporate Client not found");
                });

        CreditFacility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> {
                    logger.error("Credit Facility not found: facilityId={}", facilityId);
                    return new ResourceNotFoundException("Credit Facility not found");
                });

        if (!facility.getClient().getId().equals(client.getId())) {
            logger.warn("Facility-client mismatch: facilityId={} does not belong to clientId={}", facilityId, clientId);
            throw new BadRequestException("Credit facility does not belong to the selected client");
        }

        // Limit Check for BG
        BigDecimal availableLimit = facility.getLimitAmount().subtract(facility.getUtilizedAmount());
        logger.debug("BG facility limit check: requested={}, available={}, facilityId={}", bg.getAmount(), availableLimit, facilityId);
        if (bg.getAmount().compareTo(availableLimit) > 0) {
            logger.warn("Insufficient credit facility limit for BG. requested={}, available={}, facilityId={}",
                    bg.getAmount(), availableLimit, facilityId);
            throw new BadRequestException("Insufficient credit facility limit. Available: " + availableLimit + " USD");
        }

        bg.setClient(client);
        bg.setCreditFacility(facility);
        bg.setIssueDate(LocalDate.now());
        bg.setStatus(BankGuaranteeStatus.DRAFT);

        if (bg.getBgNumber() == null || bg.getBgNumber().trim().isEmpty()) {
            bg.setBgNumber("BG-" + LocalDate.now().getYear() + "-" + String.format("%04d", (int)(Math.random() * 9000 + 1000)));
        }

        BankGuarantee savedBG = bgRepository.save(bg);
        logger.info("Bank Guarantee created: bgNumber='{}', amount={} {}, clientId={}, by user='{}'",
                savedBG.getBgNumber(), savedBG.getAmount(), savedBG.getCurrency(), clientId, username);

        auditLogService.log(null, username, "BG_CREATION_DRAFT",
                "Created BG Draft: " + savedBG.getBgNumber() + " for " + savedBG.getAmount() + " " + savedBG.getCurrency(), null);

        // Sanctions screening trigger
        logger.info("Triggering sanctions screening for BG beneficiary='{}'", bg.getBeneficiaryName());
        sanctionsScreeningService.screenEntity(bg.getBeneficiaryName(), "BENEFICIARY", "BG", savedBG.getBgNumber());

        return savedBG;
    }

    // ─── Status Update ────────────────────────────────────────────────────────

    @Transactional
    public BankGuarantee updateStatus(Long id, String statusStr, String username) {
        logger.info("BG status update requested by user='{}': bgId={}, targetStatus='{}'", username, id, statusStr);
        BankGuarantee bg = getBGById(id);
        BankGuaranteeStatus oldStatus = bg.getStatus();
        BankGuaranteeStatus status = BankGuaranteeStatus.valueOf(statusStr.toUpperCase());

        // COMPLIANCE HOLD check
        boolean hasComplianceHold = !sanctionsScreeningRepository
                .findByTransactionIdAndStatus(bg.getBgNumber(), "FLAGGED").isEmpty();
        if (hasComplianceHold) {
            logger.warn("COMPLIANCE HOLD: Status update blocked for BG='{}', user='{}'. Unresolved FLAGGED screening exists.",
                     bg.getBgNumber(), username);
            auditLogService.log(null, username, "COMPLIANCE_HOLD_BLOCKED",
                     "Blocked status update on BG " + bg.getBgNumber() + " — open compliance hold (FLAGGED screening). Resolve via Compliance module first.", null);
            throw new IllegalStateException(
                     "COMPLIANCE_HOLD: Bank Guarantee '" + bg.getBgNumber() + "' has an unresolved sanctions screening flag. " +
                     "A Compliance Manager must clear or block this entity before status can be advanced.");
        }

        bg.setStatus(status);

        if (status == BankGuaranteeStatus.ACTIVE && oldStatus != BankGuaranteeStatus.ACTIVE) {
            logger.info("BG transitioning to ACTIVE — blocking facility limit. bgNumber='{}', amount={}", bg.getBgNumber(), bg.getAmount());
            CreditFacility facility = bg.getCreditFacility();
            BigDecimal available = facility.getLimitAmount().subtract(facility.getUtilizedAmount());
            if (bg.getAmount().compareTo(available) > 0) {
                logger.warn("Facility limit exceeded during BG activation. bgNumber='{}', required={}, available={}",
                         bg.getBgNumber(), bg.getAmount(), available);
                throw new BadRequestException("Cannot activate BG: Facility limit exceeded. Available: " + available);
            }
            facility.setUtilizedAmount(facility.getUtilizedAmount().add(bg.getAmount()));
            facilityRepository.save(facility);
            logger.info("Facility limit updated for BG activation: facilityId={}, newUtilized={}", facility.getId(), facility.getUtilizedAmount());
        } else if (status == BankGuaranteeStatus.RELEASED && oldStatus == BankGuaranteeStatus.ACTIVE) {
            // Refund utilized limit
            logger.info("BG released — refunding facility limit. bgNumber='{}', amount={}", bg.getBgNumber(), bg.getAmount());
            CreditFacility facility = bg.getCreditFacility();
            BigDecimal refundAmount = bg.getAmount().min(facility.getUtilizedAmount());
            facility.setUtilizedAmount(facility.getUtilizedAmount().subtract(refundAmount));
            facilityRepository.save(facility);
            logger.info("Facility limit refunded: facilityId={}, refunded={}, newUtilized={}", facility.getId(), refundAmount, facility.getUtilizedAmount());
        }

        BankGuarantee updated = bgRepository.save(bg);
        logger.info("BG status updated: bgNumber='{}', from='{}' to='{}', by user='{}'",
                 bg.getBgNumber(), oldStatus, status, username);
        auditLogService.log(null, username, "BG_STATUS_UPDATE",
                 "Updated BG status: " + bg.getBgNumber() + " from " + oldStatus + " to " + status, null);

        return updated;
    }

    // ─── Submit for Approval ──────────────────────────────────────────────────

    @Transactional
    public BankGuarantee submitForApproval(Long id, String username) {
        logger.info("BG submitted for approval by user='{}': bgId={}", username, id);
        BankGuarantee bg = getBGById(id);
        if (bg.getStatus() != BankGuaranteeStatus.DRAFT) {
            logger.warn("Cannot submit non-DRAFT BG for approval: bgId={}, currentStatus='{}'", id, bg.getStatus());
            throw new BadRequestException(
                "Only DRAFT guarantees can be submitted for approval. Current status: " + bg.getStatus());
        }
        bg.setStatus(BankGuaranteeStatus.PENDING_APPROVAL);
        BankGuarantee updated = bgRepository.save(bg);
        logger.info("BG submitted for approval: bgNumber='{}', status changed to PENDING_APPROVAL", bg.getBgNumber());
        auditLogService.log(null, username, "BG_SUBMITTED_FOR_APPROVAL",
                "Client submitted BG for Operations review: " + bg.getBgNumber(), null);
        return updated;
    }

    // ─── Claims ───────────────────────────────────────────────────────────────

    @Transactional
    public BGClaim fileClaim(Long bgId, BigDecimal amount, String paymentDetails, String username) {
        logger.info("BG claim filed by user='{}': bgId={}, claimAmount={}", username, bgId, amount);
        BankGuarantee bg = getBGById(bgId);

        BGClaim claim = new BGClaim();
        claim.setBg(bg);
        claim.setClaimRef("CLM-" + bg.getBgNumber() + "-" + System.currentTimeMillis() % 10000);
        claim.setAmount(amount);
        claim.setClaimDate(LocalDate.now());
        claim.setStatus(BGClaimStatus.PENDING);
        claim.setPaymentDetails(paymentDetails);

        BGClaim saved = claimRepository.save(claim);
        logger.info("BG claim created: claimRef='{}', bgNumber='{}', amount={}", saved.getClaimRef(), bg.getBgNumber(), amount);
        return saved;
    }

    @Transactional
    public BGClaim processClaim(Long claimId, String statusStr, String username) {
        logger.info("Processing BG claim claimId={} with status='{}' by user='{}'", claimId, statusStr, username);
        BGClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> {
                    logger.warn("BG claim not found: claimId={}", claimId);
                    return new ResourceNotFoundException("BG claim not found");
                });

        BGClaimStatus status = BGClaimStatus.valueOf(statusStr.toUpperCase());
        claim.setStatus(status);

        if (status == BGClaimStatus.APPROVED) {
            BankGuarantee bg = claim.getBg();
            bg.setStatus(BankGuaranteeStatus.CLAIMED);
            bgRepository.save(bg);
            logger.info("BG marked CLAIMED after approved claim: bgNumber='{}', claimRef='{}'", bg.getBgNumber(), claim.getClaimRef());

            // Release facility utilization
            CreditFacility facility = bg.getCreditFacility();
            BigDecimal releaseAmount = bg.getAmount().min(facility.getUtilizedAmount());
            facility.setUtilizedAmount(facility.getUtilizedAmount().subtract(releaseAmount));
            facilityRepository.save(facility);
            logger.info("Facility limit released after BG claim approval: facilityId={}, released={}", facility.getId(), releaseAmount);

            auditLogService.log(null, username, "BG_CLAIM_APPROVED",
                    "Approved BG Claim: " + claim.getClaimRef() + " of " + claim.getAmount() + " USD. Settled and closed BG.", null);
        } else {
            logger.info("BG claim status updated to '{}': claimRef='{}'", status, claim.getClaimRef());
        }

        BGClaim result = claimRepository.save(claim);
        logger.info("BG claim processed: claimRef='{}', finalStatus='{}'", claim.getClaimRef(), status);
        return result;
    }

    public List<BGClaim> getClaims(Long bgId) {
        logger.debug("Fetching claims for bgId={}", bgId);
        return claimRepository.findByBgId(bgId);
    }
}
