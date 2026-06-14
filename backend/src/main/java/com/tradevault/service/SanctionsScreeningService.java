package com.tradevault.service;

import com.tradevault.entity.ComplianceCase;
import com.tradevault.entity.SanctionsScreening;
import java.util.List;

public interface SanctionsScreeningService {
    SanctionsScreening screenEntity(String entityName, String entityType, String txType, String txId);
    List<SanctionsScreening> getAllScreenings();
    List<ComplianceCase> getAllCases();
    ComplianceCase resolveCase(Long caseId, String statusStr, String notes, String resolver);
}
