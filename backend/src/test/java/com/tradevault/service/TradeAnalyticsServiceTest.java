package com.tradevault.service;

import com.tradevault.entity.*;
import com.tradevault.entity.enums.*;
import com.tradevault.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradeAnalyticsServiceTest {

    @Mock
    private LetterOfCreditRepository lcRepository;

    @Mock
    private BankGuaranteeRepository bgRepository;

    @Mock
    private ExportBillRepository billRepository;

    @Mock
    private CreditFacilityRepository facilityRepository;

    @Mock
    private SanctionsScreeningRepository screeningRepository;

    @Mock
    private ComplianceCaseRepository caseRepository;

    @InjectMocks
    private TradeAnalyticsServiceImpl analyticsService;

    private LetterOfCredit activeLc;
    private LetterOfCredit draftLc;
    private BankGuarantee activeBg;
    private BankGuarantee draftBg;
    private ExportBill sentBill;
    private ExportBill paidBill;
    private CreditFacility activeFacility;

    @BeforeEach
    void setUp() {
        activeLc = new LetterOfCredit();
        activeLc.setAmount(new BigDecimal("150000.00"));
        activeLc.setStatus(LetterOfCreditStatus.ACTIVE);

        draftLc = new LetterOfCredit();
        draftLc.setAmount(new BigDecimal("50000.00"));
        draftLc.setStatus(LetterOfCreditStatus.DRAFT);

        activeBg = new BankGuarantee();
        activeBg.setAmount(new BigDecimal("200000.00"));
        activeBg.setStatus(BankGuaranteeStatus.ACTIVE);

        draftBg = new BankGuarantee();
        draftBg.setAmount(new BigDecimal("80000.00"));
        draftBg.setStatus(BankGuaranteeStatus.DRAFT);

        sentBill = new ExportBill();
        sentBill.setAmount(new BigDecimal("100000.00"));
        sentBill.setStatus(ExportBillStatus.DOCUMENTS_SENT);

        paidBill = new ExportBill();
        paidBill.setAmount(new BigDecimal("70000.00"));
        paidBill.setStatus(ExportBillStatus.PAID);

        activeFacility = new CreditFacility();
        activeFacility.setLimitAmount(new BigDecimal("1000000.00"));
        activeFacility.setUtilizedAmount(new BigDecimal("450000.00"));
        activeFacility.setStatus(CreditFacilityStatus.ACTIVE);
    }

    @Test
    void getAnalyticsSummary_success() {
        // Mocking findAll and counts
        when(lcRepository.findAll()).thenReturn(Arrays.asList(activeLc, draftLc));
        when(bgRepository.findAll()).thenReturn(Arrays.asList(activeBg, draftBg));
        when(billRepository.findAll()).thenReturn(Arrays.asList(sentBill, paidBill));
        when(facilityRepository.findAll()).thenReturn(Collections.singletonList(activeFacility));
        when(screeningRepository.count()).thenReturn(15L);
        when(caseRepository.countByCaseStatus(ComplianceCaseStatus.OPEN)).thenReturn(3L);

        Map<String, Object> summary = analyticsService.getAnalyticsSummary();

        assertNotNull(summary);
        // LC exposure (150,000) + BG exposure (200,000) + Bill exposure (100,000) = 450,000
        assertEquals(new BigDecimal("450000.00"), summary.get("totalExposure"));
        assertEquals(new BigDecimal("150000.00"), summary.get("lcExposure"));
        assertEquals(new BigDecimal("200000.00"), summary.get("bgExposure"));
        assertEquals(new BigDecimal("100000.00"), summary.get("billExposure"));
        assertEquals(new BigDecimal("1000000.00"), summary.get("totalLimit"));
        assertEquals(new BigDecimal("450000.00"), summary.get("totalUtilized"));
        assertEquals(new BigDecimal("45.00"), summary.get("utilizationRate"));
        assertEquals(1L, summary.get("activeLcsCount"));
        assertEquals(1L, summary.get("activeBgsCount"));
        assertEquals(1L, summary.get("activeBillsCount")); // paid is excluded from count (since status != PAID is active)
        assertEquals(15L, summary.get("totalScreenings"));
        assertEquals(3L, summary.get("openComplianceCases"));
    }

    @Test
    void getAnalyticsSummaryForClient_success() {
        Long clientId = 1L;
        when(lcRepository.findByClientId(clientId)).thenReturn(Arrays.asList(activeLc, draftLc));
        when(bgRepository.findByClientId(clientId)).thenReturn(Arrays.asList(activeBg, draftBg));
        when(billRepository.findByClientId(clientId)).thenReturn(Arrays.asList(sentBill, paidBill));
        when(facilityRepository.findByClientId(clientId)).thenReturn(Collections.singletonList(activeFacility));

        Map<String, Object> summary = analyticsService.getAnalyticsSummaryForClient(clientId);

        assertNotNull(summary);
        assertEquals(new BigDecimal("450000.00"), summary.get("totalExposure"));
        assertEquals(new BigDecimal("45.00"), summary.get("utilizationRate"));
        assertEquals(1L, summary.get("activeLcsCount"));
        assertEquals(1L, summary.get("activeBgsCount"));
        assertEquals(1L, summary.get("activeBillsCount"));
        assertEquals(0L, summary.get("totalScreenings"));
        assertEquals(0L, summary.get("openComplianceCases"));
    }

    @Test
    void getAnalyticsSummaryForRelationshipManager_success() {
        Long rmId = 2L;
        when(lcRepository.findByClientRelationshipManagerId(rmId)).thenReturn(Arrays.asList(activeLc, draftLc));
        when(bgRepository.findByClientRelationshipManagerId(rmId)).thenReturn(Arrays.asList(activeBg, draftBg));
        when(billRepository.findByClientRelationshipManagerId(rmId)).thenReturn(Arrays.asList(sentBill, paidBill));
        when(facilityRepository.findByClientRelationshipManagerId(rmId)).thenReturn(Collections.singletonList(activeFacility));

        Map<String, Object> summary = analyticsService.getAnalyticsSummaryForRelationshipManager(rmId);

        assertNotNull(summary);
        assertEquals(new BigDecimal("450000.00"), summary.get("totalExposure"));
        assertEquals(new BigDecimal("45.00"), summary.get("utilizationRate"));
        assertEquals(1L, summary.get("activeLcsCount"));
        assertEquals(1L, summary.get("activeBgsCount"));
        assertEquals(1L, summary.get("activeBillsCount"));
        assertEquals(0L, summary.get("totalScreenings"));
        assertEquals(0L, summary.get("openComplianceCases"));
    }
}
