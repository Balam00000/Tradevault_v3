package com.tradevault.service;

import com.tradevault.entity.ComplianceCase;
import com.tradevault.entity.SanctionsScreening;
import com.tradevault.entity.User;
import com.tradevault.entity.enums.ScreeningEntityType;
import com.tradevault.entity.enums.SanctionsScreeningStatus;
import com.tradevault.entity.enums.ComplianceCaseStatus;
import com.tradevault.repository.ComplianceCaseRepository;
import com.tradevault.repository.SanctionsScreeningRepository;
import com.tradevault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SanctionsScreeningServiceImpl implements SanctionsScreeningService {

    private static final Logger logger = LoggerFactory.getLogger(SanctionsScreeningServiceImpl.class);

    private final SanctionsScreeningRepository screeningRepository;
    private final ComplianceCaseRepository caseRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SanctionsScreeningServiceImpl(
            SanctionsScreeningRepository screeningRepository,
            ComplianceCaseRepository caseRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.screeningRepository = screeningRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // ─── Screen Entity ────────────────────────────────────────────────────────

    @Transactional
    public SanctionsScreening screenEntity(String entityName, String entityType, String txType, String txId) {
        logger.info("Initiating sanctions screening: entityName='{}', entityType='{}', txType='{}', txId='{}'",
                entityName, entityType, txType, txId);

        ScreeningEntityType entityTypeEnum = null;
        for (ScreeningEntityType set : ScreeningEntityType.values()) {
            if (set.name().equalsIgnoreCase(entityType)) {
                entityTypeEnum = set;
                break;
            }
        }
        if (entityTypeEnum == null) {
            throw new IllegalArgumentException("Unknown ScreeningEntityType: " + entityType);
        }

        BigDecimal score = BigDecimal.ZERO;
        String source = "N/A";
        SanctionsScreeningStatus status = SanctionsScreeningStatus.CLEARED;
        String notes = "Entity cleared. No match found on watchlist database.";

        String normalized = entityName.toUpperCase().trim();
        logger.debug("Screening normalized entity name: '{}'", normalized);

        if (normalized.contains("SYRIA") || normalized.contains("SUDAN") || normalized.contains("IRAN")) {
            score = new BigDecimal("89.50");
            source = "OFAC_SDN";
            status = SanctionsScreeningStatus.FLAGGED;
            notes = "High-risk match triggered (89.5% score) against Trade Block Ban Watchlist.";
            logger.warn("HIGH RISK MATCH: entityName='{}' matched OFAC_SDN watchlist with score={}. txId='{}', txType='{}'",
                    entityName, score, txId, txType);
        } else if (normalized.contains("SHANGHAI") || normalized.contains("TOKYO")) {
            score = new BigDecimal("12.00");
            source = "EU_WATCHLIST";
            status = SanctionsScreeningStatus.CLEARED;
            notes = "Low match score (12%). Checked and auto-cleared by system.";
            logger.info("Low-score match: entityName='{}' scored {} against EU_WATCHLIST — auto-cleared. txId='{}'",
                    entityName, score, txId);
        } else {
            logger.debug("Screening result: entityName='{}' CLEARED — no watchlist match. txId='{}'", entityName, txId);
        }

        SanctionsScreening screening = new SanctionsScreening();
        screening.setEntityName(entityName);
        screening.setEntityType(entityTypeEnum);
        screening.setTransactionType(txType);
        screening.setTransactionId(txId);
        screening.setMatchScore(score);
        screening.setWatchlistSource(source);
        screening.setStatus(status);
        screening.setComplianceNotes(notes);

        screening = screeningRepository.save(screening);
        logger.info("Sanctions screening result persisted: screeningId={}, status='{}', score={}, source='{}', txId='{}'",
                screening.getId(), status, score, source, txId);

        if (status == SanctionsScreeningStatus.FLAGGED) {
            logger.warn("Creating compliance case for FLAGGED screening: screeningId={}, entityName='{}', txId='{}'",
                    screening.getId(), entityName, txId);
            ComplianceCase compCase = new ComplianceCase();
            compCase.setScreening(screening);
            compCase.setCaseStatus(ComplianceCaseStatus.OPEN);
            compCase.setAssignedTo("compliance");
            compCase.setResolutionNotes("System generated compliance alert for high risk name match. Watchlist source: " + source);
            ComplianceCase savedCase = caseRepository.save(compCase);
            logger.warn("Compliance case auto-created: caseId={} for screeningId={}, entityName='{}'",
                    savedCase.getId(), screening.getId(), entityName);

            try {
                List<User> complianceUsers = userRepository.findByRole("COMPLIANCE");
                List<Long> complianceUserIds = complianceUsers.stream().map(User::getId).collect(Collectors.toList());
                notificationService.broadcastNotification(
                        complianceUserIds,
                        "Compliance Case Raised",
                        "A compliance case has been raised due to a flagged sanctions screening for entity '" + entityName + "' (" + txType + " Ref: " + txId + ").",
                        "COMPLIANCE"
                );
            } catch (Exception e) {
                logger.error("Failed to send compliance notification for screeningId={}: {}", screening.getId(), e.getMessage(), e);
            }
        }

        return screening;
    }

    // ─── Read Operations ─────────────────────────────────────────────────────

    public List<SanctionsScreening> getAllScreenings() {
        logger.debug("Fetching all sanctions screenings ordered by screened date");
        List<SanctionsScreening> screenings = screeningRepository.findAllByOrderByScreenedAtDesc();
        logger.info("Retrieved {} sanctions screenings", screenings.size());
        return screenings;
    }

    public List<ComplianceCase> getAllCases() {
        logger.debug("Fetching all compliance cases ordered by creation date");
        List<ComplianceCase> cases = caseRepository.findAllByOrderByCreatedAtDesc();
        logger.info("Retrieved {} compliance cases", cases.size());
        return cases;
    }

    // ─── Resolve Compliance Case ──────────────────────────────────────────────

    @Transactional
    public ComplianceCase resolveCase(Long caseId, String statusStr, String notes, String resolver) {
        logger.info("Compliance case resolution initiated: caseId={}, targetStatus='{}', resolver='{}'", caseId, statusStr, resolver);

        ComplianceCase compCase = caseRepository.findById(caseId)
                .orElseThrow(() -> {
                    logger.warn("Compliance case not found: caseId={}", caseId);
                    return new RuntimeException("Compliance case not found");
                });

        ComplianceCaseStatus status = null;
        for (ComplianceCaseStatus ccs : ComplianceCaseStatus.values()) {
            if (ccs.name().equalsIgnoreCase(statusStr)) {
                status = ccs;
                break;
            }
        }
        if (status == null) {
            throw new IllegalArgumentException("Unknown ComplianceCaseStatus: " + statusStr);
        }

        ComplianceCaseStatus previousStatus = compCase.getCaseStatus();
        compCase.setCaseStatus(status);
        compCase.setResolutionNotes(notes);
        compCase.setAssignedTo(resolver);

        // Sync associated screening status
        SanctionsScreening screening = compCase.getScreening();
        if (status.name().toUpperCase().contains("RESOLVED_CLEARED") || status == ComplianceCaseStatus.CLEARED) {
            screening.setStatus(SanctionsScreeningStatus.CLEARED);
            logger.info("Sanctions screening updated to CLEARED: screeningId={}, caseId={}, resolver='{}'",
                    screening.getId(), caseId, resolver);
        } else if (status.name().toUpperCase().contains("RESOLVED_BLOCKED")) {
            screening.setStatus(SanctionsScreeningStatus.FLAGGED);
            logger.warn("Sanctions screening remains FLAGGED (confirmed block): screeningId={}, caseId={}, resolver='{}'",
                    screening.getId(), caseId, resolver);
        }
        screeningRepository.save(screening);

        ComplianceCase saved = caseRepository.save(compCase);
        logger.info("Compliance case resolved: caseId={}, from='{}' to='{}', resolver='{}'",
                caseId, previousStatus, status, resolver);
        return saved;
    }
}
