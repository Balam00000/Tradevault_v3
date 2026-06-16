package com.tradevault.service;

import com.tradevault.entity.*;
import com.tradevault.entity.enums.*;
import com.tradevault.exception.BadRequestException;
import com.tradevault.exception.ResourceNotFoundException;
import com.tradevault.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LetterOfCreditServiceTest {

    @Mock
    private LetterOfCreditRepository lcRepository;

    @Mock
    private CreditFacilityRepository facilityRepository;

    @Mock
    private CorporateClientRepository clientRepository;

    @Mock
    private LCAmendmentRepository amendmentRepository;

    @Mock
    private LCDrawingRepository drawingRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SanctionsScreeningService sanctionsScreeningService;

    @Mock
    private SanctionsScreeningRepository sanctionsScreeningRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private LetterOfCreditServiceImpl lcService;

    private CorporateClient client;
    private CreditFacility facility;
    private LetterOfCredit lc;

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

        lc = new LetterOfCredit();
        lc.setId(10L);
        lc.setClient(client);
        lc.setCreditFacility(facility);
        lc.setAmount(new BigDecimal("100000.00"));
        lc.setLcNumber("LC-12345");
        lc.setStatus(LetterOfCreditStatus.DRAFT);
        lc.setApplicantName("Acme Corp");
        lc.setBeneficiaryName("Global Supplier");
    }

    @Test
    void createLC_success() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(facilityRepository.findById(2L)).thenReturn(Optional.of(facility));
        when(lcRepository.save(any(LetterOfCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LetterOfCredit created = lcService.createLC(lc, 1L, 2L, "testuser");

        assertNotNull(created);
        assertEquals(LetterOfCreditStatus.DRAFT, created.getStatus());
        verify(sanctionsScreeningService).screenEntity("Acme Corp", "APPLICANT", "LC", created.getLcNumber());
        verify(sanctionsScreeningService).screenEntity("Global Supplier", "BENEFICIARY", "LC", created.getLcNumber());
        verify(auditLogService).log(isNull(), eq("testuser"), eq("LC_CREATION_DRAFT"), anyString(), isNull());
    }

    @Test
    void createLC_insufficientLimit() {
        lc.setAmount(new BigDecimal("900000.00")); // available is 800000
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(facilityRepository.findById(2L)).thenReturn(Optional.of(facility));

        assertThrows(BadRequestException.class, () -> lcService.createLC(lc, 1L, 2L, "testuser"));
    }

    @Test
    void updateStatus_complianceHold() {
        // Mock a flagged screening entry to simulate compliance hold
        when(lcRepository.findById(10L)).thenReturn(Optional.of(lc));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus("LC-12345", SanctionsScreeningStatus.FLAGGED))
                .thenReturn(Collections.singletonList(new SanctionsScreening()));

        assertThrows(IllegalStateException.class, () -> lcService.updateStatus(10L, LetterOfCreditStatus.ACTIVE, "testuser"));
    }

    @Test
    void updateStatus_activeSuccess() {
        when(lcRepository.findById(10L)).thenReturn(Optional.of(lc));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus("LC-12345", SanctionsScreeningStatus.FLAGGED))
                .thenReturn(Collections.emptyList());
        when(lcRepository.save(any(LetterOfCredit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LetterOfCredit updated = lcService.updateStatus(10L, LetterOfCreditStatus.ACTIVE, "testuser");

        assertEquals(LetterOfCreditStatus.ACTIVE, updated.getStatus());
        // Utilized amount should increase from 200,000 to 300,000
        assertEquals(new BigDecimal("300000.00"), facility.getUtilizedAmount());
        verify(facilityRepository).save(facility);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void requestAmendment_success() {
        when(lcRepository.findById(10L)).thenReturn(Optional.of(lc));
        when(amendmentRepository.findByLcIdOrderByAmendmentNumberDesc(10L)).thenReturn(new ArrayList<>());
        when(amendmentRepository.save(any(LCAmendment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LCAmendment amendment = lcService.requestAmendment(10L, new BigDecimal("150000.00"), LocalDate.now().plusMonths(3), "justification", "testuser");

        assertNotNull(amendment);
        assertEquals(1, amendment.getAmendmentNumber());
        assertEquals(LCAmendmentStatus.PENDING_APPROVAL, amendment.getStatus());
        assertEquals(new BigDecimal("150000.00"), amendment.getNewAmount());
    }

    @Test
    void processAmendment_approvedSuccess() {
        LCAmendment amendment = new LCAmendment();
        amendment.setId(30L);
        amendment.setLc(lc);
        amendment.setPreviousAmount(new BigDecimal("100000.00"));
        amendment.setNewAmount(new BigDecimal("150000.00")); // increase of 50000
        amendment.setAmendmentNumber(1);

        when(amendmentRepository.findById(30L)).thenReturn(Optional.of(amendment));
        when(amendmentRepository.save(any(LCAmendment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LCAmendment processed = lcService.processAmendment(30L, "APPROVED", "testuser");

        assertEquals(LCAmendmentStatus.APPROVED, processed.getStatus());
        assertEquals(new BigDecimal("150000.00"), lc.getAmount());
        assertEquals(LetterOfCreditStatus.AMENDED, lc.getStatus());
        // facility utilized amount should increase by 50000 -> 250000
        assertEquals(new BigDecimal("250000.00"), facility.getUtilizedAmount());
    }

    @Test
    void presentDrawing_discrepant() {
        when(lcRepository.findById(10L)).thenReturn(Optional.of(lc));
        when(drawingRepository.save(any(LCDrawing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Missing Invoice or Bill of Lading
        LCDrawing drawing = lcService.presentDrawing(10L, new BigDecimal("20000.00"), "Only Packing List", "testuser");

        assertEquals(LCDrawingStatus.DISCREPANT, drawing.getStatus());
        assertNotNull(drawing.getDiscrepancyNotes());
    }

    @Test
    void processDrawing_paidSuccess() {
        LCDrawing drawing = new LCDrawing();
        drawing.setId(40L);
        drawing.setLc(lc);
        drawing.setAmount(new BigDecimal("50000.00"));

        when(drawingRepository.findById(40L)).thenReturn(Optional.of(drawing));
        when(drawingRepository.save(any(LCDrawing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LCDrawing processed = lcService.processDrawing(40L, "PAID", null, "testuser");

        assertEquals(LCDrawingStatus.PAID, processed.getStatus());
        assertEquals(LetterOfCreditStatus.DRAWN, lc.getStatus());
        // facility utilized amount should decrease by 50000 -> 150000
        assertEquals(new BigDecimal("150000.00"), facility.getUtilizedAmount());
    }
}
