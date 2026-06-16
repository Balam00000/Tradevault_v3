package com.tradevault.service;

import com.tradevault.entity.AuditLog;
import com.tradevault.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    void log_success() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> auditLogService.log(1L, "testuser", "LOGIN", "User logged in", "127.0.0.1"));

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void log_failure_suppressed() {
        // Force an exception during save
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("Database down"));

        // The method should catch the exception internally and NOT throw it to the caller
        assertDoesNotThrow(() -> auditLogService.log(1L, "testuser", "LOGIN", "User logged in", "127.0.0.1"));

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void getAllLogs_success() {
        AuditLog log = new AuditLog(1L, "testuser", "LOGIN", "User logged in", "127.0.0.1");
        when(auditLogRepository.findAllByOrderByTimestampDesc()).thenReturn(Collections.singletonList(log));

        List<AuditLog> logs = auditLogService.getAllLogs();

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("testuser", logs.get(0).getUsername());
        verify(auditLogRepository).findAllByOrderByTimestampDesc();
    }
}
