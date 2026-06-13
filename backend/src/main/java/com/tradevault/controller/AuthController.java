package com.tradevault.controller;

import com.tradevault.config.JwtTokenProvider;
import com.tradevault.dto.*;
import com.tradevault.entity.User;
import com.tradevault.entity.enums.UserStatus;
import com.tradevault.repository.UserRepository;
import com.tradevault.service.AuditLogService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for username='{}'", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        User user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow();

        if (user.getStatus() == UserStatus.PENDING) {
            logger.warn("Login blocked for PENDING user='{}' — awaiting admin approval", loginRequest.getUsername());
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Your registration is pending admin approval."));
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            logger.warn("Login blocked for SUSPENDED user='{}'", loginRequest.getUsername());
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Your account has been suspended. Please contact the administrator."));
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        Long clientId = user.getCorporateClient() != null ? user.getCorporateClient().getId() : null;

        AuthResponse authResponse = new AuthResponse(
                jwt,
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getEmail(),
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getId(),
                clientId
        );

        auditLogService.log(user.getId(), user.getUsername(), "USER_LOGIN",
                "User successfully logged in via REST API context", null);
        logger.info("Login successful for username='{}', role='{}', userId={}", user.getUsername(), user.getRole(), user.getId());

        return ResponseEntity.ok(ApiResponse.success("Authentication successful", authResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        logger.info("Registration attempt for username='{}', role='{}'", registerRequest.getUsername(), registerRequest.getRole());

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            logger.warn("Registration failed — username='{}' already taken", registerRequest.getUsername());
            return ResponseEntity.badRequest().body(ApiResponse.error("Username is already taken!"));
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            logger.warn("Registration failed — email='{}' already in use", registerRequest.getEmail());
            return ResponseEntity.badRequest().body(ApiResponse.error("Email Address already in use!"));
        }

        User user = new User(
                registerRequest.getUsername(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getEmail(),
                registerRequest.getFullName(),
                registerRequest.getRole()
        );

        userRepository.save(user);
        auditLogService.log(null, registerRequest.getUsername(), "USER_REGISTER",
                "Registered new user account with role: " + registerRequest.getRole(), null);
        logger.info("User registered successfully: username='{}', role='{}', status='PENDING'",
                registerRequest.getUsername(), registerRequest.getRole());

        return ResponseEntity.ok(ApiResponse.success("User registered successfully"));
    }

    /**
     * GET /auth/me — returns the currently authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            logger.warn("Unauthenticated access attempt to /auth/me");
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied. Please authenticate."));
        }
        String username = authentication.getName();
        logger.debug("Profile fetch requested for username='{}'", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Authenticated user not found in DB: username='{}'", username);
                    return new RuntimeException("User not found");
                });

        Long clientId = user.getCorporateClient() != null ? user.getCorporateClient().getId() : null;

        AuthResponse profile = new AuthResponse(
                null,
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getEmail(),
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getId(),
                clientId
        );
        logger.debug("Profile fetched for username='{}', role='{}'", user.getUsername(), user.getRole());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", profile));
    }

    /**
     * PUT /auth/profile — updates the authenticated user's username, email, and/or password.
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AuthResponse>> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest req) {

        String currentUsername = authentication.getName();
        logger.info("Profile update requested for username='{}'", currentUsername);

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> {
                    logger.error("User not found during profile update: username='{}'", currentUsername);
                    return new RuntimeException("User not found");
                });

        // Username change
        if (req.getNewUsername() != null && !req.getNewUsername().isBlank()) {
            String newUname = req.getNewUsername().trim();
            if (!newUname.equals(user.getUsername())) {
                if (userRepository.existsByUsername(newUname)) {
                    logger.warn("Username change rejected — '{}' already taken, requested by '{}'", newUname, currentUsername);
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Username '" + newUname + "' is already taken."));
                }
                logger.info("Username changed from '{}' to '{}'", currentUsername, newUname);
                user.setUsername(newUname);
            }
        }

        // Email change
        if (req.getNewEmail() != null && !req.getNewEmail().isBlank()) {
            String newEmail = req.getNewEmail().trim();
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    logger.warn("Email change rejected — '{}' already in use, requested by '{}'", newEmail, currentUsername);
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Email '" + newEmail + "' is already in use."));
                }
                logger.info("Email updated for username='{}'", currentUsername);
                user.setEmail(newEmail);
            }
        }

        // Password change
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
                logger.warn("Password change rejected — current password not provided. username='{}'", currentUsername);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Current password is required to set a new password."));
            }
            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
                logger.warn("Password change rejected — incorrect current password. username='{}'", currentUsername);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Current password is incorrect."));
            }
            if (req.getNewPassword().length() < 6) {
                logger.warn("Password change rejected — new password too short. username='{}'", currentUsername);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("New password must be at least 6 characters."));
            }
            user.setPassword(passwordEncoder.encode(req.getNewPassword()));
            logger.info("Password updated for username='{}'", currentUsername);
        }

        userRepository.save(user);
        auditLogService.log(user.getId(), user.getUsername(), "PROFILE_UPDATE",
                "User updated their profile credentials", null);
        logger.info("Profile update completed for username='{}'", user.getUsername());

        String jwt = tokenProvider.generateToken(user.getUsername(), user.getRole());
        Long clientId = user.getCorporateClient() != null ? user.getCorporateClient().getId() : null;
        AuthResponse updated = new AuthResponse(
                jwt,
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getEmail(),
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getId(),
                clientId
        );
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }
}
