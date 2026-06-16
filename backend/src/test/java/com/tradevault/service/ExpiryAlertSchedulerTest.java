package com.tradevault.service;

import com.tradevault.entity.BankGuarantee;
import com.tradevault.entity.CorporateClient;
import com.tradevault.entity.LetterOfCredit;
import com.tradevault.entity.enums.BankGuaranteeStatus;
import com.tradevault.entity.enums.LetterOfCreditStatus;
import com.tradevault.repository.BankGuaranteeRepository;
import com.tradevault.repository.LetterOfCreditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpiryAlertSchedulerTest {

    @Mock
    private LetterOfCreditRepository lcRepository;

    @Mock
    private BankGuaranteeRepository bgRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ExpiryAlertScheduler expiryAlertScheduler;

    private CorporateClient client;
    private LetterOfCredit lc;
    private BankGuarantee bg;

    @BeforeEach
    void setUp() {
        client = new CorporateClient();
        client.setId(1L);

        lc = new LetterOfCredit();
        lc.setClient(client);
        lc.setLcNumber("LC-12345");
        lc.setAmount(new BigDecimal("100000.00"));
        lc.setCurrency("USD");
        lc.setExpiryDate(LocalDate.now().plusDays(5));
        lc.setStatus(LetterOfCreditStatus.ACTIVE);

        bg = new BankGuarantee();
        bg.setClient(client);
        bg.setBgNumber("BG-12345");
        bg.setAmount(new BigDecimal("200000.00"));
        bg.setCurrency("EUR");
        bg.setExpiryDate(LocalDate.now().plusDays(10));
        bg.setStatus(BankGuaranteeStatus.ACTIVE);
    }

    @Test
    void checkLcExpiry_success() {
        // LC threshold is 7 days, so alertCutoff is now + 7 days
        when(lcRepository.findByStatusAndExpiryDateBefore(eq(LetterOfCreditStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(lc));
        when(lcRepository.findByStatusAndExpiryDateBefore(eq(LetterOfCreditStatus.AMENDED), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> expiryAlertScheduler.checkLcExpiry());

        verify(notificationService).sendNotification(
                eq(1L),
                eq("⚠️ LC Expiry Warning — LC-12345"),
                contains("is expiring in 5 day(s) on"),
                eq("LC")
        );
    }

    @Test
    void checkBgExpiry_success() {
        // BG threshold is 14 days, so alertCutoff is now + 14 days
        when(bgRepository.findByStatusAndExpiryDateBefore(eq(BankGuaranteeStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(bg));

        assertDoesNotThrow(() -> expiryAlertScheduler.checkBgExpiry());

        verify(notificationService).sendNotification(
                eq(1L),
                eq("⚠️ BG Expiry Warning — BG-12345"),
                contains("is expiring in 10 day(s) on"),
                eq("BankGuarantee")
        );
    }

    @Test
    void checkLcExpiry_failureNotificationSuppressed() {
        when(lcRepository.findByStatusAndExpiryDateBefore(eq(LetterOfCreditStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(lc));
        // Simulate a failure in notification service
        when(notificationService.sendNotification(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Notification system down"));

        // Scheduler should catch the exception and log it, rather than crash
        assertDoesNotThrow(() -> expiryAlertScheduler.checkLcExpiry());
    }
}
