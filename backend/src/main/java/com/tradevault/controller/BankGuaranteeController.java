package com.tradevault.controller;

import com.tradevault.dto.ApiResponse;
import com.tradevault.entity.BGClaim;
import com.tradevault.entity.BankGuarantee;
import com.tradevault.entity.User;
import com.tradevault.service.BankGuaranteeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bgs")
@CrossOrigin(origins = "*")
public class BankGuaranteeController {

    private static final Logger logger = LoggerFactory.getLogger(BankGuaranteeController.class);

    @Autowired
    private BankGuaranteeService bgService;

    @Autowired
    private com.tradevault.repository.UserRepository userRepository;

    @Autowired
    private com.tradevault.repository.CorporateClientRepository corporateClientRepository;

    private void checkBgAccess(BankGuarantee bg, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null || !bg.getClient().getId().equals(user.getCorporateClient().getId())) {
                logger.warn("BG access denied: username='{}' attempted to access bgId={}", user.getUsername(), bg.getId());
                throw new org.springframework.security.access.AccessDeniedException("You do not have permission to access this Bank Guarantee");
            }
        } else if ("RELATIONSHIP_MANAGER".equals(user.getRole())) {
            if (bg.getClient().getRelationshipManagerId() == null || !bg.getClient().getRelationshipManagerId().equals(user.getId())) {
                logger.warn("BG access denied: RM username='{}' attempted to access non-assigned bgId={}", user.getUsername(), bg.getId());
                throw new org.springframework.security.access.AccessDeniedException("You do not have permission to access this Bank Guarantee (not assigned to you)");
            }
        }
    }

    private void checkClientAccess(Long clientId, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null || !user.getCorporateClient().getId().equals(clientId)) {
                logger.warn("Client access denied: username='{}' attempted to access clientId={}", user.getUsername(), clientId);
                throw new org.springframework.security.access.AccessDeniedException("You do not have permission to access this client's data");
            }
        } else if ("RELATIONSHIP_MANAGER".equals(user.getRole())) {
            com.tradevault.entity.CorporateClient client = corporateClientRepository.findById(clientId).orElseThrow();
            if (client.getRelationshipManagerId() == null || !client.getRelationshipManagerId().equals(user.getId())) {
                logger.warn("Client access denied: RM username='{}' attempted to access non-assigned clientId={}", user.getUsername(), clientId);
                throw new org.springframework.security.access.AccessDeniedException("You do not have permission to access this client's data (not assigned to you)");
            }
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BankGuarantee>>> getAllBGs(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        logger.debug("GetAllBGs requested by username='{}', role='{}'", user.getUsername(), user.getRole());
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null) {
                return ResponseEntity.ok(ApiResponse.success("Bank Guarantees fetched successfully", java.util.Collections.emptyList()));
            }
            List<BankGuarantee> bgs = bgService.getBGsByClientId(user.getCorporateClient().getId());
            logger.info("Returned {} BGs for CLIENT user='{}'", bgs.size(), user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Bank Guarantees fetched successfully", bgs));
        } else if ("RELATIONSHIP_MANAGER".equals(user.getRole())) {
            List<BankGuarantee> bgs = bgService.getBGsByRelationshipManagerId(user.getId());
            logger.info("Returned {} BGs for RM user='{}'", bgs.size(), user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Bank Guarantees fetched successfully", bgs));
        }
        List<BankGuarantee> bgs = bgService.getAllBGs();
        logger.info("Returned all {} BGs for staff user='{}'", bgs.size(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Bank Guarantees fetched successfully", bgs));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ApiResponse<List<BankGuarantee>>> getBGsByClientId(@PathVariable Long clientId, Principal principal) {
        logger.debug("GetBGsByClientId clientId={} requested by username='{}'", clientId, principal.getName());
        checkClientAccess(clientId, principal);
        List<BankGuarantee> bgs = bgService.getBGsByClientId(clientId);
        logger.info("Retrieved {} BGs for clientId={}", bgs.size(), clientId);
        return ResponseEntity.ok(ApiResponse.success("Bank Guarantees for client fetched", bgs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BankGuarantee>> getBGById(@PathVariable Long id, Principal principal) {
        logger.debug("GetBGById id={} requested by username='{}'", id, principal.getName());
        BankGuarantee bg = bgService.getBGById(id);
        checkBgAccess(bg, principal);
        return ResponseEntity.ok(ApiResponse.success("Bank Guarantee details", bg));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankGuarantee>> createBG(
            @RequestBody BankGuarantee bg,
            @RequestParam Long clientId,
            @RequestParam Long facilityId,
            Principal principal) {
        logger.info("BG create request: clientId={}, facilityId={}, by username='{}'", clientId, facilityId, principal.getName());
        checkClientAccess(clientId, principal);
        BankGuarantee created = bgService.createBG(bg, clientId, facilityId, principal.getName());
        logger.info("BG created: bgNumber='{}', by username='{}'", created.getBgNumber(), principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Bank Guarantee draft created successfully", created));
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<BankGuarantee>> submitForApproval(
            @PathVariable Long id, Principal principal) {
        logger.info("BG submit for approval: bgId={}, by username='{}'", id, principal.getName());
        BankGuarantee bg = bgService.getBGById(id);
        checkBgAccess(bg, principal);
        BankGuarantee submitted = bgService.submitForApproval(id, principal.getName());
        logger.info("BG submitted for approval: bgNumber='{}', by username='{}'", submitted.getBgNumber(), principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Bank Guarantee submitted for Operations approval", submitted));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'RELATIONSHIP_MANAGER', 'TREASURY', 'ADMIN')")
    public ResponseEntity<ApiResponse<BankGuarantee>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        String status = payload.get("status");
        logger.info("BG status update: bgId={}, targetStatus='{}', by username='{}'", id, status, principal.getName());
        BankGuarantee updated = bgService.updateStatus(id, status, principal.getName());
        logger.info("BG status updated to '{}': bgNumber='{}'", status, updated.getBgNumber());
        return ResponseEntity.ok(ApiResponse.success("Bank Guarantee status updated to: " + status, updated));
    }

    @GetMapping("/{id}/claims")
    public ResponseEntity<ApiResponse<List<BGClaim>>> getClaims(@PathVariable Long id, Principal principal) {
        logger.debug("GetClaims for bgId={} requested by username='{}'", id, principal.getName());
        BankGuarantee bg = bgService.getBGById(id);
        checkBgAccess(bg, principal);
        List<BGClaim> claims = bgService.getClaims(id);
        logger.info("Retrieved {} claims for bgId={}", claims.size(), id);
        return ResponseEntity.ok(ApiResponse.success("BG Claims fetched", claims));
    }

    @PostMapping("/{id}/claims")
    public ResponseEntity<ApiResponse<BGClaim>> fileClaim(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        BankGuarantee bg = bgService.getBGById(id);
        checkBgAccess(bg, principal);
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        String details = payload.get("paymentDetails").toString();
        logger.info("Filing BG claim: bgId={}, claimAmount={}, by username='{}'", id, amount, principal.getName());
        BGClaim claim = bgService.fileClaim(id, amount, details, principal.getName());
        logger.info("BG claim filed: claimRef='{}', bgNumber='{}'", claim.getClaimRef(), bg.getBgNumber());
        return ResponseEntity.ok(ApiResponse.success("Bank Guarantee Claim filed successfully", claim));
    }

    @PutMapping("/claims/{claimId}")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'TREASURY', 'ADMIN')")
    public ResponseEntity<ApiResponse<BGClaim>> processClaim(
            @PathVariable Long claimId,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        String status = payload.get("status");
        logger.info("Processing BG claim: claimId={}, targetStatus='{}', by username='{}'", claimId, status, principal.getName());
        BGClaim processed = bgService.processClaim(claimId, status, principal.getName());
        logger.info("BG claim processed to status='{}': claimRef='{}'", status, processed.getClaimRef());
        return ResponseEntity.ok(ApiResponse.success("BG Claim processed successfully", processed));
    }
}
