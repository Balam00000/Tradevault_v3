package com.tradevault.service;

import com.tradevault.entity.AuditLog;
import java.util.List;

public interface AuditLogService {
    void log(Long userId, String username, String action, String details, String ipAddress);
    List<AuditLog> getAllLogs();
}
