package com.tradevault.controller;

import com.tradevault.dto.ApiResponse;
import com.tradevault.entity.CollectionInstruction;
import com.tradevault.entity.ExportBill;
import com.tradevault.entity.User;
import com.tradevault.service.ExportBillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bills")
@CrossOrigin(origins = "*")
public class ExportBillController {

    private static final Logger logger = LoggerFactory.getLogger(ExportBillController.class);

    @Autowired
    private ExportBillService billService;

    @Autowired
    private com.tradevault.repository.UserRepository userRepository;

    private void checkClientAccess(Long clientId, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null || !user.getCorporateClient().getId().equals(clientId)) {
                logger.warn("Client access denied: username='{}' attempted to access clientId={}", user.getUsername(), clientId);
                throw new org.springframework.security.access.AccessDeniedException("You do not have permission to access this client's data");
            }
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExportBill>>> getAllBills(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        logger.debug("GetAllBills requested by username='{}', role='{}'", user.getUsername(), user.getRole());
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null) {
                return ResponseEntity.ok(ApiResponse.success("Export Bills fetched successfully", java.util.Collections.emptyList()));
            }
            List<ExportBill> bills = billService.getBillsByClientId(user.getCorporateClient().getId());
            logger.info("Returned {} Export Bills for CLIENT user='{}'", bills.size(), user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Export Bills fetched successfully", bills));
        }
        List<ExportBill> bills = billService.getAllBills();
        logger.info("Returned all {} Export Bills for staff user='{}'", bills.size(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Export Bills fetched successfully", bills));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ApiResponse<List<ExportBill>>> getBillsByClientId(@PathVariable Long clientId, Principal principal) {
        logger.debug("GetBillsByClientId clientId={} requested by username='{}'", clientId, principal.getName());
        checkClientAccess(clientId, principal);
        List<ExportBill> bills = billService.getBillsByClientId(clientId);
        logger.info("Retrieved {} Export Bills for clientId={}", bills.size(), clientId);
        return ResponseEntity.ok(ApiResponse.success("Export Bills fetched", bills));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExportBill>> createBill(
            @RequestBody ExportBill bill,
            @RequestParam Long clientId,
            Principal principal) {
        logger.info("Export Bill create request: clientId={}, by username='{}'", clientId, principal.getName());
        checkClientAccess(clientId, principal);
        ExportBill created = billService.createBill(bill, clientId, principal.getName());
        logger.info("Export Bill created: billNumber='{}', by username='{}'", created.getBillNumber(), principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Export Bill initiated successfully", created));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'ADMIN')")
    public ResponseEntity<ApiResponse<ExportBill>> updateBillStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        String status = payload.get("status");
        String trackingStatus = payload.get("trackingStatus");
        logger.info("Export Bill status update: billId={}, targetStatus='{}', by username='{}'", id, status, principal.getName());
        ExportBill updated = billService.updateBillStatus(id, status, trackingStatus, principal.getName());
        logger.info("Export Bill status updated to '{}': billNumber='{}'", status, updated.getBillNumber());
        return ResponseEntity.ok(ApiResponse.success("Export Bill status updated", updated));
    }

    // Collection instructions
    @GetMapping("/collections")
    public ResponseEntity<ApiResponse<List<CollectionInstruction>>> getAllCollections(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        logger.debug("GetAllCollections requested by username='{}', role='{}'", user.getUsername(), user.getRole());
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null) {
                return ResponseEntity.ok(ApiResponse.success("Collection Instructions fetched", java.util.Collections.emptyList()));
            }
            List<CollectionInstruction> instructions = billService.getInstructionsByClientId(user.getCorporateClient().getId());
            logger.info("Returned {} Collection Instructions for CLIENT user='{}'", instructions.size(), user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Collection Instructions fetched", instructions));
        }
        List<CollectionInstruction> all = billService.getAllInstructions();
        logger.info("Returned all {} Collection Instructions for staff user='{}'", all.size(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Collection Instructions fetched", all));
    }

    @GetMapping("/collections/client/{clientId}")
    public ResponseEntity<ApiResponse<List<CollectionInstruction>>> getCollectionsByClientId(@PathVariable Long clientId, Principal principal) {
        logger.debug("GetCollectionsByClientId clientId={} requested by username='{}'", clientId, principal.getName());
        checkClientAccess(clientId, principal);
        List<CollectionInstruction> instructions = billService.getInstructionsByClientId(clientId);
        logger.info("Retrieved {} Collection Instructions for clientId={}", instructions.size(), clientId);
        return ResponseEntity.ok(ApiResponse.success("Collection Instructions fetched", instructions));
    }

    @PostMapping("/collections")
    public ResponseEntity<ApiResponse<CollectionInstruction>> createCollection(
            @RequestBody CollectionInstruction instruction,
            @RequestParam Long clientId,
            Principal principal) {
        logger.info("Collection Instruction create request: clientId={}, by username='{}'", clientId, principal.getName());
        checkClientAccess(clientId, principal);
        CollectionInstruction created = billService.createInstruction(instruction, clientId, principal.getName());
        logger.info("Collection Instruction created: instructionRef='{}', by username='{}'", created.getInstructionRef(), principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Collection Instruction registered", created));
    }

    @PutMapping("/collections/{id}")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'ADMIN')")
    public ResponseEntity<ApiResponse<CollectionInstruction>> updateCollectionStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        String status = payload.get("status");
        logger.info("Collection Instruction status update: id={}, targetStatus='{}', by username='{}'", id, status, principal.getName());
        CollectionInstruction updated = billService.updateInstructionStatus(id, status, principal.getName());
        logger.info("Collection Instruction status updated to '{}': instructionRef='{}'", status, updated.getInstructionRef());
        return ResponseEntity.ok(ApiResponse.success("Collection Instruction status updated", updated));
    }
}
