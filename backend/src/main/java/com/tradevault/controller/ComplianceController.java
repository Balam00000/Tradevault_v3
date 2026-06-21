package com.tradevault.controller;

import com.tradevault.dto.ApiResponse;
import com.tradevault.entity.ComplianceCase;
import com.tradevault.entity.SanctionsScreening;
import com.tradevault.service.SanctionsScreeningService;
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
@RequestMapping("/compliance")
@PreAuthorize("hasAnyRole('COMPLIANCE', 'ADMIN')")
public class ComplianceController {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceController.class);

    private final SanctionsScreeningService sanctionsScreeningService;

    public ComplianceController(SanctionsScreeningService sanctionsScreeningService) {
        this.sanctionsScreeningService = sanctionsScreeningService;
    }

    @GetMapping("/screenings")
    public ResponseEntity<ApiResponse<List<SanctionsScreening>>> getAllScreenings(Principal principal) {
        logger.debug("GetAllScreenings requested by username='{}'", principal != null ? principal.getName() : "unknown");
        List<SanctionsScreening> screenings = sanctionsScreeningService.getAllScreenings();
        logger.info("Retrieved {} sanctions screenings", screenings.size());
        return ResponseEntity.ok(ApiResponse.success("Sanctions Screenings retrieved", screenings));
    }

    @GetMapping("/cases")
    public ResponseEntity<ApiResponse<List<ComplianceCase>>> getAllCases(Principal principal) {
        logger.debug("GetAllCases requested by username='{}'", principal != null ? principal.getName() : "unknown");
        List<ComplianceCase> cases = sanctionsScreeningService.getAllCases();
        logger.info("Retrieved {} compliance cases", cases.size());
        return ResponseEntity.ok(ApiResponse.success("Compliance Cases retrieved", cases));
    }

    @PutMapping("/cases/{caseId}/resolve")
    public ResponseEntity<ApiResponse<ComplianceCase>> resolveCase(
            @PathVariable Long caseId,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        String status = payload.get("status");
        String notes = payload.get("notes");
        logger.info("Compliance case resolution: caseId={}, targetStatus='{}', resolver='{}'", caseId, status, principal.getName());
        ComplianceCase resolved = sanctionsScreeningService.resolveCase(caseId, status, notes, principal.getName());
        logger.info("Compliance case resolved: caseId={}, status='{}', resolver='{}'", caseId, status, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Compliance case resolved: " + status, resolved));
    }
}
