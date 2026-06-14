package com.tradevault.security;

import com.tradevault.entity.BankGuarantee;
import com.tradevault.entity.CorporateClient;
import com.tradevault.entity.LetterOfCredit;
import com.tradevault.entity.User;
import com.tradevault.repository.CorporateClientRepository;
import com.tradevault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public class TradeSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(TradeSecurityService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CorporateClientRepository corporateClientRepository;

    public void checkClientAccess(Long clientId, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null || !user.getCorporateClient().getId().equals(clientId)) {
                logger.warn("Client access denied: username='{}' attempted to access clientId={}", user.getUsername(), clientId);
                throw new AccessDeniedException("You do not have permission to access this client's data");
            }
        } else if ("RELATIONSHIP_MANAGER".equals(user.getRole())) {
            CorporateClient client = corporateClientRepository.findById(clientId).orElseThrow();
            if (client.getRelationshipManagerId() == null || !client.getRelationshipManagerId().equals(user.getId())) {
                logger.warn("Client access denied: RM username='{}' attempted to access non-assigned clientId={}", user.getUsername(), clientId);
                throw new AccessDeniedException("You do not have permission to access this client's data (not assigned to you)");
            }
        }
    }

    public void checkLcAccess(LetterOfCredit lc, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null || !lc.getClient().getId().equals(user.getCorporateClient().getId())) {
                logger.warn("LC access denied: username='{}' attempted to access lcId={}", user.getUsername(), lc.getId());
                throw new AccessDeniedException("You do not have permission to access this Letter of Credit");
            }
        } else if ("RELATIONSHIP_MANAGER".equals(user.getRole())) {
            if (lc.getClient().getRelationshipManagerId() == null || !lc.getClient().getRelationshipManagerId().equals(user.getId())) {
                logger.warn("LC access denied: RM username='{}' attempted to access non-assigned lcId={}", user.getUsername(), lc.getId());
                throw new AccessDeniedException("You do not have permission to access this Letter of Credit (not assigned to you)");
            }
        }
    }

    public void checkBgAccess(BankGuarantee bg, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null || !bg.getClient().getId().equals(user.getCorporateClient().getId())) {
                logger.warn("BG access denied: username='{}' attempted to access bgId={}", user.getUsername(), bg.getId());
                throw new AccessDeniedException("You do not have permission to access this Bank Guarantee");
            }
        } else if ("RELATIONSHIP_MANAGER".equals(user.getRole())) {
            if (bg.getClient().getRelationshipManagerId() == null || !bg.getClient().getRelationshipManagerId().equals(user.getId())) {
                logger.warn("BG access denied: RM username='{}' attempted to access non-assigned bgId={}", user.getUsername(), bg.getId());
                throw new AccessDeniedException("You do not have permission to access this Bank Guarantee (not assigned to you)");
            }
        }
    }

    public void verifyUserAccess(Long userId, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if (!user.getId().equals(userId)) {
            logger.warn("Notification access denied: username='{}' (userId={}) attempted to access notifications of userId={}",
                    user.getUsername(), user.getId(), userId);
            throw new AccessDeniedException("You do not have permission to access these notifications");
        }
    }
}
