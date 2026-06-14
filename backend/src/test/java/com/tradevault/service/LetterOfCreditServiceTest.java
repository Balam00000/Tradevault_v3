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
 * Unit tests for {@link LetterOfCreditService}.
 * All dependencies are mocked with Mockito — no Spring context is loaded.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LetterOfCreditService Unit Tests")
class LetterOfCreditServiceTest {

    @Mock private LetterOfCreditRepository lcRepository;
    @Mock private CreditFacilityRepository facilityRepository;
    @Mock private CorporateClientRepository clientRepository;
    @Mock private LCAmendmentRepository amendmentRepository;
    @Mock private LCDrawingRepository drawingRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private SanctionsScreeningService sanctionsScreeningService;
    @Mock private SanctionsScreeningRepository sanctionsScreeningRepository;
    @Mock private NotificationRepository notificationRepository;

    @InjectMocks
    private LetterOfCreditService lcService;

    // ─── Test Fixtures ─────────────────────────────────────────────────────────

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
        CorporateClient client = buildClient(clientId);
        f.setClient(client);
        return f;
    }

    private LetterOfCredit buildLC(Long id, String status, BigDecimal amount, CorporateClient client, CreditFacility facility) {
        LetterOfCredit lc = new LetterOfCredit();
        lc.setId(id);
        lc.setLcNumber("LC-2026-0001");
        lc.setStatus(status != null ? LetterOfCreditStatus.valueOf(status.toUpperCase()) : null);
        lc.setAmount(amount);
        lc.setApplicantName("Test Applicant");
        lc.setBeneficiaryName("Test Beneficiary");
        lc.setExpiryDate(LocalDate.now().plusMonths(6));
        lc.setCurrency("USD");
        lc.setClient(client);
        lc.setCreditFacility(facility);
        return lc;
    }

    // ─── getAllLCs ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllLCs: should return all LCs ordered by creation date")
    void getAllLCs_returnsAllLCs() {
        when(lcRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(new LetterOfCredit(), new LetterOfCredit()));
        List<LetterOfCredit> result = lcService.getAllLCs();
        assertThat(result).hasSize(2);
        verify(lcRepository).findAllByOrderByCreatedAtDesc();
    }

    // ─── getLCById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getLCById: should throw ResourceNotFoundException when not found")
    void getLCById_notFound_throwsException() {
        when(lcRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> lcService.getLCById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getLCById: should return LC when found")
    void getLCById_found_returnsLC() {
        LetterOfCredit lc = new LetterOfCredit();
        lc.setId(1L);
        when(lcRepository.findById(1L)).thenReturn(Optional.of(lc));
        LetterOfCredit result = lcService.getLCById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    // ─── createLC ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createLC: should create LC successfully with sufficient limit")
    void createLC_success() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("1000000"), BigDecimal.ZERO);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(facilityRepository.findById(10L)).thenReturn(Optional.of(facility));
        lenient().when(sanctionsScreeningRepository.findByTransactionIdAndStatus(any(), any())).thenReturn(List.of());
        when(sanctionsScreeningService.screenEntity(any(), any(), any(), any())).thenReturn(new SanctionsScreening());

        LetterOfCredit lc = new LetterOfCredit();
        lc.setAmount(new BigDecimal("500000"));
        lc.setApplicantName("ACME Corp");
        lc.setBeneficiaryName("Supplier Ltd");
        lc.setExpiryDate(LocalDate.now().plusMonths(6));
        lc.setCurrency("USD");
        lc.setLcType(LCType.SIGHT);

        when(lcRepository.save(any())).thenAnswer(inv -> {
            LetterOfCredit saved = inv.getArgument(0);
            saved.setId(100L);
            if (saved.getLcNumber() == null) saved.setLcNumber("LC-2026-1000");
            return saved;
        });

        LetterOfCredit result = lcService.createLC(lc, 1L, 10L, "testuser");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(LetterOfCreditStatus.DRAFT);
        assertThat(result.getLcNumber()).isNotBlank();
        verify(auditLogService).log(isNull(), eq("testuser"), eq("LC_CREATION_DRAFT"), any(), isNull());
        verify(sanctionsScreeningService, times(2)).screenEntity(any(), any(), eq("LC"), any());
    }

    @Test
    @DisplayName("createLC: should throw BadRequestException when facility limit is insufficient")
    void createLC_insufficientLimit_throwsBadRequest() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("100000"), new BigDecimal("90000"));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(facilityRepository.findById(10L)).thenReturn(Optional.of(facility));

        LetterOfCredit lc = new LetterOfCredit();
        lc.setAmount(new BigDecimal("50000")); // Available = 10000, requested = 50000
        lc.setApplicantName("ACME");
        lc.setBeneficiaryName("Supplier");

        assertThatThrownBy(() -> lcService.createLC(lc, 1L, 10L, "testuser"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient credit facility limit");
        verify(lcRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLC: should throw BadRequestException when facility does not belong to client")
    void createLC_facilityClientMismatch_throwsBadRequest() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 999L, new BigDecimal("1000000"), BigDecimal.ZERO); // belongs to client 999

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(facilityRepository.findById(10L)).thenReturn(Optional.of(facility));

        LetterOfCredit lc = new LetterOfCredit();
        lc.setAmount(new BigDecimal("500000"));

        assertThatThrownBy(() -> lcService.createLC(lc, 1L, 10L, "testuser"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to the selected client");
    }

    // ─── updateStatus ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus: should update LC to ACTIVE and block facility limit")
    void updateStatus_toActive_blocksFacilityLimit() {
        CorporateClient client = buildClient(1L);
        client.setId(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("1000000"), BigDecimal.ZERO);
        LetterOfCredit lc = buildLC(1L, "IN_REVIEW", new BigDecimal("500000"), client, facility);

        when(lcRepository.findById(1L)).thenReturn(Optional.of(lc));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus(any(), any())).thenReturn(List.of());
        when(notificationRepository.save(any())).thenReturn(new Notification());
        when(lcRepository.save(any())).thenReturn(lc);
        when(facilityRepository.save(any())).thenReturn(facility);

        LetterOfCredit updated = lcService.updateStatus(1L, "ACTIVE", "ops-user");

        assertThat(updated.getStatus()).isEqualTo(LetterOfCreditStatus.ACTIVE);
        assertThat(facility.getUtilizedAmount()).isEqualByComparingTo("500000");
        verify(facilityRepository).save(facility);
        verify(notificationRepository).save(any());
    }

    @Test
    @DisplayName("updateStatus: should throw IllegalStateException when compliance hold is active")
    void updateStatus_complianceHold_throwsIllegalState() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("1000000"), BigDecimal.ZERO);
        LetterOfCredit lc = buildLC(1L, "IN_REVIEW", new BigDecimal("500000"), client, facility);

        when(lcRepository.findById(1L)).thenReturn(Optional.of(lc));
        SanctionsScreening flagged = new SanctionsScreening();
        flagged.setStatus(SanctionsScreeningStatus.FLAGGED);
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus(any(), eq("FLAGGED"))).thenReturn(List.of(flagged));

        assertThatThrownBy(() -> lcService.updateStatus(1L, "ACTIVE", "ops-user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLIANCE_HOLD");
        verify(lcRepository, never()).save(any());
    }

    // ─── requestAmendment ─────────────────────────────────────────────────────

    @Test
    @DisplayName("requestAmendment: should create amendment with PENDING_APPROVAL status")
    void requestAmendment_success() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("1000000"), new BigDecimal("500000"));
        LetterOfCredit lc = buildLC(1L, "ACTIVE", new BigDecimal("500000"), client, facility);

        when(lcRepository.findById(1L)).thenReturn(Optional.of(lc));
        when(amendmentRepository.findByLcIdOrderByAmendmentNumberDesc(1L)).thenReturn(List.of());
        when(amendmentRepository.save(any())).thenAnswer(inv -> {
            LCAmendment a = inv.getArgument(0);
            a.setId(50L);
            return a;
        });

        LCAmendment result = lcService.requestAmendment(1L, new BigDecimal("600000"),
                LocalDate.now().plusMonths(9), "Need more time", "client-user");

        assertThat(result.getStatus()).isEqualTo(LCAmendmentStatus.PENDING_APPROVAL);
        assertThat(result.getAmendmentNumber()).isEqualTo(1);
        assertThat(result.getNewAmount()).isEqualByComparingTo("600000");
    }

    // ─── processAmendment ─────────────────────────────────────────────────────

    @Test
    @DisplayName("processAmendment: APPROVED should update LC amount and facility utilization")
    void processAmendment_approved_adjustsFacilityLimit() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("1000000"), new BigDecimal("500000"));
        LetterOfCredit lc = buildLC(1L, "ACTIVE", new BigDecimal("500000"), client, facility);

        LCAmendment amendment = new LCAmendment();
        amendment.setId(50L);
        amendment.setLc(lc);
        amendment.setPreviousAmount(new BigDecimal("500000"));
        amendment.setNewAmount(new BigDecimal("700000"));
        amendment.setPreviousExpiryDate(lc.getExpiryDate());
        amendment.setNewExpiryDate(LocalDate.now().plusMonths(12));
        amendment.setAmendmentNumber(1);

        when(amendmentRepository.findById(50L)).thenReturn(Optional.of(amendment));
        when(amendmentRepository.save(any())).thenReturn(amendment);
        when(lcRepository.save(any())).thenReturn(lc);
        when(facilityRepository.save(any())).thenReturn(facility);

        LCAmendment result = lcService.processAmendment(50L, "APPROVED", "ops-user");

        assertThat(result.getStatus()).isEqualTo(LCAmendmentStatus.APPROVED);
        // diff = 700000 - 500000 = 200000 added to utilized
        assertThat(facility.getUtilizedAmount()).isEqualByComparingTo("700000");
        assertThat(lc.getStatus()).isEqualTo(LetterOfCreditStatus.AMENDED);
    }

    // ─── presentDrawing ───────────────────────────────────────────────────────

    @Test
    @DisplayName("presentDrawing: should mark as PENDING_REVIEW when all required docs present")
    void presentDrawing_success_allDocuments() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("1000000"), new BigDecimal("500000"));
        LetterOfCredit lc = buildLC(1L, "ACTIVE", new BigDecimal("500000"), client, facility);

        when(lcRepository.findById(1L)).thenReturn(Optional.of(lc));
        when(drawingRepository.save(any())).thenAnswer(inv -> {
            LCDrawing d = inv.getArgument(0);
            d.setId(200L);
            return d;
        });

        LCDrawing result = lcService.presentDrawing(1L, new BigDecimal("300000"),
                "Bill of Lading, Commercial Invoice, Packing List", "beneficiary-user");

        assertThat(result.getStatus()).isEqualTo(LCDrawingStatus.PENDING_REVIEW);
        assertThat(result.getDiscrepancyNotes()).isNull();
    }

    @Test
    @DisplayName("presentDrawing: should mark as DISCREPANT when required docs are missing")
    void presentDrawing_missingDocs_markedDiscrepant() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("1000000"), new BigDecimal("500000"));
        LetterOfCredit lc = buildLC(1L, "ACTIVE", new BigDecimal("500000"), client, facility);

        when(lcRepository.findById(1L)).thenReturn(Optional.of(lc));
        when(drawingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Only packing list — missing Bill of Lading and Invoice
        LCDrawing result = lcService.presentDrawing(1L, new BigDecimal("300000"),
                "Packing List Only", "beneficiary-user");

        assertThat(result.getStatus()).isEqualTo(LCDrawingStatus.DISCREPANT);
        assertThat(result.getDiscrepancyNotes()).contains("Bill of Lading");
    }

    // ─── processDrawing ───────────────────────────────────────────────────────

    @Test
    @DisplayName("processDrawing: PAID status should release facility limit and set LC to DRAWN")
    void processDrawing_paid_releasesLimit() {
        CorporateClient client = buildClient(1L);
        CreditFacility facility = buildFacility(10L, 1L, new BigDecimal("1000000"), new BigDecimal("500000"));
        LetterOfCredit lc = buildLC(1L, "ACTIVE", new BigDecimal("500000"), client, facility);

        LCDrawing drawing = new LCDrawing();
        drawing.setId(200L);
        drawing.setDrawingRef("DRW-LC-2026-0001-1111");
        drawing.setAmount(new BigDecimal("300000"));
        drawing.setLc(lc);

        when(drawingRepository.findById(200L)).thenReturn(Optional.of(drawing));
        when(drawingRepository.save(any())).thenReturn(drawing);
        when(lcRepository.save(any())).thenReturn(lc);
        when(facilityRepository.save(any())).thenReturn(facility);

        LCDrawing result = lcService.processDrawing(200L, "PAID", null, "ops-user");

        assertThat(result.getStatus()).isEqualTo(LCDrawingStatus.PAID);
        // 500000 - 300000 = 200000 utilized remaining
        assertThat(facility.getUtilizedAmount()).isEqualByComparingTo("200000");
        assertThat(lc.getStatus()).isEqualTo(LetterOfCreditStatus.DRAWN);
        verify(auditLogService).log(isNull(), eq("ops-user"), eq("LC_DRAWING_PAID"), any(), isNull());
    }

    @Test
    @DisplayName("getLCsByRelationshipManagerId: should return LCs belonging to RM clients")
    void getLCsByRelationshipManagerId_returnsLCs() {
        when(lcRepository.findByClientRelationshipManagerId(101L)).thenReturn(List.of(new LetterOfCredit(), new LetterOfCredit()));
        List<LetterOfCredit> result = lcService.getLCsByRelationshipManagerId(101L);
        assertThat(result).hasSize(2);
        verify(lcRepository).findByClientRelationshipManagerId(101L);
    }
}
