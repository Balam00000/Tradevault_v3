package com.tradevault.service;

import com.tradevault.entity.*;
import com.tradevault.entity.enums.*;
import com.tradevault.exception.BadRequestException;
import com.tradevault.exception.ResourceNotFoundException;
import com.tradevault.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BankGuaranteeService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BankGuaranteeService Unit Tests")
class BankGuaranteeServiceTest {

    @Mock private BankGuaranteeRepository bgRepository;
    @Mock private CreditFacilityRepository facilityRepository;
    @Mock private CorporateClientRepository clientRepository;
    @Mock private BGClaimRepository claimRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private SanctionsScreeningService sanctionsScreeningService;
    @Mock private SanctionsScreeningRepository sanctionsScreeningRepository;

    @InjectMocks
    private BankGuaranteeService bgService;

    // ─── Fixtures ─────────────────────────────────────────────────────────────

    private CorporateClient buildClient(Long id) {
        CorporateClient c = new CorporateClient();
        c.setId(id);
        c.setCompanyName("Test Corp");
        return c;
    }

    private CreditFacility buildFacility(Long id, Long clientId, BigDecimal limit, BigDecimal utilized) {
        CreditFacility f = new CreditFacility();
        f.setId(id);
        f.setLimitAmount(limit);
        f.setUtilizedAmount(utilized);
        f.setStatus(CreditFacilityStatus.ACTIVE);
        f.setClient(buildClient(clientId));
        return f;
    }

    private BankGuarantee buildBG(Long id, String status, BigDecimal amount, CorporateClient client, CreditFacility facility) {
        BankGuarantee bg = new BankGuarantee();
        bg.setId(id);
        bg.setBgNumber("BG-2026-0001");
        bg.setStatus(status != null ? BankGuaranteeStatus.valueOf(status.toUpperCase()) : null);
        bg.setAmount(amount);
        bg.setBeneficiaryName("Test Beneficiary");
        bg.setBgType(BGType.PERFORMANCE_BOND);
        bg.setExpiryDate(LocalDate.now().plusMonths(12));
        bg.setCurrency("USD");
        bg.setClient(client);
        bg.setCreditFacility(facility);
        return bg;
    }

    // ─── getAllBGs ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllBGs: should return all BGs ordered by creation date")
    void getAllBGs_returnsAll() {
        when(bgRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(new BankGuarantee(), new BankGuarantee()));
        assertThat(bgService.getAllBGs()).hasSize(2);
    }

    // ─── getBGById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBGById: should throw ResourceNotFoundException when BG not found")
    void getBGById_notFound_throwsException() {
        when(bgRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bgService.getBGById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── createBG ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBG: should create BG successfully with sufficient limit")
    void createBG_success() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("500000"), BigDecimal.ZERO);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(facilityRepository.findById(10L)).thenReturn(Optional.of(facility));
        when(sanctionsScreeningService.screenEntity(any(), any(), any(), any())).thenReturn(new SanctionsScreening());
        when(bgRepository.save(any())).thenAnswer(inv -> {
            BankGuarantee saved = inv.getArgument(0);
            saved.setId(100L);
            if (saved.getBgNumber() == null) saved.setBgNumber("BG-2026-1000");
            return saved;
        });

        BankGuarantee bg = new BankGuarantee();
        bg.setAmount(new BigDecimal("200000"));
        bg.setBeneficiaryName("Safe Beneficiary");
        bg.setBgType(BGType.PERFORMANCE_BOND);
        bg.setExpiryDate(LocalDate.now().plusMonths(12));

        BankGuarantee result = bgService.createBG(bg, 1L, 10L, "testuser");

        assertThat(result.getStatus()).isEqualTo(BankGuaranteeStatus.DRAFT);
        assertThat(result.getBgNumber()).isNotBlank();
        verify(auditLogService).log(isNull(), eq("testuser"), eq("BG_CREATION_DRAFT"), any(), isNull());
        verify(sanctionsScreeningService).screenEntity(eq("Safe Beneficiary"), eq("BENEFICIARY"), eq("BG"), any());
    }

    @Test
    @DisplayName("createBG: should throw BadRequestException when facility limit is insufficient")
    void createBG_insufficientLimit_throwsBadRequest() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("100000"), new BigDecimal("95000"));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(facilityRepository.findById(10L)).thenReturn(Optional.of(facility));

        BankGuarantee bg = new BankGuarantee();
        bg.setAmount(new BigDecimal("50000")); // Available = 5000, requested = 50000

        assertThatThrownBy(() -> bgService.createBG(bg, 1L, 10L, "testuser"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient credit facility limit");
    }

    // ─── updateStatus ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus: ACTIVE should block facility utilized amount")
    void updateStatus_toActive_blocksFacility() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("500000"), BigDecimal.ZERO);
        BankGuarantee bg = buildBG(1L, "PENDING_APPROVAL", new BigDecimal("200000"), client, facility);

        when(bgRepository.findById(1L)).thenReturn(Optional.of(bg));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus(any(), any())).thenReturn(List.of());
        when(bgRepository.save(any())).thenReturn(bg);
        when(facilityRepository.save(any())).thenReturn(facility);

        bgService.updateStatus(1L, "ACTIVE", "ops-user");

        assertThat(bg.getStatus()).isEqualTo(BankGuaranteeStatus.ACTIVE);
        assertThat(facility.getUtilizedAmount()).isEqualByComparingTo("200000");
    }

    @Test
    @DisplayName("updateStatus: RELEASED should refund facility utilized amount")
    void updateStatus_released_refundsFacility() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("500000"), new BigDecimal("200000"));
        BankGuarantee bg = buildBG(1L, "ACTIVE", new BigDecimal("200000"), client, facility);

        when(bgRepository.findById(1L)).thenReturn(Optional.of(bg));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus(any(), any())).thenReturn(List.of());
        when(bgRepository.save(any())).thenReturn(bg);
        when(facilityRepository.save(any())).thenReturn(facility);

        bgService.updateStatus(1L, "RELEASED", "ops-user");

        assertThat(facility.getUtilizedAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("updateStatus: should throw IllegalStateException when compliance hold is active")
    void updateStatus_complianceHold_throws() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("500000"), BigDecimal.ZERO);
        BankGuarantee bg = buildBG(1L, "PENDING_APPROVAL", new BigDecimal("200000"), client, facility);

        when(bgRepository.findById(1L)).thenReturn(Optional.of(bg));
        SanctionsScreening flagged = new SanctionsScreening();
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus(any(), eq("FLAGGED"))).thenReturn(List.of(flagged));

        assertThatThrownBy(() -> bgService.updateStatus(1L, "ACTIVE", "ops-user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLIANCE_HOLD");
    }

    // ─── submitForApproval ────────────────────────────────────────────────────

    @Test
    @DisplayName("submitForApproval: DRAFT BG should move to PENDING_APPROVAL")
    void submitForApproval_success() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("500000"), BigDecimal.ZERO);
        BankGuarantee bg = buildBG(1L, "DRAFT", new BigDecimal("200000"), client, facility);

        when(bgRepository.findById(1L)).thenReturn(Optional.of(bg));
        when(bgRepository.save(any())).thenReturn(bg);

        BankGuarantee result = bgService.submitForApproval(1L, "client-user");
        assertThat(result.getStatus()).isEqualTo(BankGuaranteeStatus.PENDING_APPROVAL);
    }

    @Test
    @DisplayName("submitForApproval: non-DRAFT BG should throw BadRequestException")
    void submitForApproval_notDraft_throwsBadRequest() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("500000"), BigDecimal.ZERO);
        BankGuarantee bg = buildBG(1L, "ACTIVE", new BigDecimal("200000"), client, facility);

        when(bgRepository.findById(1L)).thenReturn(Optional.of(bg));

        assertThatThrownBy(() -> bgService.submitForApproval(1L, "client-user"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DRAFT");
    }

    // ─── fileClaim / processClaim ─────────────────────────────────────────────

    @Test
    @DisplayName("fileClaim: should create PENDING claim with generated claimRef")
    void fileClaim_success() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("500000"), new BigDecimal("200000"));
        BankGuarantee bg = buildBG(1L, "ACTIVE", new BigDecimal("200000"), client, facility);

        when(bgRepository.findById(1L)).thenReturn(Optional.of(bg));
        when(claimRepository.save(any())).thenAnswer(inv -> {
            BGClaim c = inv.getArgument(0);
            c.setId(300L);
            return c;
        });

        BGClaim result = bgService.fileClaim(1L, new BigDecimal("200000"), "Bank transfer", "beneficiary");

        assertThat(result.getStatus()).isEqualTo(BGClaimStatus.PENDING);
        assertThat(result.getClaimRef()).startsWith("CLM-");
    }

    @Test
    @DisplayName("processClaim: APPROVED should set BG to CLAIMED and release facility limit")
    void processClaim_approved_releasesLimit() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("500000"), new BigDecimal("200000"));
        BankGuarantee bg = buildBG(1L, "ACTIVE", new BigDecimal("200000"), client, facility);

        BGClaim claim = new BGClaim();
        claim.setId(300L);
        claim.setClaimRef("CLM-BG-2026-0001-9999");
        claim.setAmount(new BigDecimal("200000"));
        claim.setBg(bg);
        claim.setStatus(BGClaimStatus.PENDING);

        when(claimRepository.findById(300L)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any())).thenReturn(claim);
        when(bgRepository.save(any())).thenReturn(bg);
        when(facilityRepository.save(any())).thenReturn(facility);

        BGClaim result = bgService.processClaim(300L, "APPROVED", "ops-user");

        assertThat(result.getStatus()).isEqualTo(BGClaimStatus.APPROVED);
        assertThat(bg.getStatus()).isEqualTo(BankGuaranteeStatus.CLAIMED);
        assertThat(facility.getUtilizedAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("getBGsByRelationshipManagerId: should return BGs belonging to RM clients")
    void getBGsByRelationshipManagerId_returnsBGs() {
        when(bgRepository.findByClientRelationshipManagerId(101L)).thenReturn(List.of(new BankGuarantee(), new BankGuarantee()));
        List<BankGuarantee> result = bgService.getBGsByRelationshipManagerId(101L);
        assertThat(result).hasSize(2);
        verify(bgRepository).findByClientRelationshipManagerId(101L);
    }
}
