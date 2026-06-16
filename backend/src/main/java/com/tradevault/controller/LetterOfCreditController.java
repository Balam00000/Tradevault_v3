package com.tradevault.controller;

import com.tradevault.dto.ApiResponse;
import com.tradevault.entity.*;
import com.tradevault.service.LetterOfCreditService;
import com.tradevault.security.TradeSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tradevault.entity.enums.LetterOfCreditStatus;
import com.tradevault.repository.UserRepository;
import com.tradevault.repository.LCAmendmentRepository;
import com.tradevault.exception.BadRequestException;
import com.tradevault.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lcs")
@CrossOrigin(origins = "*")
public class LetterOfCreditController {

    private static final Logger logger = LoggerFactory.getLogger(LetterOfCreditController.class);

    @Autowired
    private LetterOfCreditService lcService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LCAmendmentRepository amendmentRepository;

    @Autowired
    private TradeSecurityService tradeSecurityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LetterOfCredit>>> getAllLCs(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        logger.debug("GetAllLCs requested by username='{}', role='{}'", user.getUsername(), user.getRole());
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null) {
                return ResponseEntity.ok(ApiResponse.success("Letters of Credit fetched successfully", Collections.emptyList()));
            }
            List<LetterOfCredit> lcs = lcService.getLCsByClientId(user.getCorporateClient().getId());
            logger.info("Returned {} LCs for CLIENT user='{}'", lcs.size(), user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Letters of Credit fetched successfully", lcs));
        } else if ("RELATIONSHIP_MANAGER".equals(user.getRole())) {
            List<LetterOfCredit> lcs = lcService.getLCsByRelationshipManagerId(user.getId());
            logger.info("Returned {} LCs for RM user='{}'", lcs.size(), user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Letters of Credit fetched successfully", lcs));
        }
        List<LetterOfCredit> lcs = lcService.getAllLCs();
        logger.info("Returned all {} LCs for staff user='{}'", lcs.size(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Letters of Credit fetched successfully", lcs));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ApiResponse<List<LetterOfCredit>>> getLCsByClientId(@PathVariable Long clientId, Principal principal) {
        logger.debug("GetLCsByClientId clientId={} requested by username='{}'", clientId, principal.getName());
        tradeSecurityService.checkClientAccess(clientId, principal);
        List<LetterOfCredit> lcs = lcService.getLCsByClientId(clientId);
        logger.info("Retrieved {} LCs for clientId={}", lcs.size(), clientId);
        return ResponseEntity.ok(ApiResponse.success("Letters of Credit for client fetched", lcs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LetterOfCredit>> getLCById(@PathVariable Long id, Principal principal) {
        logger.debug("GetLCById id={} requested by username='{}'", id, principal.getName());
        LetterOfCredit lc = lcService.getLCById(id);
        tradeSecurityService.checkLcAccess(lc, principal);
        return ResponseEntity.ok(ApiResponse.success("Letter of Credit details", lc));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LetterOfCredit>> createLC(
            @RequestBody LetterOfCredit lc,
            @RequestParam Long clientId,
            @RequestParam Long facilityId,
            Principal principal) {
        logger.info("LC create request: clientId={}, facilityId={}, by username='{}'", clientId, facilityId, principal.getName());
        tradeSecurityService.checkClientAccess(clientId, principal);
        LetterOfCredit created = lcService.createLC(lc, clientId, facilityId, principal.getName());
        logger.info("LC created: lcNumber='{}', by username='{}'", created.getLcNumber(), principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Letter of Credit draft created successfully", created));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CLIENT', 'OPERATIONS', 'RELATIONSHIP_MANAGER', 'COMPLIANCE', 'ADMIN')")
    public ResponseEntity<ApiResponse<LetterOfCredit>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        String status = payload.get("status");
        logger.info("LC status update request: lcId={}, targetStatus='{}', by username='{}'", id, status, principal.getName());
        LetterOfCredit lc = lcService.getLCById(id);
        tradeSecurityService.checkLcAccess(lc, principal);

        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if ("CLIENT".equals(user.getRole())) {
            if (!"IN_REVIEW".equals(status)) {
                logger.warn("CLIENT user='{}' attempted invalid status '{}' on lcId={}", user.getUsername(), status, id);
                throw new BadRequestException("Corporate clients are only allowed to submit Letters of Credit for review (IN_REVIEW status)");
            }
            if (lc.getStatus() != LetterOfCreditStatus.DRAFT) {
                logger.warn("CLIENT user='{}' attempted to submit non-DRAFT LC: lcId={}, currentStatus='{}'", user.getUsername(), id, lc.getStatus());
                throw new BadRequestException("Only DRAFT Letters of Credit can be submitted for review");
            }
        }

        LetterOfCreditStatus targetStatus = LetterOfCreditStatus.valueOf(status.toUpperCase());
        LetterOfCredit updated = lcService.updateStatus(id, targetStatus, principal.getName());
        logger.info("LC status updated to '{}': lcNumber='{}'", status, updated.getLcNumber());
        return ResponseEntity.ok(ApiResponse.success("Letter of Credit status updated to: " + status, updated));
    }

    // Amendments APIs
    @GetMapping("/{id}/amendments")
    public ResponseEntity<ApiResponse<List<LCAmendment>>> getAmendments(@PathVariable Long id, Principal principal) {
        logger.debug("GetAmendments for lcId={} requested by username='{}'", id, principal.getName());
        LetterOfCredit lc = lcService.getLCById(id);
        tradeSecurityService.checkLcAccess(lc, principal);
        List<LCAmendment> amendments = lcService.getAmendments(id);
        logger.info("Retrieved {} amendments for lcId={}", amendments.size(), id);
        return ResponseEntity.ok(ApiResponse.success("LC Amendments fetched", amendments));
    }

    @PostMapping("/{id}/amendments")
    public ResponseEntity<ApiResponse<LCAmendment>> requestAmendment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        LetterOfCredit lc = lcService.getLCById(id);
        tradeSecurityService.checkLcAccess(lc, principal);
        BigDecimal newAmount = new BigDecimal(payload.get("newAmount").toString());
        LocalDate newExpiry = LocalDate.parse(payload.get("newExpiryDate").toString());
        String justification = payload.get("justification").toString();
        logger.info("LC amendment requested: lcId={}, newAmount={}, newExpiry={}, by username='{}'",
                id, newAmount, newExpiry, principal.getName());
        LCAmendment requested = lcService.requestAmendment(id, newAmount, newExpiry, justification, principal.getName());
        logger.info("LC amendment created: amendmentId={}, lcNumber='{}'", requested.getId(), lc.getLcNumber());
        return ResponseEntity.ok(ApiResponse.success("LC Amendment requested successfully", requested));
    }

    @PutMapping("/amendments/{amendmentId}")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'RELATIONSHIP_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LCAmendment>> processAmendment(
            @PathVariable Long amendmentId,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        String status = payload.get("status");
        logger.info("Processing LC amendment: amendmentId={}, targetStatus='{}', by username='{}'", amendmentId, status, principal.getName());
        LCAmendment amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Amendment not found"));
        tradeSecurityService.checkLcAccess(amendment.getLc(), principal);
        LCAmendment processed = lcService.processAmendment(amendmentId, status, principal.getName());
        logger.info("LC amendment processed to status='{}': amendmentId={}", status, amendmentId);
        return ResponseEntity.ok(ApiResponse.success("Amendment processed successfully", processed));
    }

    // Drawings APIs
    @GetMapping("/{id}/drawings")
    public ResponseEntity<ApiResponse<List<LCDrawing>>> getDrawings(@PathVariable Long id, Principal principal) {
        logger.debug("GetDrawings for lcId={} requested by username='{}'", id, principal.getName());
        LetterOfCredit lc = lcService.getLCById(id);
        tradeSecurityService.checkLcAccess(lc, principal);
        List<LCDrawing> drawings = lcService.getDrawings(id);
        logger.info("Retrieved {} drawings for lcId={}", drawings.size(), id);
        return ResponseEntity.ok(ApiResponse.success("LC Drawings fetched", drawings));
    }

    @PostMapping("/{id}/drawings")
    public ResponseEntity<ApiResponse<LCDrawing>> presentDrawing(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        LetterOfCredit lc = lcService.getLCById(id);
        tradeSecurityService.checkLcAccess(lc, principal);
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        String documents = payload.get("documentsPresented").toString();
        logger.info("LC drawing presented: lcId={}, amount={}, by username='{}'", id, amount, principal.getName());
        LCDrawing drawing = lcService.presentDrawing(id, amount, documents, principal.getName());
        logger.info("LC drawing created: drawingRef='{}', status='{}', lcNumber='{}'",
                drawing.getDrawingRef(), drawing.getStatus(), lc.getLcNumber());
        return ResponseEntity.ok(ApiResponse.success("Documentary drawing presented successfully", drawing));
    }

    @PutMapping("/drawings/{drawingId}")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'ADMIN')")
    public ResponseEntity<ApiResponse<LCDrawing>> processDrawing(
            @PathVariable Long drawingId,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        String status = payload.get("status");
        String discrepancy = payload.get("discrepancyNotes");
        logger.info("Processing LC drawing: drawingId={}, targetStatus='{}', by username='{}'", drawingId, status, principal.getName());
        LCDrawing processed = lcService.processDrawing(drawingId, status, discrepancy, principal.getName());
        logger.info("LC drawing processed to status='{}': drawingRef='{}'", status, processed.getDrawingRef());
        return ResponseEntity.ok(ApiResponse.success("Drawing processed: " + status, processed));
    }
}
