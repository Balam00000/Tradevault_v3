package com.tradevault.service;

import com.tradevault.entity.BankGuarantee;
import com.tradevault.entity.LetterOfCredit;
import com.tradevault.repository.BankGuaranteeRepository;
import com.tradevault.repository.LetterOfCreditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import com.tradevault.entity.enums.LetterOfCreditStatus;
import com.tradevault.entity.enums.BankGuaranteeStatus;

/**
 * ExpiryAlertScheduler — runs daily scheduled jobs to detect instruments
 * approaching expiry and sends in-app notifications to clients and operations staff.
 *
 * Schedule:
 *   - LCs: alerts when expiry is within 7 calendar days
 *   - BGs: alerts when expiry is within 14 calendar days
 *   - Runs daily at 08:00 UTC
 */
@Component
public class ExpiryAlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ExpiryAlertScheduler.class);

    // Alert thresholds (days before expiry)
    private static final int LC_ALERT_DAYS  = 7;
    private static final int BG_ALERT_DAYS  = 14;

    @Autowired
    private LetterOfCreditRepository lcRepository;

    @Autowired
    private BankGuaranteeRepository bgRepository;

    @Autowired
    private NotificationService notificationService;

    // ─── LC Expiry Alert Job ──────────────────────────────────────────────────

    /**
     * Runs every day at 08:00 UTC.
     * Scans all ACTIVE / AMENDED LCs expiring within 7 days and notifies the client.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkLcExpiry() {
        logger.info("ExpiryAlertScheduler: Starting LC expiry check (threshold={} days)", LC_ALERT_DAYS);
        LocalDate alertCutoff = LocalDate.now().plusDays(LC_ALERT_DAYS);

        try {
            List<LetterOfCredit> expiringActiveLCs   = lcRepository.findByStatusAndExpiryDateBefore(LetterOfCreditStatus.ACTIVE,  alertCutoff);
            List<LetterOfCredit> expiringAmendedLCs  = lcRepository.findByStatusAndExpiryDateBefore(LetterOfCreditStatus.AMENDED, alertCutoff);

            int totalAlerted = 0;

            for (LetterOfCredit lc : expiringActiveLCs) {
                sendLcExpiryAlert(lc);
                totalAlerted++;
            }
            for (LetterOfCredit lc : expiringAmendedLCs) {
                sendLcExpiryAlert(lc);
                totalAlerted++;
            }

            logger.info("ExpiryAlertScheduler: LC expiry check complete — {} LCs alerted (expiring before {})",
                    totalAlerted, alertCutoff);
        } catch (Exception e) {
            logger.error("ExpiryAlertScheduler: LC expiry job failed unexpectedly: {}", e.getMessage(), e);
        }
    }

    private void sendLcExpiryAlert(LetterOfCredit lc) {
        long daysLeft = LocalDate.now().until(lc.getExpiryDate()).getDays();
        String title = "⚠️ LC Expiry Warning — " + lc.getLcNumber();
        String message = String.format(
                "Your Letter of Credit %s (Amount: %s %s) is expiring in %d day(s) on %s. Please take action.",
                lc.getLcNumber(), lc.getAmount(), lc.getCurrency(), daysLeft, lc.getExpiryDate());
        try {
            Long userId = lc.getClient().getId();
            notificationService.sendNotification(userId, title, message, "LC");
            logger.warn("LC expiry alert sent: lcNumber='{}', clientId={}, daysLeft={}, expiryDate={}",
                    lc.getLcNumber(), userId, daysLeft, lc.getExpiryDate());
        } catch (Exception e) {
            logger.error("Failed to send LC expiry alert for lcNumber='{}': {}", lc.getLcNumber(), e.getMessage(), e);
        }
    }

    // ─── BG Expiry Alert Job ──────────────────────────────────────────────────

    /**
     * Runs every day at 08:00 UTC.
     * Scans all ACTIVE BGs expiring within 14 days and notifies the client.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkBgExpiry() {
        logger.info("ExpiryAlertScheduler: Starting BG expiry check (threshold={} days)", BG_ALERT_DAYS);
        LocalDate alertCutoff = LocalDate.now().plusDays(BG_ALERT_DAYS);

        try {
            List<BankGuarantee> expiringBGs = bgRepository.findByStatusAndExpiryDateBefore(BankGuaranteeStatus.ACTIVE, alertCutoff);
            int totalAlerted = 0;

            for (BankGuarantee bg : expiringBGs) {
                long daysLeft = LocalDate.now().until(bg.getExpiryDate()).getDays();
                String title = "⚠️ BG Expiry Warning — " + bg.getBgNumber();
                String message = String.format(
                        "Your Bank Guarantee %s (Amount: %s %s) is expiring in %d day(s) on %s. Please take action.",
                        bg.getBgNumber(), bg.getAmount(), bg.getCurrency(), daysLeft, bg.getExpiryDate());
                try {
                    Long userId = bg.getClient().getId();
                    notificationService.sendNotification(userId, title, message, "BankGuarantee");
                    logger.warn("BG expiry alert sent: bgNumber='{}', clientId={}, daysLeft={}, expiryDate={}",
                            bg.getBgNumber(), userId, daysLeft, bg.getExpiryDate());
                    totalAlerted++;
                } catch (Exception e) {
                    logger.error("Failed to send BG expiry alert for bgNumber='{}': {}", bg.getBgNumber(), e.getMessage(), e);
                }
            }

            logger.info("ExpiryAlertScheduler: BG expiry check complete — {} BGs alerted (expiring before {})",
                    totalAlerted, alertCutoff);
        } catch (Exception e) {
            logger.error("ExpiryAlertScheduler: BG expiry job failed unexpectedly: {}", e.getMessage(), e);
        }
    }
}
