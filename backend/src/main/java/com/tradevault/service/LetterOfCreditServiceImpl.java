package com.tradevault.service;

import com.tradevault.entity.*;
import com.tradevault.entity.enums.LetterOfCreditStatus;
import com.tradevault.entity.enums.LCAmendmentStatus;
import com.tradevault.entity.enums.LCDrawingStatus;
import com.tradevault.entity.enums.SanctionsScreeningStatus;
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
public class LetterOfCreditServiceImpl implements LetterOfCreditService {

    private static final Logger logger = LoggerFactory.getLogger(LetterOfCreditServiceImpl.class);

    @Autowired
    private LetterOfCreditRepository lcRepository;

    @Autowired
    private CreditFacilityRepository facilityRepository;

    @Autowired
    private CorporateClientRepository clientRepository;

    @Autowired
    private LCAmendmentRepository amendmentRepository;

    @Autowired
    private LCDrawingRepository drawingRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SanctionsScreeningService sanctionsScreeningService;

    @Autowired
    private SanctionsScreeningRepository sanctionsScreeningRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    // ─── Read Operations ─────────────────────────────────────────────────────

    public List<LetterOfCredit> getAllLCs() {
        logger.debug("Fetching all Letters of Credit ordered by creation date");
        List<LetterOfCredit> lcs = lcRepository.findAllByOrderByCreatedAtDesc();
        logger.info("Retrieved {} Letters of Credit", lcs.size());
        return lcs;
    }

    public List<LetterOfCredit> getLCsByClientId(Long clientId) {
        logger.debug("Fetching Letters of Credit for clientId={}", clientId);
        List<LetterOfCredit> lcs = lcRepository.findByClientId(clientId);
        logger.info("Retrieved {} Letters of Credit for clientId={}", lcs.size(), clientId);
        return lcs;
    }

    public List<LetterOfCredit> getLCsByRelationshipManagerId(Long rmId) {
        logger.debug("Fetching Letters of Credit for relationshipManagerId={}", rmId);
        List<LetterOfCredit> lcs = lcRepository.findByClientRelationshipManagerId(rmId);
        logger.info("Retrieved {} Letters of Credit for relationshipManagerId={}", lcs.size(), rmId);
        return lcs;
    }

    public LetterOfCredit getLCById(Long id) {
        logger.debug("Fetching Letter of Credit with id={}", id);
        return lcRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Letter of Credit not found with id={}", id);
                    return new ResourceNotFoundException("Letter of Credit not found with id: " + id);
                });
    }

    // ─── Create LC ───────────────────────────────────────────────────────────

    @Transactional
    public LetterOfCredit createLC(LetterOfCredit lc, Long clientId, Long facilityId, String username) {
        logger.info("LC creation initiated by user='{}' for clientId={}, facilityId={}", username, clientId, facilityId);

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

        // Limit Check
        BigDecimal availableLimit = facility.getLimitAmount().subtract(facility.getUtilizedAmount());
        logger.debug("Facility limit check: requested={}, available={}, facilityId={}", lc.getAmount(), availableLimit, facilityId);
        if (lc.getAmount().compareTo(availableLimit) > 0) {
            logger.warn("Insufficient credit facility limit for LC. Requested={}, Available={}, facilityId={}", lc.getAmount(), availableLimit, facilityId);
            throw new BadRequestException("Insufficient credit facility limit. Available: " + availableLimit + " USD");
        }

        lc.setClient(client);
        lc.setCreditFacility(facility);
        lc.setIssueDate(LocalDate.now());
        lc.setStatus(LetterOfCreditStatus.DRAFT);

        if (lc.getLcNumber() == null || lc.getLcNumber().trim().isEmpty()) {
            lc.setLcNumber("LC-" + LocalDate.now().getYear() + "-" + String.format("%04d", (int)(Math.random() * 9000 + 1000)));
        }

        LetterOfCredit savedLC = lcRepository.save(lc);
        logger.info("Letter of Credit created: lcNumber='{}', amount={} {}, clientId={}, by user='{}'",
                savedLC.getLcNumber(), savedLC.getAmount(), savedLC.getCurrency(), clientId, username);

        auditLogService.log(null, username, "LC_CREATION_DRAFT",
                "Created LC Draft: " + savedLC.getLcNumber() + " for " + savedLC.getAmount() + " " + savedLC.getCurrency(), null);

        // Sanctions screening trigger
        logger.info("Triggering sanctions screening for LC applicant='{}' and beneficiary='{}'",
                lc.getApplicantName(), lc.getBeneficiaryName());
        sanctionsScreeningService.screenEntity(lc.getApplicantName(), "APPLICANT", "LC", savedLC.getLcNumber());
        sanctionsScreeningService.screenEntity(lc.getBeneficiaryName(), "BENEFICIARY", "LC", savedLC.getLcNumber());

        return savedLC;
    }

    // ─── Status Update ────────────────────────────────────────────────────────

    @Transactional
    public LetterOfCredit updateStatus(Long id, LetterOfCreditStatus status, String username) {
        logger.info("LC status update requested by user='{}': lcId={}, targetStatus='{}'", username, id, status);
        LetterOfCredit lc = getLCById(id);
        LetterOfCreditStatus oldStatus = lc.getStatus();

        // COMPLIANCE HOLD check
        boolean hasComplianceHold = !sanctionsScreeningRepository
                .findByTransactionIdAndStatus(lc.getLcNumber(), SanctionsScreeningStatus.FLAGGED).isEmpty();
        if (hasComplianceHold) {
            logger.warn("COMPLIANCE HOLD: Status update blocked for LC='{}', user='{}'. Unresolved FLAGGED screening exists.",
                    lc.getLcNumber(), username);
            auditLogService.log(null, username, "COMPLIANCE_HOLD_BLOCKED",
                    "Blocked status update on LC " + lc.getLcNumber() + " — open compliance hold (FLAGGED screening). Resolve via Compliance module first.", null);
            throw new IllegalStateException(
                    "COMPLIANCE_HOLD: Letter of Credit '" + lc.getLcNumber() + "' has an unresolved sanctions screening flag. " +
                    "A Compliance Manager must clear or block this entity before status can be advanced.");
        }

        lc.setStatus(status);

        // Facility utilization on ACTIVE transition
        if (status == LetterOfCreditStatus.ACTIVE && oldStatus != LetterOfCreditStatus.ACTIVE && oldStatus != LetterOfCreditStatus.AMENDED) {
            logger.info("LC transitioning to ACTIVE — blocking facility limit. lcNumber='{}', amount={}", lc.getLcNumber(), lc.getAmount());
            CreditFacility facility = lc.getCreditFacility();
            BigDecimal available = facility.getLimitAmount().subtract(facility.getUtilizedAmount());
            if (lc.getAmount().compareTo(available) > 0) {
                logger.warn("Facility limit exceeded during LC activation. lcNumber='{}', required={}, available={}",
                        lc.getLcNumber(), lc.getAmount(), available);
                throw new BadRequestException("Cannot activate LC: Facility limit exceeded. Available: " + available);
            }
            facility.setUtilizedAmount(facility.getUtilizedAmount().add(lc.getAmount()));
            facilityRepository.save(facility);
            logger.info("Facility limit updated: facilityId={}, newUtilized={}", facility.getId(), facility.getUtilizedAmount());

            // Notify client
            notificationRepository.save(new Notification(
                    lc.getClient().getId(),
                    "Letter of Credit Active",
                    "Your Letter of Credit " + lc.getLcNumber() + " is now ACTIVE.",
                    "INFO"
            ));
            logger.debug("Activation notification sent to clientId={} for LC='{}'", lc.getClient().getId(), lc.getLcNumber());
        }

        LetterOfCredit updated = lcRepository.save(lc);
        logger.info("LC status updated: lcNumber='{}', from='{}' to='{}', by user='{}'",
                lc.getLcNumber(), oldStatus, status, username);
        auditLogService.log(null, username, "LC_STATUS_UPDATE",
                "Updated LC status: " + lc.getLcNumber() + " from " + oldStatus + " to " + status, null);

        return updated;
    }

    // ─── Amendment Logic ─────────────────────────────────────────────────────

    @Transactional
    public LCAmendment requestAmendment(Long lcId, BigDecimal newAmount, LocalDate newExpiryDate, String justification, String username) {
        logger.info("LC amendment requested by user='{}': lcId={}, newAmount={}, newExpiry={}", username, lcId, newAmount, newExpiryDate);
        LetterOfCredit lc = getLCById(lcId);

        LCAmendment amendment = new LCAmendment();
        amendment.setLc(lc);
        amendment.setPreviousAmount(lc.getAmount());
        amendment.setNewAmount(newAmount);
        amendment.setPreviousExpiryDate(lc.getExpiryDate());
        amendment.setNewExpiryDate(newExpiryDate);
        amendment.setJustification(justification);
        amendment.setCreatedBy(username);
        amendment.setStatus(LCAmendmentStatus.PENDING_APPROVAL);

        List<LCAmendment> amendments = amendmentRepository.findByLcIdOrderByAmendmentNumberDesc(lcId);
        int num = amendments.isEmpty() ? 1 : amendments.get(0).getAmendmentNumber() + 1;
        amendment.setAmendmentNumber(num);

        LCAmendment saved = amendmentRepository.save(amendment);
        logger.info("LC amendment created: amendmentNumber={}, lcNumber='{}', previousAmount={}, newAmount={}",
                num, lc.getLcNumber(), lc.getAmount(), newAmount);
        return saved;
    }

    @Transactional
    public LCAmendment processAmendment(Long amendmentId, String status, String username) {
        logger.info("Processing LC amendment amendmentId={} with status='{}' by user='{}'", amendmentId, status, username);
        LCAmendment amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> {
                    logger.warn("Amendment not found: amendmentId={}", amendmentId);
                    return new ResourceNotFoundException("Amendment not found");
                });

        LCAmendmentStatus statusEnum = LCAmendmentStatus.valueOf(status.toUpperCase());
        amendment.setStatus(statusEnum);
        LCAmendment saved = amendmentRepository.save(amendment);

        if (statusEnum == LCAmendmentStatus.APPROVED) {
            LetterOfCredit lc = amendment.getLc();
            BigDecimal diff = amendment.getNewAmount().subtract(amendment.getPreviousAmount());
            logger.debug("Amendment approved: amount difference={}, lcNumber='{}'", diff, lc.getLcNumber());

            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                CreditFacility facility = lc.getCreditFacility();
                BigDecimal available = facility.getLimitAmount().subtract(facility.getUtilizedAmount());
                if (diff.compareTo(available) > 0) {
                    logger.warn("Insufficient facility limit for amendment increase. required={}, available={}, facilityId={}",
                            diff, available, facility.getId());
                    throw new BadRequestException("Cannot approve amendment: Insufficient facility limit for the increase of " + diff + " USD");
                }
                facility.setUtilizedAmount(facility.getUtilizedAmount().add(diff));
                facilityRepository.save(facility);
                logger.info("Facility utilized amount increased by {} for amendment approval. facilityId={}", diff, facility.getId());
            } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                CreditFacility facility = lc.getCreditFacility();
                facility.setUtilizedAmount(facility.getUtilizedAmount().add(diff)); // diff is negative
                facilityRepository.save(facility);
                logger.info("Facility utilized amount decreased by {} for amendment approval. facilityId={}", diff.abs(), facility.getId());
            }

            lc.setAmount(amendment.getNewAmount());
            lc.setExpiryDate(amendment.getNewExpiryDate());
            lc.setStatus(LetterOfCreditStatus.AMENDED);
            lcRepository.save(lc);
            logger.info("LC amended successfully: lcNumber='{}', newAmount={}, newExpiry={}", lc.getLcNumber(), lc.getAmount(), lc.getExpiryDate());

            auditLogService.log(null, username, "LC_AMENDMENT_APPROVED",
                    "Approved Amendment #" + amendment.getAmendmentNumber() + " for LC: " + lc.getLcNumber() + ". New Amount: " + lc.getAmount(), null);
        } else {
            logger.info("LC amendment rejected: amendmentId={}, lcNumber='{}'", amendmentId, amendment.getLc().getLcNumber());
            auditLogService.log(null, username, "LC_AMENDMENT_REJECTED",
                    "Rejected Amendment #" + amendment.getAmendmentNumber() + " for LC: " + amendment.getLc().getLcNumber(), null);
        }

        return saved;
    }

    // ─── Drawing Logic ────────────────────────────────────────────────────────

    @Transactional
    public LCDrawing presentDrawing(Long lcId, BigDecimal amount, String documents, String username) {
        logger.info("LC drawing presentation initiated by user='{}': lcId={}, amount={}", username, lcId, amount);
        LetterOfCredit lc = getLCById(lcId);

        LCDrawing drawing = new LCDrawing();
        drawing.setLc(lc);
        drawing.setDrawingRef("DRW-" + lc.getLcNumber() + "-" + System.currentTimeMillis() % 10000);
        drawing.setAmount(amount);
        drawing.setPresentationDate(LocalDate.now());
        drawing.setDocumentsPresented(documents);
        drawing.setStatus(LCDrawingStatus.PENDING_REVIEW);

        // Documentary compliance check
        boolean hasBillOfLading = documents.toUpperCase().contains("BILL OF LADING");
        boolean hasInvoice = documents.toUpperCase().contains("INVOICE");
        if (!hasBillOfLading || !hasInvoice) {
            drawing.setStatus(LCDrawingStatus.DISCREPANT);
            drawing.setDiscrepancyNotes("Missing required document: Bill of Lading or Commercial Invoice in the presentation.");
            logger.warn("Drawing discrepancy detected for LC='{}': hasBillOfLading={}, hasInvoice={}",
                    lc.getLcNumber(), hasBillOfLading, hasInvoice);
        } else {
            logger.debug("Drawing documentary check passed for LC='{}'", lc.getLcNumber());
        }

        LCDrawing saved = drawingRepository.save(drawing);
        logger.info("LC drawing submitted: drawingRef='{}', status='{}', lcNumber='{}'",
                saved.getDrawingRef(), saved.getStatus(), lc.getLcNumber());
        return saved;
    }

    @Transactional
    public LCDrawing processDrawing(Long drawingId, String status, String discrepancyNotes, String username) {
        logger.info("Processing LC drawing drawingId={} with status='{}' by user='{}'", drawingId, status, username);
        LCDrawing drawing = drawingRepository.findById(drawingId)
                .orElseThrow(() -> {
                    logger.warn("Drawing not found: drawingId={}", drawingId);
                    return new ResourceNotFoundException("Drawing not found");
                });

        LCDrawingStatus statusEnum = LCDrawingStatus.valueOf(status.toUpperCase());
        drawing.setStatus(statusEnum);
        if (discrepancyNotes != null) {
            drawing.setDiscrepancyNotes(discrepancyNotes);
        }

        if (statusEnum == LCDrawingStatus.PAID) {
            LetterOfCredit lc = drawing.getLc();
            CreditFacility facility = lc.getCreditFacility();

            BigDecimal releaseAmount = drawing.getAmount().min(facility.getUtilizedAmount());
            facility.setUtilizedAmount(facility.getUtilizedAmount().subtract(releaseAmount));
            facilityRepository.save(facility);
            logger.info("Facility limit released after LC drawing payment: drawingRef='{}', released={}, facilityId={}",
                    drawing.getDrawingRef(), releaseAmount, facility.getId());

            lc.setStatus(LetterOfCreditStatus.DRAWN);
            lcRepository.save(lc);
            logger.info("LC marked DRAWN: lcNumber='{}' after drawing payment drawingRef='{}'",
                    lc.getLcNumber(), drawing.getDrawingRef());

            auditLogService.log(null, username, "LC_DRAWING_PAID",
                    "Paid drawing " + drawing.getDrawingRef() + " of " + drawing.getAmount() + " USD. Released limit.", null);
        }

        LCDrawing result = drawingRepository.save(drawing);
        logger.info("LC drawing processed: drawingRef='{}', finalStatus='{}'", drawing.getDrawingRef(), status);
        return result;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public List<LCAmendment> getAmendments(Long lcId) {
        logger.debug("Fetching amendments for lcId={}", lcId);
        return amendmentRepository.findByLcIdOrderByAmendmentNumberDesc(lcId);
    }

    public List<LCDrawing> getDrawings(Long lcId) {
        logger.debug("Fetching drawings for lcId={}", lcId);
        return drawingRepository.findByLcIdOrderByPresentationDateDesc(lcId);
    }
}
