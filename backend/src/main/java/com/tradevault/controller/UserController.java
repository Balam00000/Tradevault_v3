package com.tradevault.controller;

import com.tradevault.dto.ApiResponse;
import com.tradevault.dto.UserUpdateRequest;
import com.tradevault.entity.CorporateClient;
import com.tradevault.entity.User;
import com.tradevault.entity.enums.UserStatus;
import com.tradevault.exception.ResourceNotFoundException;
import com.tradevault.repository.CorporateClientRepository;
import com.tradevault.repository.UserRepository;
import com.tradevault.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CorporateClientRepository corporateClientRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers(Principal principal) {
        logger.debug("GetAllUsers requested by admin='{}'", principal != null ? principal.getName() : "system");
        List<User> users = userRepository.findAll();
        logger.info("Retrieved {} users for admin", users.size());
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request,
            Principal principal) {
        logger.info("UpdateUser request: userId={}, by admin='{}'", id, principal.getName());
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found for update: userId={}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });

        logger.debug("Updating user fields: username='{}', newRole='{}', newStatus='{}'",
                user.getUsername(), request.getRole(), request.getStatus());

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus() != null ? UserStatus.valueOf(request.getStatus().toUpperCase()) : null);

        if (request.getCorporateClientId() != null) {
            CorporateClient client = corporateClientRepository.findById(request.getCorporateClientId())
                    .orElseThrow(() -> {
                        logger.warn("Corporate Client not found for user association: clientId={}", request.getCorporateClientId());
                        return new ResourceNotFoundException("Corporate Client not found");
                    });
            user.setCorporateClient(client);
            logger.debug("User '{}' linked to corporateClientId={}", user.getUsername(), client.getId());
        } else {
            user.setCorporateClient(null);
            logger.debug("User '{}' unlinked from corporate client", user.getUsername());
        }

        User updated = userRepository.save(user);
        auditLogService.log(null, principal.getName(), "USER_UPDATE",
                "Updated user account details for username: " + updated.getUsername() + ", Role: " + updated.getRole() + ", Status: " + updated.getStatus(), null);
        logger.info("User updated successfully: userId={}, username='{}', role='{}', status='{}'",
                updated.getId(), updated.getUsername(), updated.getRole(), updated.getStatus());

        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id, Principal principal) {
        logger.info("DeleteUser request: userId={}, by admin='{}'", id, principal.getName());
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found for deletion: userId={}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });

        String username = user.getUsername();
        userRepository.delete(user);
        auditLogService.log(null, principal.getName(), "USER_DELETE",
                "Deleted user account: " + username, null);
        logger.info("User deleted: userId={}, username='{}', by admin='{}'", id, username, principal.getName());

        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
}
