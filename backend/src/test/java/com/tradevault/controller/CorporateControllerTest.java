package com.tradevault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradevault.dto.LoginRequest;
import com.tradevault.entity.*;
import com.tradevault.repository.*;
import com.tradevault.entity.enums.*;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link CorporateController} using MockMvc + H2.
 * Tests role-based access control and CRUD operations for clients and facilities.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CorporateController Integration Tests")
@Transactional
class CorporateControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CorporateClientRepository clientRepository;
    @Autowired private CreditFacilityRepository facilityRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String clientToken;
    private String operationsToken;
    private CorporateClient seededClient;

    @BeforeEach
    void setUp() throws Exception {
        adminToken      = loginAs("adminusr2", "ADMIN", "ACTIVE");
        operationsToken = loginAs("opsusr2", "OPERATIONS", "ACTIVE");

        // Seed a corporate client for client-role user
        seededClient = new CorporateClient();
        seededClient.setCompanyName("Test Corp Ltd");
        seededClient.setCountry("GB");
        seededClient.setTaxId("GB" + System.currentTimeMillis()); // unique per test
        seededClient.setCreditLimit(new BigDecimal("2000000"));
        seededClient.setStatus(CorporateClientStatus.ACTIVE);
        seededClient = clientRepository.save(seededClient);

        clientToken = loginAsClientUser("clientusr2", seededClient);
    }

    private String loginAs(String username, String role, String status) throws Exception {
        if (!userRepository.existsByUsername(username)) {
            User user = new User(username, passwordEncoder.encode("password"), username + "@test.com", "Test " + username, role);
            user.setStatus(UserStatus.valueOf(status.toUpperCase()));
            userRepository.save(user);
        }
        return getToken(username, "password");
    }

    private String loginAsClientUser(String username, CorporateClient client) throws Exception {
        User user;
        if (userRepository.existsByUsername(username)) {
            user = userRepository.findByUsername(username).orElseThrow();
        } else {
            user = new User(username, passwordEncoder.encode("password"), username + "@test.com", "Client " + username, "CLIENT");
            user.setStatus(UserStatus.ACTIVE);
        }
        user.setCorporateClient(client);
        userRepository.save(user);
        return getToken(username, "password");
    }

    private String getToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("token").asText();
    }

    // ─── GET /corporates ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /corporates: ADMIN should see all clients")
    void getAllClients_adminUser_returnsAllClients() throws Exception {
        mockMvc.perform(get("/corporates")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /corporates: OPERATIONS user should see all clients")
    void getAllClients_operationsUser_returnsClients() throws Exception {
        mockMvc.perform(get("/corporates")
                        .header("Authorization", "Bearer " + operationsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /corporates: CLIENT user should only see their own client")
    void getAllClients_clientUser_returnsOwnClientOnly() throws Exception {
        MvcResult result = mockMvc.perform(get("/corporates")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(seededClient.getCompanyName());
    }

    @Test
    @DisplayName("GET /corporates: should return 403 without authentication")
    void getAllClients_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/corporates"))
                .andExpect(status().isForbidden());
    }

    // ─── POST /corporates — Create Client ─────────────────────────────────────

    @Test
    @DisplayName("POST /corporates: ADMIN should create client successfully")
    void createClient_adminUser_success() throws Exception {
        CorporateClient newClient = new CorporateClient();
        newClient.setCompanyName("New Trading Co");
        newClient.setCountry("US");
        newClient.setTaxId("US987654");
        newClient.setCreditLimit(new BigDecimal("1000000"));
        newClient.setStatus(CorporateClientStatus.ACTIVE);

        mockMvc.perform(post("/corporates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newClient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyName").value("New Trading Co"));
    }

    @Test
    @DisplayName("POST /corporates: OPERATIONS user should be denied access")
    void createClient_operationsUser_returns403() throws Exception {
        CorporateClient newClient = new CorporateClient();
        newClient.setCompanyName("Unauthorized Co");
        newClient.setCountry("US");

        mockMvc.perform(post("/corporates")
                        .header("Authorization", "Bearer " + operationsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newClient)))
                .andExpect(status().isForbidden());
    }

    // ─── GET /corporates/facilities ───────────────────────────────────────────

    @Test
    @DisplayName("GET /corporates/facilities: ADMIN should see all facilities")
    void getAllFacilities_adminUser_returnsAll() throws Exception {
        // Seed a facility
        CreditFacility facility = new CreditFacility();
        facility.setClient(seededClient);
        facility.setFacilityType(CreditFacilityType.LETTER_OF_CREDIT_FACILITY);
        facility.setLimitAmount(new BigDecimal("500000"));
        facility.setUtilizedAmount(BigDecimal.ZERO);
        facility.setCurrency("USD");
        facility.setExpiryDate(LocalDate.now().plusYears(1));
        facility.setStatus(CreditFacilityStatus.ACTIVE);
        facilityRepository.save(facility);

        mockMvc.perform(get("/corporates/facilities")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /corporates/facilities: ADMIN should create facility with zero initial utilization")
    void createFacility_adminUser_success() throws Exception {
        CreditFacility facility = new CreditFacility();
        facility.setFacilityType(CreditFacilityType.GUARANTEE_FACILITY);
        facility.setLimitAmount(new BigDecimal("750000"));
        facility.setCurrency("USD");
        facility.setExpiryDate(LocalDate.now().plusYears(2));
        facility.setStatus(CreditFacilityStatus.ACTIVE);

        mockMvc.perform(post("/corporates/facilities?clientId=" + seededClient.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(facility)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.facilityType").value("GUARANTEE_FACILITY"))
                .andExpect(jsonPath("$.data.utilizedAmount").value(0));
    }
}
