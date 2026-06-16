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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradeReportServiceTest {

    @Mock
    private TradeReportRepository tradeReportRepository;

    @Mock
    private LetterOfCreditRepository letterOfCreditRepository;

    @Mock
    private BankGuaranteeRepository bankGuaranteeRepository;

    @Mock
    private ExportBillRepository exportBillRepository;

    @Mock
    private CreditFacilityRepository creditFacilityRepository;

    @Mock
    private ComplianceCaseRepository complianceCaseRepository;

    @Mock
    private LCDrawingRepository lcDrawingRepository;

    @InjectMocks
    private TradeReportServiceImpl tradeReportService;

    private LetterOfCredit lc;
    private BankGuarantee bg;
    private ExportBill eb;
    private CreditFacility facility;
    private LCDrawing paidDrawing;
    private LCDrawing pendingDrawing;

    @BeforeEach
    void setUp() {
        lc = new LetterOfCredit();
        lc.setAmount(new BigDecimal("100000.00"));

        bg = new BankGuarantee();
        bg.setAmount(new BigDecimal("200000.00"));

        eb = new ExportBill();
        eb.setAmount(new BigDecimal("50000.00"));

        facility = new CreditFacility();
        facility.setLimitAmount(new BigDecimal("500000.00"));
        facility.setUtilizedAmount(new BigDecimal("150000.00"));

        paidDrawing = new LCDrawing();
        paidDrawing.setStatus(LCDrawingStatus.PAID);

        pendingDrawing = new LCDrawing();
        pendingDrawing.setStatus(LCDrawingStatus.PENDING_REVIEW);
    }

    @Test
    void getAllReports_success() {
        TradeReport report = new TradeReport(ReportScope.CLIENT, "{}");
        when(tradeReportRepository.findAll()).thenReturn(Collections.singletonList(report));

        List<TradeReport> result = tradeReportService.getAllReports();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tradeReportRepository).findAll();
    }

    @Test
    void getReportsByScope_success() {
        TradeReport report = new TradeReport(ReportScope.CLIENT, "{}");
        when(tradeReportRepository.findByScopeOrderByGeneratedDateDesc(ReportScope.CLIENT))
                .thenReturn(Collections.singletonList(report));

        List<TradeReport> result = tradeReportService.getReportsByScope(ReportScope.CLIENT);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tradeReportRepository).findByScopeOrderByGeneratedDateDesc(ReportScope.CLIENT);
    }

    @Test
    void generateReport_success() {
        // Counts
        when(letterOfCreditRepository.count()).thenReturn(1L);
        when(bankGuaranteeRepository.count()).thenReturn(1L);
        when(exportBillRepository.count()).thenReturn(1L);

        // FindAll for amounts
        when(letterOfCreditRepository.findAll()).thenReturn(Collections.singletonList(lc));
        when(bankGuaranteeRepository.findAll()).thenReturn(Collections.singletonList(bg));
        when(exportBillRepository.findAll()).thenReturn(Collections.singletonList(eb));

        // Credit Facilities
        when(creditFacilityRepository.findAll()).thenReturn(Collections.singletonList(facility));

        // Compliance flags
        when(complianceCaseRepository.count()).thenReturn(2L);

        // LC Drawings
        when(lcDrawingRepository.count()).thenReturn(2L);
        when(lcDrawingRepository.findAll()).thenReturn(Arrays.asList(paidDrawing, pendingDrawing));

        // Save
        when(tradeReportRepository.save(any(TradeReport.class))).thenAnswer(invocation -> {
            TradeReport report = invocation.getArgument(0);
            report.setId(99L);
            return report;
        });

        TradeReport report = tradeReportService.generateReport(ReportScope.CLIENT);

        assertNotNull(report);
        assertEquals(99L, report.getId());
        assertEquals(ReportScope.CLIENT, report.getScope());
        assertNotNull(report.getMetrics());
        
        // Let's assert JSON contains correct values
        // Total instruments: 1 + 1 + 1 = 3
        // Total Trade Value: 100k + 200k + 50k = 350k
        // Utilisation: 150k / 500k = 30.00%
        // Compliance flags: 2
        // Drawing success: 1 paid out of 2 total = 50.00%
        assertTrue(report.getMetrics().contains("\"instrumentsIssued\":3"));
        assertTrue(report.getMetrics().contains("\"totalTradeValue\":350000.00"));
        assertTrue(report.getMetrics().contains("\"facilityUtilisationRate\":30.00"));
        assertTrue(report.getMetrics().contains("\"complianceFlags\":2"));
        assertTrue(report.getMetrics().contains("\"drawingSuccessRate\":50.00"));

        verify(tradeReportRepository).save(any(TradeReport.class));
    }
}
