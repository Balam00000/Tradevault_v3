package com.tradevault.service;

import com.tradevault.entity.AuditLog;
import com.tradevault.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional
    public void log(Long userId, String username, String action, String details, String ipAddress) {
        logger.debug("Persisting audit log: userId={}, username='{}', action='{}', ip='{}'",
                userId, username, action, ipAddress);
        try {
            AuditLog auditLog = new AuditLog(userId, username, action, details, ipAddress);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log persisted successfully: action='{}', username='{}'", action, username);
        } catch (Exception e) {
            // Audit log failures must never interrupt core business operations
            logger.error("AUDIT LOG FAILURE — failed to persist audit entry: action='{}', username='{}', error='{}'",
                    action, username, e.getMessage(), e);
        }
    }

    public List<AuditLog> getAllLogs() {
        logger.debug("Fetching all audit logs ordered by timestamp");
        List<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc();
        logger.info("Retrieved {} audit log entries", logs.size());
        return logs;
    }
}
