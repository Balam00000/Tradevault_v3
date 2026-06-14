package com.tradevault.repository;

import com.tradevault.entity.LetterOfCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.tradevault.entity.enums.LetterOfCreditStatus;

@Repository
public interface LetterOfCreditRepository extends JpaRepository<LetterOfCredit, Long> {
    List<LetterOfCredit> findByClientId(Long clientId);
    Optional<LetterOfCredit> findByLcNumber(String lcNumber);
    List<LetterOfCredit> findAllByOrderByCreatedAtDesc();
    List<LetterOfCredit> findByStatusAndExpiryDateBefore(LetterOfCreditStatus status, LocalDate date);
    List<LetterOfCredit> findByClientRelationshipManagerId(Long relationshipManagerId);
}
