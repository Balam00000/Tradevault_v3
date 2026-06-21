package com.tradevault.service;

import com.tradevault.entity.*;
import com.tradevault.entity.enums.*;
import com.tradevault.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TradeAnalyticsServiceImpl implements TradeAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(TradeAnalyticsServiceImpl.class);

    private final LetterOfCreditRepository lcRepository;
    private final BankGuaranteeRepository bgRepository;
    private final ExportBillRepository billRepository;
    private final CreditFacilityRepository facilityRepository;
    private final SanctionsScreeningRepository screeningRepository;
    private final ComplianceCaseRepository caseRepository;

    public TradeAnalyticsServiceImpl(
            LetterOfCreditRepository lcRepository,
            BankGuaranteeRepository bgRepository,
            ExportBillRepository billRepository,
            CreditFacilityRepository facilityRepository,
            SanctionsScreeningRepository screeningRepository,
            ComplianceCaseRepository caseRepository) {
        this.lcRepository = lcRepository;
        this.bgRepository = bgRepository;
        this.billRepository = billRepository;
        this.facilityRepository = facilityRepository;
        this.screeningRepository = screeningRepository;
        this.caseRepository = caseRepository;
    }

    // ─── Global Analytics Summary ─────────────────────────────────────────────

    public Map<String, Object> getAnalyticsSummary() {
        logger.info("Generating global trade analytics summary");
        Map<String, Object> summary = new HashMap<>();

        // Fetch all items
        List<LetterOfCredit> lcs = lcRepository.findAll();
        List<BankGuarantee> bgs = bgRepository.findAll();
        List<ExportBill> bills = billRepository.findAll();
        List<CreditFacility> facilities = facilityRepository.findAll();
        logger.debug("Analytics data loaded: {} LCs, {} BGs, {} Bills, {} Facilities", lcs.size(), bgs.size(), bills.size(), facilities.size());

        // Active Exposure
        BigDecimal lcExposure = lcs.stream()
                .filter(lc -> lc.getStatus() == LetterOfCreditStatus.ACTIVE || lc.getStatus() == LetterOfCreditStatus.AMENDED)
                .map(LetterOfCredit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal bgExposure = bgs.stream()
                .filter(bg -> bg.getStatus() == BankGuaranteeStatus.ACTIVE)
                .map(BankGuarantee::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal billExposure = bills.stream()
                .filter(bill -> bill.getStatus() == ExportBillStatus.DOCUMENTS_SENT || bill.getStatus() == ExportBillStatus.ACCEPTED)
                .map(ExportBill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExposure = lcExposure.add(bgExposure).add(billExposure);
        logger.debug("Exposure breakdown — LC={}, BG={}, Bills={}, Total={}", lcExposure, bgExposure, billExposure, totalExposure);

        // Facility Limits & Utilisation
        BigDecimal totalLimit = facilities.stream()
                .filter(f -> f.getStatus() == CreditFacilityStatus.ACTIVE)
                .map(CreditFacility::getLimitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUtilized = facilities.stream()
                .filter(f -> f.getStatus() == CreditFacilityStatus.ACTIVE)
                .map(CreditFacility::getUtilizedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal utilizationRate = totalLimit.compareTo(BigDecimal.ZERO) > 0
                ? totalUtilized.multiply(new BigDecimal("100")).divide(totalLimit, 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;
        logger.debug("Facility utilisation — limit={}, utilized={}, rate={}%", totalLimit, totalUtilized, utilizationRate);

        // Counts
        long activeLcsCount = lcs.stream().filter(lc -> lc.getStatus() == LetterOfCreditStatus.ACTIVE).count();
        long activeBgsCount = bgs.stream().filter(bg -> bg.getStatus() == BankGuaranteeStatus.ACTIVE).count();
        long activeBillsCount = bills.stream().filter(bill -> bill.getStatus() != ExportBillStatus.PAID).count();
        long totalScreenings = screeningRepository.count();
        long openComplianceCases = caseRepository.countByCaseStatus(ComplianceCaseStatus.OPEN);
        logger.debug("Counts — activeLCs={}, activeBGs={}, activeBills={}, screenings={}, openCases={}",
                activeLcsCount, activeBgsCount, activeBillsCount, totalScreenings, openComplianceCases);

        // Build result
        summary.put("totalExposure", totalExposure);
        summary.put("lcExposure", lcExposure);
        summary.put("bgExposure", bgExposure);
        summary.put("billExposure", billExposure);
        summary.put("totalLimit", totalLimit);
        summary.put("totalUtilized", totalUtilized);
        summary.put("utilizationRate", utilizationRate);
        summary.put("activeLcsCount", activeLcsCount);
        summary.put("activeBgsCount", activeBgsCount);
        summary.put("activeBillsCount", activeBillsCount);
        summary.put("totalScreenings", totalScreenings);
        summary.put("openComplianceCases", openComplianceCases);

        logger.info("Global analytics summary generated successfully: totalExposure={}, utilizationRate={}%, openComplianceCases={}",
                totalExposure, utilizationRate, openComplianceCases);
        return summary;
    }

    // ─── Client-Scoped Analytics Summary ─────────────────────────────────────

    public Map<String, Object> getAnalyticsSummaryForClient(Long clientId) {
        logger.info("Generating client-scoped analytics summary for clientId={}", clientId);
        Map<String, Object> summary = new HashMap<>();

        List<LetterOfCredit> lcs = lcRepository.findByClientId(clientId);
        List<BankGuarantee> bgs = bgRepository.findByClientId(clientId);
        List<ExportBill> bills = billRepository.findByClientId(clientId);
        List<CreditFacility> facilities = facilityRepository.findByClientId(clientId);
        logger.debug("Client analytics data loaded for clientId={}: {} LCs, {} BGs, {} Bills, {} Facilities",
                clientId, lcs.size(), bgs.size(), bills.size(), facilities.size());

        // Active Exposure
        BigDecimal lcExposure = lcs.stream()
                .filter(lc -> lc.getStatus() == LetterOfCreditStatus.ACTIVE || lc.getStatus() == LetterOfCreditStatus.AMENDED)
                .map(LetterOfCredit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal bgExposure = bgs.stream()
                .filter(bg -> bg.getStatus() == BankGuaranteeStatus.ACTIVE)
                .map(BankGuarantee::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal billExposure = bills.stream()
                .filter(bill -> bill.getStatus() == ExportBillStatus.DOCUMENTS_SENT || bill.getStatus() == ExportBillStatus.ACCEPTED)
                .map(ExportBill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExposure = lcExposure.add(bgExposure).add(billExposure);

        // Facility Limits & Utilisation
        BigDecimal totalLimit = facilities.stream()
                .filter(f -> f.getStatus() == CreditFacilityStatus.ACTIVE)
                .map(CreditFacility::getLimitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUtilized = facilities.stream()
                .filter(f -> f.getStatus() == CreditFacilityStatus.ACTIVE)
                .map(CreditFacility::getUtilizedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal utilizationRate = totalLimit.compareTo(BigDecimal.ZERO) > 0
                ? totalUtilized.multiply(new BigDecimal("100")).divide(totalLimit, 2, BigDecimal.ROUND_HALF_UP)
                : java.math.BigDecimal.ZERO;

        // Counts
        long activeLcsCount = lcs.stream().filter(lc -> lc.getStatus() == LetterOfCreditStatus.ACTIVE).count();
        long activeBgsCount = bgs.stream().filter(bg -> bg.getStatus() == BankGuaranteeStatus.ACTIVE).count();
        long activeBillsCount = bills.stream().filter(bill -> bill.getStatus() != ExportBillStatus.PAID).count();

        summary.put("totalExposure", totalExposure);
        summary.put("lcExposure", lcExposure);
        summary.put("bgExposure", bgExposure);
        summary.put("billExposure", billExposure);
        summary.put("totalLimit", totalLimit);
        summary.put("totalUtilized", totalUtilized);
        summary.put("utilizationRate", utilizationRate);
        summary.put("activeLcsCount", activeLcsCount);
        summary.put("activeBgsCount", activeBgsCount);
        summary.put("activeBillsCount", activeBillsCount);
        summary.put("totalScreenings", 0L);
        summary.put("openComplianceCases", 0L);

        logger.info("Client analytics summary generated for clientId={}: totalExposure={}, utilizationRate={}%",
                clientId, totalExposure, utilizationRate);
        return summary;
    }

    public Map<String, Object> getAnalyticsSummaryForRelationshipManager(Long rmId) {
        logger.info("Generating analytics summary for relationshipManagerId={}", rmId);
        Map<String, Object> summary = new HashMap<>();

        List<LetterOfCredit> lcs = lcRepository.findByClientRelationshipManagerId(rmId);
        List<BankGuarantee> bgs = bgRepository.findByClientRelationshipManagerId(rmId);
        List<ExportBill> bills = billRepository.findByClientRelationshipManagerId(rmId);
        List<CreditFacility> facilities = facilityRepository.findByClientRelationshipManagerId(rmId);
        logger.debug("Relationship manager analytics data loaded for rmId={}: {} LCs, {} BGs, {} Bills, {} Facilities",
                rmId, lcs.size(), bgs.size(), bills.size(), facilities.size());

        // Active Exposure
        BigDecimal lcExposure = lcs.stream()
                .filter(lc -> lc.getStatus() == LetterOfCreditStatus.ACTIVE || lc.getStatus() == LetterOfCreditStatus.AMENDED)
                .map(LetterOfCredit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal bgExposure = bgs.stream()
                .filter(bg -> bg.getStatus() == BankGuaranteeStatus.ACTIVE)
                .map(BankGuarantee::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal billExposure = bills.stream()
                .filter(bill -> bill.getStatus() == ExportBillStatus.DOCUMENTS_SENT || bill.getStatus() == ExportBillStatus.ACCEPTED)
                .map(ExportBill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExposure = lcExposure.add(bgExposure).add(billExposure);

        // Facility Limits & Utilisation
        BigDecimal totalLimit = facilities.stream()
                .filter(f -> f.getStatus() == CreditFacilityStatus.ACTIVE)
                .map(CreditFacility::getLimitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUtilized = facilities.stream()
                .filter(f -> f.getStatus() == CreditFacilityStatus.ACTIVE)
                .map(CreditFacility::getUtilizedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal utilizationRate = totalLimit.compareTo(BigDecimal.ZERO) > 0
                ? totalUtilized.multiply(new BigDecimal("100")).divide(totalLimit, 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        // Counts
        long activeLcsCount = lcs.stream().filter(lc -> lc.getStatus() == LetterOfCreditStatus.ACTIVE).count();
        long activeBgsCount = bgs.stream().filter(bg -> bg.getStatus() == BankGuaranteeStatus.ACTIVE).count();
        long activeBillsCount = bills.stream().filter(bill -> bill.getStatus() != ExportBillStatus.PAID).count();

        summary.put("totalExposure", totalExposure);
        summary.put("lcExposure", lcExposure);
        summary.put("bgExposure", bgExposure);
        summary.put("billExposure", billExposure);
        summary.put("totalLimit", totalLimit);
        summary.put("totalUtilized", totalUtilized);
        summary.put("utilizationRate", utilizationRate);
        summary.put("activeLcsCount", activeLcsCount);
        summary.put("activeBgsCount", activeBgsCount);
        summary.put("activeBillsCount", activeBillsCount);
        summary.put("totalScreenings", 0L);
        summary.put("openComplianceCases", 0L);

        logger.info("Relationship manager analytics summary generated for rmId={}: totalExposure={}, utilizationRate={}%",
                rmId, totalExposure, utilizationRate);
        return summary;
    }
}
