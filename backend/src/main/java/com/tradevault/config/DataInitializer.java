package com.tradevault.config;

import com.tradevault.entity.User;
import com.tradevault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * DataInitializer runs on every application startup.
 * It ensures the default seed users always exist with a correctly
 * BCrypt-encoded password — avoids hash corruption issues that occur
 * when hardcoded SQL hash strings are copy-pasted or stored via
 * MySQL Workbench across different systems.
 *
 * Default password for ALL seed users: "password"
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEFAULT_PASSWORD = "password";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        log.info("DataInitializer: Checking seed users...");

        seedUser("client",       "client@tradevault.com",       "Acme Corp Corporate User",               "CLIENT");
        seedUser("ops",          "ops@tradevault.com",          "Sarah Jenkins (Trade Operations)",        "OPERATIONS");
        seedUser("relationship", "rm@tradevault.com",           "David Miller (Relationship Manager)",     "RELATIONSHIP_MANAGER");
        seedUser("treasury",     "treasury@tradevault.com",     "Elena Rostova (Treasury Director)",       "TREASURY");
        seedUser("compliance",   "compliance@tradevault.com",   "Marcus Vance (Compliance Officer)",       "COMPLIANCE");
        seedUser("admin",        "admin@tradevault.com",        "System Admin",                            "ADMIN");

        log.info("DataInitializer: Seed user check complete.");
    }

    /**
     * Creates the user if they don't exist yet.
     * If the user DOES exist but has a corrupted/wrong password hash,
     * it resets the password to the default using a freshly generated BCrypt hash.
     */
    private void seedUser(String username, String email, String fullName, String role) {
        userRepository.findByUsername(username).ifPresentOrElse(
            existingUser -> {
                // Fix corrupted password: BCrypt hashes are always exactly 60 chars
                // and start with "$2a$" or "$2b$". If not, reset it.
                String storedHash = existingUser.getPassword();
                boolean isValidHash = storedHash != null
                        && storedHash.length() == 60
                        && storedHash.startsWith("$2");

                if (!isValidHash) {
                    log.warn("DataInitializer: Fixing corrupted password hash for user '{}'", username);
                    existingUser.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
                    userRepository.save(existingUser);
                } else {
                    log.debug("DataInitializer: User '{}' already exists with valid hash. Skipping.", username);
                }
            },
            () -> {
                // User does not exist — create them fresh
                User newUser = new User(
                        username,
                        passwordEncoder.encode(DEFAULT_PASSWORD),
                        email,
                        fullName,
                        role
                );
                newUser.setStatus("ACTIVE");
                userRepository.save(newUser);
                log.info("DataInitializer: Created seed user '{}'", username);
            }
        );
    }
}
