package com.tradevault.service;

import com.tradevault.entity.LetterOfCredit;
import com.tradevault.entity.LCAmendment;
import com.tradevault.entity.LCDrawing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LetterOfCreditService {
    List<LetterOfCredit> getAllLCs();
    List<LetterOfCredit> getLCsByClientId(Long clientId);
    List<LetterOfCredit> getLCsByRelationshipManagerId(Long rmId);
    LetterOfCredit getLCById(Long id);
    LetterOfCredit createLC(LetterOfCredit lc, Long clientId, Long facilityId, String username);
    LetterOfCredit updateStatus(Long id, com.tradevault.entity.enums.LetterOfCreditStatus status, String username);
    LCAmendment requestAmendment(Long lcId, BigDecimal newAmount, LocalDate newExpiryDate, String justification, String username);
    LCAmendment processAmendment(Long amendmentId, String status, String username);
    LCDrawing presentDrawing(Long lcId, BigDecimal amount, String documents, String username);
    LCDrawing processDrawing(Long drawingId, String status, String discrepancyNotes, String username);
    List<LCAmendment> getAmendments(Long lcId);
    List<LCDrawing> getDrawings(Long lcId);
}
