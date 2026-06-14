package com.tradevault.controller;

import com.tradevault.dto.ApiResponse;
import com.tradevault.entity.CorporateClient;
import com.tradevault.entity.CreditFacility;
import com.tradevault.entity.User;
import com.tradevault.repository.CorporateClientRepository;
import com.tradevault.repository.CreditFacilityRepository;
import com.tradevault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/corporates")
@CrossOrigin(origins = "*")
public class CorporateController {

    private static final Logger logger = LoggerFactory.getLogger(CorporateController.class);

    @Autowired
    private CorporateClientRepository clientRepository;

    @Autowired
    private CreditFacilityRepository facilityRepository;

    @Autowired
    private UserRepository userRepository;

    // ─── Auth helpers ─────────────────────────────────────────────────────────

    private User resolveUser(Principal principal) {
        return userRepository.findByUsername(principal.getName()).orElseThrow();
    }

    private void requireAdmin(Principal principal) {
        User u = resolveUser(principal);
        if (!"ADMIN".equals(u.getRole())) {
            logger.warn("Unauthorized admin action attempted by username='{}', role='{}'", u.getUsername(), u.getRole());
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only administrators can perform this action");
        }
    }

    private void checkClientAccess(Long clientId, Principal principal) {
        User user = resolveUser(principal);
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null || !user.getCorporateClient().getId().equals(clientId)) {
                logger.warn("Client access denied: username='{}' attempted to access clientId={}", user.getUsername(), clientId);
                throw new org.springframework.security.access.AccessDeniedException(
                        "You do not have permission to access this client's data");
            }
        } else if ("RELATIONSHIP_MANAGER".equals(user.getRole())) {
            CorporateClient client = clientRepository.findById(clientId).orElseThrow();
            if (client.getRelationshipManagerId() == null || !client.getRelationshipManagerId().equals(user.getId())) {
                logger.warn("Client access denied: RM username='{}' attempted to access non-assigned clientId={}", user.getUsername(), clientId);
                throw new org.springframework.security.access.AccessDeniedException(
                        "You do not have permission to access this client's data (not assigned to you)");
            }
        }
    }

    // ─── Corporate Client CRUD ────────────────────────────────────────────────

    /** GET /corporates — list all clients (admin) or own client (client role) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CorporateClient>>> getAllClients(Principal principal) {
        User user = resolveUser(principal);
        logger.debug("GetAllClients requested by username='{}', role='{}'", user.getUsername(), user.getRole());
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null) {
                logger.debug("CLIENT user='{}' has no corporate client linked", user.getUsername());
                return ResponseEntity.ok(ApiResponse.success("Corporate clients fetched", Collections.emptyList()));
            }
            logger.debug("Returning own corporate client for user='{}'", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Corporate clients fetched", List.of(user.getCorporateClient())));
        } else if ("RELATIONSHIP_MANAGER".equals(user.getRole())) {
            List<CorporateClient> rms = clientRepository.findByRelationshipManagerId(user.getId());
            logger.info("Relationship Manager user='{}' retrieved {} corporate clients", user.getUsername(), rms.size());
            return ResponseEntity.ok(ApiResponse.success("Corporate clients fetched", rms));
        }
        List<CorporateClient> all = clientRepository.findAll();
        logger.info("Admin/staff user='{}' retrieved all {} corporate clients", user.getUsername(), all.size());
        return ResponseEntity.ok(ApiResponse.success("Corporate clients fetched", all));
    }

    /** GET /corporates/clients — legacy alias */
    @GetMapping("/clients")
    public ResponseEntity<ApiResponse<List<CorporateClient>>> getAllClientsLegacy(Principal principal) {
        return getAllClients(principal);
    }

    /** GET /corporates/{id} — single client detail (admin or own) */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CorporateClient>> getClientById(
            @PathVariable Long id, Principal principal) {
        logger.debug("GetClientById id={} requested by username='{}'", id, principal.getName());
        checkClientAccess(id, principal);
        CorporateClient client = clientRepository.findById(id).orElseThrow();
        logger.debug("Corporate client found: clientId={}, company='{}'", id, client.getCompanyName());
        return ResponseEntity.ok(ApiResponse.success("Corporate client details", client));
    }

    /** POST /corporates — create corporate client (ADMIN only) */
    @PostMapping
    public ResponseEntity<ApiResponse<CorporateClient>> createClient(
            @RequestBody CorporateClient client, Principal principal) {
        requireAdmin(principal);
        logger.info("Creating corporate client: companyName='{}', by admin='{}'", client.getCompanyName(), principal.getName());
        CorporateClient saved = clientRepository.save(client);
        logger.info("Corporate client created successfully: clientId={}, companyName='{}'", saved.getId(), saved.getCompanyName());
        return ResponseEntity.ok(ApiResponse.success("Corporate client created", saved));
    }

    /** POST /corporates/clients — legacy alias for create */
    @PostMapping("/clients")
    public ResponseEntity<ApiResponse<CorporateClient>> createClientLegacy(
            @RequestBody CorporateClient client, Principal principal) {
        return createClient(client, principal);
    }

    /** PUT /corporates/{id} — update corporate client fields (ADMIN only) */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CorporateClient>> updateClient(
            @PathVariable Long id, @RequestBody CorporateClient patch, Principal principal) {
        requireAdmin(principal);
        logger.info("Updating corporate client id={} by admin='{}'", id, principal.getName());
        CorporateClient existing = clientRepository.findById(id).orElseThrow();
        if (patch.getCompanyName() != null) existing.setCompanyName(patch.getCompanyName());
        if (patch.getCountry() != null) existing.setCountry(patch.getCountry());
        if (patch.getTaxId() != null) existing.setTaxId(patch.getTaxId());
        if (patch.getCreditLimit() != null) existing.setCreditLimit(patch.getCreditLimit());
        if (patch.getStatus() != null) {
            logger.info("Corporate client status change: clientId={}, from='{}' to='{}'", id, existing.getStatus(), patch.getStatus());
            existing.setStatus(patch.getStatus());
        }
        existing.setRelationshipManagerId(patch.getRelationshipManagerId());
        CorporateClient updated = clientRepository.save(existing);
        logger.info("Corporate client updated: clientId={}, company='{}'", id, updated.getCompanyName());
        return ResponseEntity.ok(ApiResponse.success("Corporate client updated", updated));
    }

    // ─── Credit Facility CRUD ─────────────────────────────────────────────────

    /** GET /corporates/facilities — list facilities (admin: all; client: own) */
    @GetMapping("/facilities")
    public ResponseEntity<ApiResponse<List<CreditFacility>>> getAllFacilities(Principal principal) {
        User user = resolveUser(principal);
        logger.debug("GetAllFacilities requested by username='{}', role='{}'", user.getUsername(), user.getRole());
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null) {
                return ResponseEntity.ok(ApiResponse.success("Credit facilities fetched", Collections.emptyList()));
            }
            List<CreditFacility> clientFacilities = facilityRepository.findByClientId(user.getCorporateClient().getId());
            logger.debug("Returning {} facilities for client user='{}'", clientFacilities.size(), user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Credit facilities fetched", clientFacilities));
        } else if ("RELATIONSHIP_MANAGER".equals(user.getRole())) {
            List<CreditFacility> rmFacilities = facilityRepository.findByClientRelationshipManagerId(user.getId());
            logger.info("Relationship Manager user='{}' retrieved {} credit facilities", user.getUsername(), rmFacilities.size());
            return ResponseEntity.ok(ApiResponse.success("Credit facilities fetched", rmFacilities));
        }
        List<CreditFacility> all = facilityRepository.findAll();
        logger.info("Admin/staff user='{}' retrieved all {} credit facilities", user.getUsername(), all.size());
        return ResponseEntity.ok(ApiResponse.success("Credit facilities fetched", all));
    }

    /** GET /corporates/{clientId}/facilities — list facilities for one client */
    @GetMapping("/{clientId}/facilities")
    public ResponseEntity<ApiResponse<List<CreditFacility>>> getFacilitiesByClient(
            @PathVariable Long clientId, Principal principal) {
        logger.debug("GetFacilitiesByClient clientId={} requested by username='{}'", clientId, principal.getName());
        checkClientAccess(clientId, principal);
        List<CreditFacility> facilities = facilityRepository.findByClientId(clientId);
        logger.info("Retrieved {} facilities for clientId={}", facilities.size(), clientId);
        return ResponseEntity.ok(ApiResponse.success("Credit facilities fetched for client", facilities));
    }

    /** GET /corporates/facilities/client/{clientId} — legacy alias */
    @GetMapping("/facilities/client/{clientId}")
    public ResponseEntity<ApiResponse<List<CreditFacility>>> getFacilitiesByClientLegacy(
            @PathVariable Long clientId, Principal principal) {
        return getFacilitiesByClient(clientId, principal);
    }

    /** POST /corporates/facilities?clientId= — create facility (ADMIN only) */
    @PostMapping("/facilities")
    public ResponseEntity<ApiResponse<CreditFacility>> createFacility(
            @RequestBody CreditFacility facility,
            @RequestParam Long clientId,
            Principal principal) {
        requireAdmin(principal);
        logger.info("Creating credit facility for clientId={}, type='{}', limit={}, by admin='{}'",
                clientId, facility.getFacilityType(), facility.getLimitAmount(), principal.getName());
        CorporateClient client = clientRepository.findById(clientId).orElseThrow();
        facility.setClient(client);
        if (facility.getUtilizedAmount() == null) facility.setUtilizedAmount(BigDecimal.ZERO);
        CreditFacility saved = facilityRepository.save(facility);
        logger.info("Credit facility created: facilityId={}, type='{}', limit={}, clientId={}",
                saved.getId(), saved.getFacilityType(), saved.getLimitAmount(), clientId);
        return ResponseEntity.ok(ApiResponse.success("Credit facility created", saved));
    }

    /** PUT /corporates/facilities/{id} — update facility fields (ADMIN only) */
    @PutMapping("/facilities/{id}")
    public ResponseEntity<ApiResponse<CreditFacility>> updateFacility(
            @PathVariable Long id, @RequestBody CreditFacility patch, Principal principal) {
        requireAdmin(principal);
        logger.info("Updating credit facility id={} by admin='{}'", id, principal.getName());
        CreditFacility existing = facilityRepository.findById(id).orElseThrow();
        if (patch.getFacilityType() != null) existing.setFacilityType(patch.getFacilityType());
        if (patch.getLimitAmount() != null) {
            logger.info("Facility limit change: facilityId={}, from={} to={}", id, existing.getLimitAmount(), patch.getLimitAmount());
            existing.setLimitAmount(patch.getLimitAmount());
        }
        if (patch.getCurrency() != null) existing.setCurrency(patch.getCurrency());
        if (patch.getExpiryDate() != null) existing.setExpiryDate(patch.getExpiryDate());
        if (patch.getStatus() != null) {
            logger.info("Facility status change: facilityId={}, from='{}' to='{}'", id, existing.getStatus(), patch.getStatus());
            existing.setStatus(patch.getStatus());
        }
        CreditFacility updated = facilityRepository.save(existing);
        logger.info("Credit facility updated: facilityId={}, type='{}'", id, updated.getFacilityType());
        return ResponseEntity.ok(ApiResponse.success("Credit facility updated", updated));
    }
}
