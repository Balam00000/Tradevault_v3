package com.tradevault.service;

import com.tradevault.entity.*;
import com.tradevault.entity.enums.ExportBillStatus;
import com.tradevault.entity.enums.ExportBillType;
import com.tradevault.entity.enums.CollectionInstructionStatus;
import com.tradevault.entity.enums.CollectionInstructionType;
import com.tradevault.entity.enums.SanctionsScreeningStatus;
import com.tradevault.exception.ResourceNotFoundException;
import com.tradevault.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExportBillServiceImpl implements ExportBillService {

    private static final Logger logger = LoggerFactory.getLogger(ExportBillServiceImpl.class);

    @Autowired
    private ExportBillRepository billRepository;

    @Autowired
    private CollectionInstructionRepository instructionRepository;

    @Autowired
    private CorporateClientRepository clientRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SanctionsScreeningService sanctionsScreeningService;

    @Autowired
    private SanctionsScreeningRepository sanctionsScreeningRepository;

    // ─── Export Bill Read Operations ─────────────────────────────────────────

    public List<ExportBill> getAllBills() {
        logger.debug("Fetching all Export Bills ordered by creation date");
        List<ExportBill> bills = billRepository.findAllByOrderByCreatedAtDesc();
        logger.info("Retrieved {} Export Bills", bills.size());
        return bills;
    }

    public List<ExportBill> getBillsByClientId(Long clientId) {
        logger.debug("Fetching Export Bills for clientId={}", clientId);
        List<ExportBill> bills = billRepository.findByClientId(clientId);
        logger.info("Retrieved {} Export Bills for clientId={}", bills.size(), clientId);
        return bills;
    }

    public List<ExportBill> getBillsByRelationshipManagerId(Long rmId) {
        logger.debug("Fetching Export Bills for relationshipManagerId={}", rmId);
        List<ExportBill> bills = billRepository.findByClientRelationshipManagerId(rmId);
        logger.info("Retrieved {} Export Bills for relationshipManagerId={}", bills.size(), rmId);
        return bills;
    }

    public ExportBill getBillById(Long id) {
        logger.debug("Fetching Export Bill with id={}", id);
        return billRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Export Bill not found with id={}", id);
                    return new ResourceNotFoundException("Export Bill not found");
                });
    }

    // ─── Create Export Bill ───────────────────────────────────────────────────

    @Transactional
    public ExportBill createBill(ExportBill bill, Long clientId, String username) {
        logger.info("Export Bill creation initiated by user='{}' for clientId={}", username, clientId);

        CorporateClient client = clientRepository.findById(clientId)
                .orElseThrow(() -> {
                    logger.error("Client not found for bill creation: clientId={}", clientId);
                    return new ResourceNotFoundException("Client not found");
                });

        bill.setClient(client);
        bill.setStatus(ExportBillStatus.INITIATED);
        bill.setTrackingStatus("DOCUMENTS_PREPARED");

        if (bill.getBillNumber() == null || bill.getBillNumber().trim().isEmpty()) {
            bill.setBillNumber("EXP-BILL-" + String.format("%04d", (int)(Math.random() * 9000 + 1000)));
        }

        ExportBill saved = billRepository.save(bill);
        logger.info("Export Bill created: billNumber='{}', amount={} {}, drawee='{}', clientId={}, by user='{}'",
                saved.getBillNumber(), saved.getAmount(), saved.getCurrency(), saved.getDraweeName(), clientId, username);

        auditLogService.log(null, username, "EXPORT_BILL_CREATED",
                "Created export bill: " + saved.getBillNumber() + " for " + saved.getAmount(), null);

        // Sanctions screening for drawee
        logger.info("Triggering sanctions screening for export bill drawee='{}'", bill.getDraweeName());
        sanctionsScreeningService.screenEntity(bill.getDraweeName(), "DRAWER", "EXPORT_BILL", saved.getBillNumber());

        return saved;
    }

    // ─── Update Bill Status ───────────────────────────────────────────────────

    @Transactional
    public ExportBill updateBillStatus(Long id, ExportBillStatus status, String trackingStatus, String username) {
        logger.info("Export Bill status update requested by user='{}': billId={}, targetStatus='{}'", username, id, status);
        ExportBill bill = getBillById(id);

        // COMPLIANCE HOLD check
        boolean hasComplianceHold = !sanctionsScreeningRepository
                .findByTransactionIdAndStatus(bill.getBillNumber(), SanctionsScreeningStatus.FLAGGED).isEmpty();
        if (hasComplianceHold) {
            logger.warn("COMPLIANCE HOLD: Status update blocked for bill='{}', user='{}'. Unresolved FLAGGED screening exists.",
                    bill.getBillNumber(), username);
            auditLogService.log(null, username, "COMPLIANCE_HOLD_BLOCKED",
                    "Blocked status update on bill " + bill.getBillNumber() + " — open compliance hold (FLAGGED screening). Resolve via Compliance module first.", null);
            throw new IllegalStateException(
                    "COMPLIANCE_HOLD: Export Bill '" + bill.getBillNumber() + "' has an unresolved sanctions screening flag. " +
                    "A Compliance Manager must clear or block this entity before status can be advanced.");
        }

        ExportBillStatus oldStatus = bill.getStatus();
        bill.setStatus(status);
        if (trackingStatus != null) {
            bill.setTrackingStatus(trackingStatus);
            logger.debug("Export Bill tracking status updated to '{}' for billNumber='{}'", trackingStatus, bill.getBillNumber());
        }

        ExportBill saved = billRepository.save(bill);
        logger.info("Export Bill status updated: billNumber='{}', from='{}' to='{}', by user='{}'",
                bill.getBillNumber(), oldStatus, status, username);
        auditLogService.log(null, username, "EXPORT_BILL_STATUS_UPDATE",
                "Updated export bill " + bill.getBillNumber() + " status to: " + status, null);

        return saved;
    }

    // ─── Collection Instruction Read Operations ───────────────────────────────

    public List<CollectionInstruction> getAllInstructions() {
        logger.debug("Fetching all Collection Instructions ordered by creation date");
        List<CollectionInstruction> instructions = instructionRepository.findAllByOrderByCreatedAtDesc();
        logger.info("Retrieved {} Collection Instructions", instructions.size());
        return instructions;
    }

    public List<CollectionInstruction> getInstructionsByClientId(Long clientId) {
        logger.debug("Fetching Collection Instructions for clientId={}", clientId);
        List<CollectionInstruction> instructions = instructionRepository.findByClientId(clientId);
        logger.info("Retrieved {} Collection Instructions for clientId={}", instructions.size(), clientId);
        return instructions;
    }

    public List<CollectionInstruction> getInstructionsByRelationshipManagerId(Long rmId) {
        logger.debug("Fetching Collection Instructions for relationshipManagerId={}", rmId);
        List<CollectionInstruction> instructions = instructionRepository.findByClientRelationshipManagerId(rmId);
        logger.info("Retrieved {} Collection Instructions for relationshipManagerId={}", instructions.size(), rmId);
        return instructions;
    }

    // ─── Create Collection Instruction ────────────────────────────────────────

    @Transactional
    public CollectionInstruction createInstruction(CollectionInstruction instruction, Long clientId, String username) {
        logger.info("Collection Instruction creation initiated by user='{}' for clientId={}", username, clientId);

        CorporateClient client = clientRepository.findById(clientId)
                .orElseThrow(() -> {
                    logger.error("Client not found for collection instruction creation: clientId={}", clientId);
                    return new ResourceNotFoundException("Client not found");
                });

        instruction.setClient(client);
        instruction.setStatus(CollectionInstructionStatus.PENDING);

        if (instruction.getInstructionRef() == null || instruction.getInstructionRef().trim().isEmpty()) {
            instruction.setInstructionRef("COL-INST-" + String.format("%03d", (int)(Math.random() * 900 + 100)));
        }

        CollectionInstruction saved = instructionRepository.save(instruction);
        logger.info("Collection Instruction created: instructionRef='{}', amount={}, drawee='{}', clientId={}, by user='{}'",
                saved.getInstructionRef(), saved.getAmount(), instruction.getDraweeName(), clientId, username);

        auditLogService.log(null, username, "COLLECTION_INSTRUCTION_CREATED",
                "Created Collection Instruction: " + saved.getInstructionRef() + " for " + saved.getAmount(), null);

        // Sanctions screening for drawee
        logger.info("Triggering sanctions screening for collection drawee='{}'", instruction.getDraweeName());
        sanctionsScreeningService.screenEntity(instruction.getDraweeName(), "DRAWEE", "COLLECTION", saved.getInstructionRef());

        return saved;
    }

    // ─── Update Collection Instruction Status ─────────────────────────────────

    @Transactional
    public CollectionInstruction updateInstructionStatus(Long id, CollectionInstructionStatus status, String username) {
        logger.info("Collection Instruction status update requested by user='{}': instructionId={}, targetStatus='{}'", username, id, status);
        CollectionInstruction instruction = instructionRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Collection Instruction not found: id={}", id);
                    return new ResourceNotFoundException("Collection instruction not found");
                });

        // COMPLIANCE HOLD check
        boolean hasComplianceHold = !sanctionsScreeningRepository
                .findByTransactionIdAndStatus(instruction.getInstructionRef(), SanctionsScreeningStatus.FLAGGED).isEmpty();
        if (hasComplianceHold) {
            logger.warn("COMPLIANCE HOLD: Status update blocked for collection='{}', user='{}'. Unresolved FLAGGED screening exists.",
                    instruction.getInstructionRef(), username);
            auditLogService.log(null, username, "COMPLIANCE_HOLD_BLOCKED",
                    "Blocked status update on collection " + instruction.getInstructionRef() + " — open compliance hold (FLAGGED screening). Resolve via Compliance module first.", null);
            throw new IllegalStateException(
                    "COMPLIANCE_HOLD: Collection Instruction '" + instruction.getInstructionRef() + "' has an unresolved sanctions screening flag. " +
                    "A Compliance Manager must clear or block this entity before status can be advanced.");
        }

        CollectionInstructionStatus oldStatus = instruction.getStatus();
        instruction.setStatus(status);

        CollectionInstruction saved = instructionRepository.save(instruction);
        logger.info("Collection Instruction status updated: instructionRef='{}', from='{}' to='{}', by user='{}'",
                instruction.getInstructionRef(), oldStatus, status, username);
        auditLogService.log(null, username, "COLLECTION_INSTRUCTION_UPDATE",
                "Updated collection instruction " + instruction.getInstructionRef() + " status to: " + status, null);

        return saved;
    }
}
