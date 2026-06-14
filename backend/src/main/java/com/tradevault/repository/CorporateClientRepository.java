package com.tradevault.repository;

import com.tradevault.entity.CorporateClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CorporateClientRepository extends JpaRepository<CorporateClient, Long> {
    Optional<CorporateClient> findByTaxId(String taxId);
    List<CorporateClient> findByRelationshipManagerId(Long relationshipManagerId);
}
