package com.tradevault.service;

import com.tradevault.entity.AuditLog;
import com.tradevault.repository.AuditLogRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuditLogService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Unit Tests")
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private AuditLogServiceImpl auditLogService;

    @Test
    @DisplayName("log: should persist audit entry with correct fields")
    void log_persistsAuditEntry() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> {
            AuditLog log = inv.getArgument(0);
            log.setId(1L);
            return log;
        });

        // Should not throw
        assertThatCode(() -> auditLogService.log(10L, "testuser", "USER_LOGIN", "Logged in via API", "192.168.1.1"))
                .doesNotThrowAnyException();

        verify(auditLogRepository).save(argThat(log ->
                "testuser".equals(log.getUsername()) &&
                "USER_LOGIN".equals(log.getAction()) &&
                "Logged in via API".equals(log.getDetails())
        ));
    }

    @Test
    @DisplayName("log: should not propagate exception when repository fails (fault tolerance)")
    void log_repositoryFailure_doesNotPropagate() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB failure"));

        // Audit log failures must never disrupt business operations
        assertThatCode(() -> auditLogService.log(1L, "testuser", "ACTION", "details", null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getAllLogs: should return all logs ordered by timestamp")
    void getAllLogs_returnsOrderedLogs() {
        AuditLog log1 = new AuditLog(); log1.setId(1L);
        AuditLog log2 = new AuditLog(); log2.setId(2L);
        when(auditLogRepository.findAllByOrderByTimestampDesc()).thenReturn(List.of(log1, log2));

        List<AuditLog> result = auditLogService.getAllLogs();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("log: should handle null userId gracefully")
    void log_nullUserId_doesNotThrow() {
        when(auditLogRepository.save(any())).thenReturn(new AuditLog());
        assertThatCode(() -> auditLogService.log(null, "system", "SCHEDULED_JOB", "Ran expiry check", null))
                .doesNotThrowAnyException();
    }
}
