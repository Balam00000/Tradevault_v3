package com.tradevault.service;

import com.tradevault.entity.*;
import com.tradevault.entity.enums.*;
import com.tradevault.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TradeAnalyticsService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TradeAnalyticsService Unit Tests")
class TradeAnalyticsServiceTest {

    @Mock private LetterOfCreditRepository lcRepository;
    @Mock private BankGuaranteeRepository bgRepository;
    @Mock private ExportBillRepository billRepository;
    @Mock private CreditFacilityRepository facilityRepository;
    @Mock private SanctionsScreeningRepository screeningRepository;
    @Mock private ComplianceCaseRepository caseRepository;

    @InjectMocks
    private TradeAnalyticsService analyticsService;

    // ─── Fixture helpers ──────────────────────────────────────────────────────

    private LetterOfCredit buildLC(String status, BigDecimal amount) {
        LetterOfCredit lc = new LetterOfCredit();
        lc.setStatus(status != null ? LetterOfCreditStatus.valueOf(status.toUpperCase()) : null);
        lc.setAmount(amount);
        return lc;
    }

    private BankGuarantee buildBG(String status, BigDecimal amount) {
        BankGuarantee bg = new BankGuarantee();
        bg.setStatus(status != null ? BankGuaranteeStatus.valueOf(status.toUpperCase()) : null);
        bg.setAmount(amount);
        return bg;
    }

    private ExportBill buildBill(String status, BigDecimal amount) {
        ExportBill bill = new ExportBill();
        bill.setStatus(status != null ? ExportBillStatus.valueOf(status.toUpperCase()) : null);
        bill.setAmount(amount);
        return bill;
    }

    private CreditFacility buildFacility(String status, BigDecimal limit, BigDecimal utilized) {
        CreditFacility f = new CreditFacility();
        f.setStatus(status != null ? CreditFacilityStatus.valueOf(status.toUpperCase()) : null);
        f.setLimitAmount(limit);
        f.setUtilizedAmount(utilized);
        return f;
    }

    // ─── getAnalyticsSummary ──────────────────────────────────────────────────

    @Test
    @DisplayName("getAnalyticsSummary: should return all-zero summary when no data exists")
    void getAnalyticsSummary_emptyData_returnsZeros() {
        when(lcRepository.findAll()).thenReturn(List.of());
        when(bgRepository.findAll()).thenReturn(List.of());
        when(billRepository.findAll()).thenReturn(List.of());
        when(facilityRepository.findAll()).thenReturn(List.of());
        when(screeningRepository.count()).thenReturn(0L);
        when(caseRepository.countByCaseStatus(ComplianceCaseStatus.OPEN)).thenReturn(0L);

        Map<String, Object> result = analyticsService.getAnalyticsSummary();

        assertThat(result).isNotNull();
        assertThat((BigDecimal) result.get("totalExposure")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat((BigDecimal) result.get("utilizationRate")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat((Long) result.get("activeLcsCount")).isEqualTo(0L);
        assertThat((Long) result.get("activeBgsCount")).isEqualTo(0L);
    }

    @Test
    @DisplayName("getAnalyticsSummary: should calculate correct exposure for active LCs")
    void getAnalyticsSummary_withActiveLCs() {
        when(lcRepository.findAll()).thenReturn(List.of(
                buildLC("ACTIVE", new BigDecimal("500000")),
                buildLC("ACTIVE", new BigDecimal("300000")),
                buildLC("DRAFT",  new BigDecimal("200000"))  // draft — should not count
        ));
        when(bgRepository.findAll()).thenReturn(List.of());
        when(billRepository.findAll()).thenReturn(List.of());
        when(facilityRepository.findAll()).thenReturn(List.of(
                buildFacility("ACTIVE", new BigDecimal("2000000"), new BigDecimal("800000"))
        ));
        when(screeningRepository.count()).thenReturn(5L);
        when(caseRepository.countByCaseStatus(ComplianceCaseStatus.OPEN)).thenReturn(2L);

        Map<String, Object> result = analyticsService.getAnalyticsSummary();

        assertThat((BigDecimal) result.get("lcExposure")).isEqualByComparingTo("800000");
        assertThat((BigDecimal) result.get("bgExposure")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat((BigDecimal) result.get("totalExposure")).isEqualByComparingTo("800000");
        assertThat((Long) result.get("activeLcsCount")).isEqualTo(2L);
        assertThat((Long) result.get("totalScreenings")).isEqualTo(5L);
        assertThat((Long) result.get("openComplianceCases")).isEqualTo(2L);
    }

    @Test
    @DisplayName("getAnalyticsSummary: should calculate utilization rate correctly")
    void getAnalyticsSummary_utilizationRateCalculation() {
        when(lcRepository.findAll()).thenReturn(List.of());
        when(bgRepository.findAll()).thenReturn(List.of());
        when(billRepository.findAll()).thenReturn(List.of());
        when(facilityRepository.findAll()).thenReturn(List.of(
                buildFacility("ACTIVE", new BigDecimal("1000000"), new BigDecimal("250000"))
        ));
        when(screeningRepository.count()).thenReturn(0L);
        when(caseRepository.countByCaseStatus(ComplianceCaseStatus.OPEN)).thenReturn(0L);

        Map<String, Object> result = analyticsService.getAnalyticsSummary();

        // 250000 / 1000000 * 100 = 25.00
        assertThat((BigDecimal) result.get("utilizationRate")).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("getAnalyticsSummary: should only count ACTIVE facility limits")
    void getAnalyticsSummary_onlyCountsActiveFacilities() {
        when(lcRepository.findAll()).thenReturn(List.of());
        when(bgRepository.findAll()).thenReturn(List.of());
        when(billRepository.findAll()).thenReturn(List.of());
        when(facilityRepository.findAll()).thenReturn(List.of(
                buildFacility("ACTIVE",  new BigDecimal("1000000"), new BigDecimal("100000")),
                buildFacility("EXPIRED", new BigDecimal("500000"),  new BigDecimal("500000")) // should be excluded
        ));
        when(screeningRepository.count()).thenReturn(0L);
        when(caseRepository.countByCaseStatus(ComplianceCaseStatus.OPEN)).thenReturn(0L);

        Map<String, Object> result = analyticsService.getAnalyticsSummary();

        assertThat((BigDecimal) result.get("totalLimit")).isEqualByComparingTo("1000000");
        assertThat((BigDecimal) result.get("totalUtilized")).isEqualByComparingTo("100000");
    }

    // ─── getAnalyticsSummaryForClient ─────────────────────────────────────────

    @Test
    @DisplayName("getAnalyticsSummaryForClient: should return client-scoped data only")
    void getAnalyticsSummaryForClient_success() {
        Long clientId = 42L;
        when(lcRepository.findByClientId(clientId)).thenReturn(List.of(
                buildLC("ACTIVE", new BigDecimal("100000"))
        ));
        when(bgRepository.findByClientId(clientId)).thenReturn(List.of(
                buildBG("ACTIVE", new BigDecimal("50000"))
        ));
        when(billRepository.findByClientId(clientId)).thenReturn(List.of(
                buildBill("DOCUMENTS_SENT", new BigDecimal("30000"))
        ));
        when(facilityRepository.findByClientId(clientId)).thenReturn(List.of(
                buildFacility("ACTIVE", new BigDecimal("500000"), new BigDecimal("180000"))
        ));

        Map<String, Object> result = analyticsService.getAnalyticsSummaryForClient(clientId);

        assertThat((BigDecimal) result.get("lcExposure")).isEqualByComparingTo("100000");
        assertThat((BigDecimal) result.get("bgExposure")).isEqualByComparingTo("50000");
        assertThat((BigDecimal) result.get("billExposure")).isEqualByComparingTo("30000");
        assertThat((BigDecimal) result.get("totalExposure")).isEqualByComparingTo("180000");
        assertThat((Long) result.get("activeLcsCount")).isEqualTo(1L);
        assertThat((Long) result.get("activeBgsCount")).isEqualTo(1L);
    }
}
