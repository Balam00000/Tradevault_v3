package com.tradevault.repository;

import com.tradevault.entity.ExportBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExportBillRepository extends JpaRepository<ExportBill, Long> {
    List<ExportBill> findByClientId(Long clientId);
    Optional<ExportBill> findByBillNumber(String billNumber);
    List<ExportBill> findAllByOrderByCreatedAtDesc();
    List<ExportBill> findByClientRelationshipManagerId(Long relationshipManagerId);
}
