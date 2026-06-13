package com.tradevault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradevault.dto.LoginRequest;
import com.tradevault.dto.RegisterRequest;
import com.tradevault.entity.User;
import com.tradevault.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import com.tradevault.entity.enums.UserStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AuthController} using MockMvc + H2 in-memory database.
 * These tests exercise the full Spring Security filter chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─── Setup: seed a test user ───────────────────────────────────────────────

    private void createUser(String username, String password, String role, String status) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User(username, passwordEncoder.encode(password), username + "@test.com", "Test " + username, role);
            user.setStatus(UserStatus.valueOf(status.toUpperCase()));
            userRepository.save(user);
        }
    }

    // ─── POST /auth/register ──────────────────────────────────────────────────

    @Test
    @DisplayName("register: should return 200 and success message for valid registration")
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser_" + System.currentTimeMillis());
        request.setPassword("password123");
        request.setEmail("newuser_" + System.currentTimeMillis() + "@test.com");
        request.setFullName("New User");
        request.setRole("CLIENT");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    @DisplayName("register: should return 400 when username is already taken")
    void register_duplicateUsername_returnsBadRequest() throws Exception {
        createUser("dupuser", "password123", "CLIENT", "ACTIVE");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("dupuser");
        request.setPassword("password123");
        request.setEmail("different@test.com");
        request.setFullName("Dup User");
        request.setRole("CLIENT");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username is already taken!"));
    }

    @Test
    @DisplayName("register: should return 400 when email is already in use")
    void register_duplicateEmail_returnsBadRequest() throws Exception {
        createUser("uniqueuser1", "password123", "CLIENT", "ACTIVE");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("differentuser");
        request.setPassword("password123");
        request.setEmail("uniqueuser1@test.com"); // same email
        request.setFullName("Another User");
        request.setRole("CLIENT");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email Address already in use!"));
    }

    // ─── POST /auth/login ─────────────────────────────────────────────────────

    @Test
    @DisplayName("login: should return JWT token for valid ACTIVE credentials")
    void login_success_returnsJwt() throws Exception {
        createUser("activeuser", "password123", "OPERATIONS", "ACTIVE");

        LoginRequest request = new LoginRequest();
        request.setUsername("activeuser");
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("activeuser"))
                .andExpect(jsonPath("$.data.role").value("OPERATIONS"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("token");
    }

    @Test
    @DisplayName("login: should return 403 when user account is PENDING approval")
    void login_pendingUser_returns403() throws Exception {
        createUser("pendinguser", "password123", "CLIENT", "PENDING");

        LoginRequest request = new LoginRequest();
        request.setUsername("pendinguser");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Your registration is pending admin approval."));
    }

    @Test
    @DisplayName("login: should return 403 when user account is SUSPENDED")
    void login_suspendedUser_returns403() throws Exception {
        createUser("suspendeduser", "password123", "CLIENT", "SUSPENDED");

        LoginRequest request = new LoginRequest();
        request.setUsername("suspendeduser");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Your account has been suspended. Please contact the administrator."));
    }

    @Test
    @DisplayName("login: should return 401 for invalid credentials")
    void login_wrongPassword_returns401() throws Exception {
        createUser("validuser", "correctpassword", "CLIENT", "ACTIVE");

        LoginRequest request = new LoginRequest();
        request.setUsername("validuser");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ─── GET /auth/me ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/me: should return profile for authenticated user")
    void getMe_authenticated_returnsProfile() throws Exception {
        createUser("meuser", "password123", "COMPLIANCE", "ACTIVE");

        // First login to get token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("meuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();
        String responseBody = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("data").get("token").asText();

        // Use token to call /auth/me
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("meuser"))
                .andExpect(jsonPath("$.data.role").value("COMPLIANCE"));
    }

    @Test
    @DisplayName("GET /auth/me: should return 403 without JWT token")
    void getMe_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isForbidden());
    }
}
