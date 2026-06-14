package com.tradevault.repository;

import com.tradevault.entity.CollectionInstruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionInstructionRepository extends JpaRepository<CollectionInstruction, Long> {
    List<CollectionInstruction> findByClientId(Long clientId);
    Optional<CollectionInstruction> findByInstructionRef(String instructionRef);
    List<CollectionInstruction> findAllByOrderByCreatedAtDesc();
    List<CollectionInstruction> findByClientRelationshipManagerId(Long relationshipManagerId);
}
