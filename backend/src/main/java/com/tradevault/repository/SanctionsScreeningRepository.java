package com.tradevault.repository;

import com.tradevault.entity.SanctionsScreening;
import com.tradevault.entity.enums.SanctionsScreeningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SanctionsScreeningRepository extends JpaRepository<SanctionsScreening, Long> {
    List<SanctionsScreening> findByTransactionId(String transactionId);
    List<SanctionsScreening> findByStatus(SanctionsScreeningStatus status);
    List<SanctionsScreening> findAllByOrderByScreenedAtDesc();
    List<SanctionsScreening> findByTransactionIdAndStatus(String transactionId, SanctionsScreeningStatus status);
}
