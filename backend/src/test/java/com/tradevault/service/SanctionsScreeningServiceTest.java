package com.tradevault.service;

import com.tradevault.entity.ComplianceCase;
import com.tradevault.entity.SanctionsScreening;
import com.tradevault.entity.enums.*;
import com.tradevault.repository.ComplianceCaseRepository;
import com.tradevault.repository.SanctionsScreeningRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SanctionsScreeningService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SanctionsScreeningService Unit Tests")
class SanctionsScreeningServiceTest {

    @Mock private SanctionsScreeningRepository screeningRepository;
    @Mock private ComplianceCaseRepository caseRepository;

    @InjectMocks
    private SanctionsScreeningServiceImpl screeningService;

    // ─── screenEntity — CLEARED ────────────────────────────────────────────────

    @Test
    @DisplayName("screenEntity: should return CLEARED for a safe entity name")
    void screenEntity_cleared_safeEntity() {
        when(screeningRepository.save(any())).thenAnswer(inv -> {
            SanctionsScreening s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SanctionsScreening result = screeningService.screenEntity("Apple Inc", "BENEFICIARY", "LC", "LC-2026-0001");

        assertThat(result.getStatus()).isEqualTo(SanctionsScreeningStatus.CLEARED);
        assertThat(result.getMatchScore()).isEqualByComparingTo(BigDecimal.ZERO);
        // No compliance case created for CLEARED entities
        verify(caseRepository, never()).save(any());
    }

    // ─── screenEntity — FLAGGED (high-risk) ───────────────────────────────────

    @Test
    @DisplayName("screenEntity: should return FLAGGED for IRAN entity and create compliance case")
    void screenEntity_flagged_iranEntity_createsComplianceCase() {
        SanctionsScreening saved = new SanctionsScreening();
        saved.setId(10L);
        saved.setStatus(SanctionsScreeningStatus.FLAGGED);
 
        when(screeningRepository.save(any())).thenReturn(saved);
        when(caseRepository.save(any())).thenAnswer(inv -> {
            ComplianceCase cc = inv.getArgument(0);
            cc.setId(50L);
            return cc;
        });
 
        SanctionsScreening result = screeningService.screenEntity("IRAN State Bank", "COUNTERPARTY", "LC", "LC-2026-0002");
 
        assertThat(result.getStatus()).isEqualTo(SanctionsScreeningStatus.FLAGGED);
        // Compliance case must be created automatically
        verify(caseRepository).save(argThat(cc ->
                cc.getCaseStatus() == ComplianceCaseStatus.OPEN && cc.getScreening() == saved
        ));
    }

    @Test
    @DisplayName("screenEntity: should return FLAGGED for SYRIA entity")
    void screenEntity_flagged_syriaEntity() {
        when(screeningRepository.save(any())).thenAnswer(inv -> {
            SanctionsScreening s = inv.getArgument(0);
            s.setId(20L);
            s.setStatus(SanctionsScreeningStatus.FLAGGED);
            return s;
        });
        when(caseRepository.save(any())).thenReturn(new ComplianceCase());
 
        SanctionsScreening result = screeningService.screenEntity("Syria Trading Co", "APPLICANT", "BG", "BG-2026-0001");
 
        assertThat(result.getStatus()).isEqualTo(SanctionsScreeningStatus.FLAGGED);
        verify(caseRepository).save(any());
    }

    @Test
    @DisplayName("screenEntity: should return CLEARED for SHANGHAI with low score and no case created")
    void screenEntity_cleared_shanghaiLowScore() {
        when(screeningRepository.save(any())).thenAnswer(inv -> {
            SanctionsScreening s = inv.getArgument(0);
            s.setId(30L);
            s.setStatus(SanctionsScreeningStatus.CLEARED);
            return s;
        });
 
        SanctionsScreening result = screeningService.screenEntity("Shanghai Exports Ltd", "BENEFICIARY", "LC", "LC-2026-0003");
 
        assertThat(result.getStatus()).isEqualTo(SanctionsScreeningStatus.CLEARED);
        verify(caseRepository, never()).save(any());
    }

    // ─── resolveCase ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("resolveCase: RESOLVED_CLEARED should update screening status to CLEARED")
    void resolveCase_resolvedCleared_updatesScreening() {
        SanctionsScreening screening = new SanctionsScreening();
        screening.setId(10L);
        screening.setStatus(SanctionsScreeningStatus.FLAGGED);
 
        ComplianceCase compCase = new ComplianceCase();
        compCase.setId(50L);
        compCase.setCaseStatus(ComplianceCaseStatus.OPEN);
        compCase.setScreening(screening);
 
        when(caseRepository.findById(50L)).thenReturn(Optional.of(compCase));
        when(caseRepository.save(any())).thenReturn(compCase);
        when(screeningRepository.save(any())).thenReturn(screening);
 
        ComplianceCase result = screeningService.resolveCase(50L, "RESOLVED_CLEARED", "False positive — verified safe entity", "compliance-officer");
 
        assertThat(result.getCaseStatus()).isEqualTo(ComplianceCaseStatus.RESOLVED_CLEARED);
        assertThat(screening.getStatus()).isEqualTo(SanctionsScreeningStatus.CLEARED);
        verify(screeningRepository).save(screening);
    }

    @Test
    @DisplayName("resolveCase: RESOLVED_BLOCKED should keep screening status as FLAGGED")
    void resolveCase_resolvedBlocked_keepsFlagged() {
        SanctionsScreening screening = new SanctionsScreening();
        screening.setId(10L);
        screening.setStatus(SanctionsScreeningStatus.FLAGGED);
 
        ComplianceCase compCase = new ComplianceCase();
        compCase.setId(50L);
        compCase.setCaseStatus(ComplianceCaseStatus.OPEN);
        compCase.setScreening(screening);
 
        when(caseRepository.findById(50L)).thenReturn(Optional.of(compCase));
        when(caseRepository.save(any())).thenReturn(compCase);
        when(screeningRepository.save(any())).thenReturn(screening);
 
        screeningService.resolveCase(50L, "RESOLVED_BLOCKED", "Confirmed sanctioned entity", "compliance-officer");
 
        assertThat(screening.getStatus()).isEqualTo(SanctionsScreeningStatus.FLAGGED);
    }

    @Test
    @DisplayName("resolveCase: should throw RuntimeException when case not found")
    void resolveCase_notFound_throws() {
        when(caseRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> screeningService.resolveCase(999L, "RESOLVED_CLEARED", "notes", "officer"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ─── getAllScreenings / getAllCases ────────────────────────────────────────

    @Test
    @DisplayName("getAllScreenings: should return all screenings ordered by date")
    void getAllScreenings_returnsOrdered() {
        when(screeningRepository.findAllByOrderByScreenedAtDesc()).thenReturn(
                List.of(new SanctionsScreening(), new SanctionsScreening(), new SanctionsScreening()));
        assertThat(screeningService.getAllScreenings()).hasSize(3);
    }

    @Test
    @DisplayName("getAllCases: should return all cases ordered by creation date")
    void getAllCases_returnsOrdered() {
        when(caseRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(new ComplianceCase()));
        assertThat(screeningService.getAllCases()).hasSize(1);
    }
}
