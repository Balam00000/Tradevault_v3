package com.tradevault.service;

import com.tradevault.entity.BankGuarantee;
import com.tradevault.entity.BGClaim;
import com.tradevault.entity.enums.BankGuaranteeStatus;
import java.math.BigDecimal;
import java.util.List;

public interface BankGuaranteeService {
    List<BankGuarantee> getAllBGs();
    List<BankGuarantee> getBGsByClientId(Long clientId);
    List<BankGuarantee> getBGsByRelationshipManagerId(Long rmId);
    BankGuarantee getBGById(Long id);
    BankGuarantee createBG(BankGuarantee bg, Long clientId, Long facilityId, String username);
    BankGuarantee updateStatus(Long id, BankGuaranteeStatus status, String username);
    BankGuarantee submitForApproval(Long id, String username);
    BGClaim fileClaim(Long bgId, BigDecimal amount, String paymentDetails, String username);
    BGClaim processClaim(Long claimId, String statusStr, String username);
    List<BGClaim> getClaims(Long bgId);
}
