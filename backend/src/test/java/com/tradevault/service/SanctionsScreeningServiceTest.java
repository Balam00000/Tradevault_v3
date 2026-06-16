package com.tradevault.service;

import com.tradevault.entity.ComplianceCase;
import com.tradevault.entity.SanctionsScreening;
import com.tradevault.entity.enums.ComplianceCaseStatus;
import com.tradevault.entity.enums.SanctionsScreeningStatus;
import com.tradevault.entity.enums.ScreeningEntityType;
import com.tradevault.repository.ComplianceCaseRepository;
import com.tradevault.repository.SanctionsScreeningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SanctionsScreeningServiceTest {

    @Mock
    private SanctionsScreeningRepository screeningRepository;

    @Mock
    private ComplianceCaseRepository caseRepository;

    @InjectMocks
    private SanctionsScreeningServiceImpl sanctionsScreeningService;

    private ComplianceCase complianceCase;
    private SanctionsScreening screening;

    @BeforeEach
    void setUp() {
        screening = new SanctionsScreening();
        screening.setId(100L);
        screening.setEntityName("Syrian Trade Corp");
        screening.setEntityType(ScreeningEntityType.BENEFICIARY);
        screening.setTransactionType("LC");
        screening.setTransactionId("LC-99999");
        screening.setStatus(SanctionsScreeningStatus.FLAGGED);
        screening.setMatchScore(new BigDecimal("89.50"));

        complianceCase = new ComplianceCase();
        complianceCase.setId(200L);
        complianceCase.setScreening(screening);
        complianceCase.setCaseStatus(ComplianceCaseStatus.OPEN);
        complianceCase.setAssignedTo("compliance");
    }

    @Test
    void screenEntity_flaggedHighRisk() {
        when(screeningRepository.save(any(SanctionsScreening.class))).thenAnswer(invocation -> {
            SanctionsScreening s = invocation.getArgument(0);
            s.setId(100L);
            return s;
        });
        when(caseRepository.save(any(ComplianceCase.class))).thenAnswer(invocation -> {
            ComplianceCase c = invocation.getArgument(0);
            c.setId(200L);
            return c;
        });

        SanctionsScreening result = sanctionsScreeningService.screenEntity("Syria Exports Ltd", "BENEFICIARY", "LC", "LC-99999");

        assertNotNull(result);
        assertEquals(SanctionsScreeningStatus.FLAGGED, result.getStatus());
        assertEquals(new BigDecimal("89.50"), result.getMatchScore());
        verify(caseRepository).save(any(ComplianceCase.class));
    }

    @Test
    void screenEntity_clearedLowRisk() {
        when(screeningRepository.save(any(SanctionsScreening.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SanctionsScreening result = sanctionsScreeningService.screenEntity("Shanghai Shipping Co", "APPLICANT", "LC", "LC-99999");

        assertNotNull(result);
        assertEquals(SanctionsScreeningStatus.CLEARED, result.getStatus());
        assertEquals(new BigDecimal("12.00"), result.getMatchScore());
        verify(caseRepository, never()).save(any(ComplianceCase.class));
    }

    @Test
    void screenEntity_fullyCleared() {
        when(screeningRepository.save(any(SanctionsScreening.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SanctionsScreening result = sanctionsScreeningService.screenEntity("Clean Trader Inc", "APPLICANT", "LC", "LC-99999");

        assertNotNull(result);
        assertEquals(SanctionsScreeningStatus.CLEARED, result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getMatchScore());
        verify(caseRepository, never()).save(any(ComplianceCase.class));
    }

    @Test
    void resolveCase_resolvedCleared() {
        when(caseRepository.findById(200L)).thenReturn(Optional.of(complianceCase));
        when(caseRepository.save(any(ComplianceCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(screeningRepository.save(any(SanctionsScreening.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComplianceCase resolved = sanctionsScreeningService.resolveCase(200L, "CLEARED", "Cleared of watchlists", "compliance_manager");

        assertNotNull(resolved);
        assertEquals(ComplianceCaseStatus.CLEARED, resolved.getCaseStatus());
        assertEquals(SanctionsScreeningStatus.CLEARED, resolved.getScreening().getStatus());
        assertEquals("compliance_manager", resolved.getAssignedTo());
        verify(screeningRepository).save(screening);
    }
}
