package com.tradevault.repository;

import com.tradevault.entity.CreditFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditFacilityRepository extends JpaRepository<CreditFacility, Long> {
    List<CreditFacility> findByClientId(Long clientId);
    List<CreditFacility> findByClientRelationshipManagerId(Long relationshipManagerId);
}
