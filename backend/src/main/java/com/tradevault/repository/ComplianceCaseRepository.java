package com.tradevault.repository;

import com.tradevault.entity.ComplianceCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tradevault.entity.enums.ComplianceCaseStatus;
import java.util.List;

@Repository
public interface ComplianceCaseRepository extends JpaRepository<ComplianceCase, Long> {
    List<ComplianceCase> findByCaseStatus(ComplianceCaseStatus caseStatus);
    List<ComplianceCase> findByAssignedTo(String assignedTo);
    List<ComplianceCase> findAllByOrderByCreatedAtDesc();
    long countByCaseStatus(ComplianceCaseStatus caseStatus);
}
