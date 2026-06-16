package com.tradevault.service;

import com.tradevault.entity.*;
import com.tradevault.entity.enums.*;
import com.tradevault.exception.BadRequestException;
import com.tradevault.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BankGuaranteeServiceTest {

    @Mock
    private BankGuaranteeRepository bgRepository;

    @Mock
    private CreditFacilityRepository facilityRepository;

    @Mock
    private CorporateClientRepository clientRepository;

    @Mock
    private BGClaimRepository claimRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SanctionsScreeningService sanctionsScreeningService;

    @Mock
    private SanctionsScreeningRepository sanctionsScreeningRepository;

    @InjectMocks
    private BankGuaranteeServiceImpl bgService;

    private CorporateClient client;
    private CreditFacility facility;
    private BankGuarantee bg;

    @BeforeEach
    void setUp() {
        client = new CorporateClient();
        client.setId(1L);
        client.setCompanyName("Acme Corp");

        facility = new CreditFacility();
        facility.setId(2L);
        facility.setClient(client);
        facility.setLimitAmount(new BigDecimal("1000000.00"));
        facility.setUtilizedAmount(new BigDecimal("200000.00"));

        bg = new BankGuarantee();
        bg.setId(10L);
        bg.setClient(client);
        bg.setCreditFacility(facility);
        bg.setAmount(new BigDecimal("100000.00"));
        bg.setBgNumber("BG-12345");
        bg.setStatus(BankGuaranteeStatus.DRAFT);
        bg.setBeneficiaryName("Global Supplier");
    }

    @Test
    void createBG_success() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(facilityRepository.findById(2L)).thenReturn(Optional.of(facility));
        when(bgRepository.save(any(BankGuarantee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BankGuarantee created = bgService.createBG(bg, 1L, 2L, "testuser");

        assertNotNull(created);
        assertEquals(BankGuaranteeStatus.DRAFT, created.getStatus());
        verify(sanctionsScreeningService).screenEntity("Global Supplier", "BENEFICIARY", "BG", created.getBgNumber());
        verify(auditLogService).log(isNull(), eq("testuser"), eq("BG_CREATION_DRAFT"), anyString(), isNull());
    }

    @Test
    void createBG_insufficientLimit() {
        bg.setAmount(new BigDecimal("900000.00")); // available is 800000
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(facilityRepository.findById(2L)).thenReturn(Optional.of(facility));

        assertThrows(BadRequestException.class, () -> bgService.createBG(bg, 1L, 2L, "testuser"));
    }

    @Test
    void updateStatus_complianceHold() {
        when(bgRepository.findById(10L)).thenReturn(Optional.of(bg));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus("BG-12345", SanctionsScreeningStatus.FLAGGED))
                .thenReturn(Collections.singletonList(new SanctionsScreening()));

        assertThrows(IllegalStateException.class, () -> bgService.updateStatus(10L, BankGuaranteeStatus.ACTIVE, "testuser"));
    }

    @Test
    void updateStatus_activeSuccess() {
        when(bgRepository.findById(10L)).thenReturn(Optional.of(bg));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus("BG-12345", SanctionsScreeningStatus.FLAGGED))
                .thenReturn(Collections.emptyList());
        when(bgRepository.save(any(BankGuarantee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BankGuarantee updated = bgService.updateStatus(10L, BankGuaranteeStatus.ACTIVE, "testuser");

        assertEquals(BankGuaranteeStatus.ACTIVE, updated.getStatus());
        // Utilized amount should increase from 200,000 to 300,000
        assertEquals(new BigDecimal("300000.00"), facility.getUtilizedAmount());
        verify(facilityRepository).save(facility);
    }

    @Test
    void updateStatus_releasedLimitRefund() {
        bg.setStatus(BankGuaranteeStatus.ACTIVE);
        when(bgRepository.findById(10L)).thenReturn(Optional.of(bg));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus("BG-12345", SanctionsScreeningStatus.FLAGGED))
                .thenReturn(Collections.emptyList());
        when(bgRepository.save(any(BankGuarantee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BankGuarantee updated = bgService.updateStatus(10L, BankGuaranteeStatus.RELEASED, "testuser");

        assertEquals(BankGuaranteeStatus.RELEASED, updated.getStatus());
        // Utilized amount should refund from 200,000 to 100,000
        assertEquals(new BigDecimal("100000.00"), facility.getUtilizedAmount());
        verify(facilityRepository).save(facility);
    }

    @Test
    void submitForApproval_success() {
        when(bgRepository.findById(10L)).thenReturn(Optional.of(bg));
        when(bgRepository.save(any(BankGuarantee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BankGuarantee updated = bgService.submitForApproval(10L, "testuser");

        assertEquals(BankGuaranteeStatus.PENDING_APPROVAL, updated.getStatus());
        verify(auditLogService).log(isNull(), eq("testuser"), eq("BG_SUBMITTED_FOR_APPROVAL"), anyString(), isNull());
    }

    @Test
    void submitForApproval_invalidStatus() {
        bg.setStatus(BankGuaranteeStatus.ACTIVE);
        when(bgRepository.findById(10L)).thenReturn(Optional.of(bg));

        assertThrows(BadRequestException.class, () -> bgService.submitForApproval(10L, "testuser"));
    }

    @Test
    void fileClaim_success() {
        when(bgRepository.findById(10L)).thenReturn(Optional.of(bg));
        when(claimRepository.save(any(BGClaim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BGClaim claim = bgService.fileClaim(10L, new BigDecimal("50000.00"), "Wire details", "testuser");

        assertNotNull(claim);
        assertEquals(BGClaimStatus.PENDING, claim.getStatus());
        assertEquals(new BigDecimal("50000.00"), claim.getAmount());
    }

    @Test
    void processClaim_approvedSuccess() {
        BGClaim claim = new BGClaim();
        claim.setId(50L);
        claim.setBg(bg);
        claim.setAmount(new BigDecimal("50000.00"));
        claim.setStatus(BGClaimStatus.PENDING);

        when(claimRepository.findById(50L)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(BGClaim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BGClaim processed = bgService.processClaim(50L, "APPROVED", "testuser");

        assertEquals(BGClaimStatus.APPROVED, processed.getStatus());
        assertEquals(BankGuaranteeStatus.CLAIMED, bg.getStatus());
        // Utilized amount should refund from 200,000 to 100,000 (refunds utilizing limit amount up to bg amount)
        assertEquals(new BigDecimal("100000.00"), facility.getUtilizedAmount());
    }
}
