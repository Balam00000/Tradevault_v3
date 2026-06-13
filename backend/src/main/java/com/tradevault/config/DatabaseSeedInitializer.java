package com.tradevault.config;

import com.tradevault.entity.CorporateClient;
import com.tradevault.entity.User;
import com.tradevault.entity.enums.UserStatus;
import com.tradevault.repository.CorporateClientRepository;
import com.tradevault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class DatabaseSeedInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeedInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CorporateClientRepository corporateClientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing database user seeds and authority state checks...");

        Map<String, String> defaultUsers = new HashMap<>();
        defaultUsers.put("client", "CLIENT");
        defaultUsers.put("ops", "OPERATIONS");
        defaultUsers.put("relationship", "RELATIONSHIP_MANAGER");
        defaultUsers.put("treasury", "TREASURY");
        defaultUsers.put("compliance", "COMPLIANCE");
        defaultUsers.put("admin", "ADMIN");

        for (Map.Entry<String, String> entry : defaultUsers.entrySet()) {
            String username = entry.getKey();
            String role = entry.getValue();

            Optional<User> optUser = userRepository.findByUsername(username);
            User user;
            if (optUser.isPresent()) {
                user = optUser.get();
                logger.info("Updating existing seed user: {}, ensuring active status and reset password", username);
            } else {
                logger.info("Creating missing seed user: {}", username);
                user = new User();
                user.setUsername(username);
                user.setEmail(username + "@tradevault.com");
                user.setFullName(username.substring(0, 1).toUpperCase() + username.substring(1) + " User");
            }

            user.setRole(role);
            user.setStatus(UserStatus.ACTIVE);
            user.setPassword(passwordEncoder.encode("password"));

            // Map 'client' user to Corporate Client 1 (Acme)
            if ("client".equals(username)) {
                Optional<CorporateClient> clientOpt = corporateClientRepository.findById(1L);
                if (clientOpt.isPresent()) {
                    user.setCorporateClient(clientOpt.get());
                    logger.info("Associated user 'client' with CorporateClient: {}", clientOpt.get().getCompanyName());
                } else {
                    logger.warn("CorporateClient 1 not found. Make sure data.sql inserts it.");
                }
            }

            userRepository.save(user);
        }

        logger.info("Database user seeds and authority state checked successfully.");
    }
}
