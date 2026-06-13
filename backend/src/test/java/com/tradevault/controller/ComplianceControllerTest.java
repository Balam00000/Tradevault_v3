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
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link ComplianceController} using MockMvc + H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ComplianceController Integration Tests")
@Transactional
class ComplianceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private SanctionsScreeningRepository screeningRepository;
    @Autowired private ComplianceCaseRepository caseRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String complianceToken;
    private String operationsToken;

    // ─── Setup ────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        complianceToken = loginAs("complianceofficer", "COMPLIANCE", "ACTIVE");
        operationsToken = loginAs("opsofficer", "OPERATIONS", "ACTIVE");
    }

    private String loginAs(String username, String role, String status) throws Exception {
        if (!userRepository.existsByUsername(username)) {
            User user = new User(username, passwordEncoder.encode("password"), username + "@test.com", "Test " + username, role);
            user.setStatus(UserStatus.valueOf(status.toUpperCase()));
            userRepository.save(user);
        }
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword("password");
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("token").asText();
    }

    // ─── GET /compliance/screenings ───────────────────────────────────────────

    @Test
    @DisplayName("GET /compliance/screenings: should return 200 for COMPLIANCE user")
    void getScreenings_complianceUser_returns200() throws Exception {
        mockMvc.perform(get("/compliance/screenings")
                        .header("Authorization", "Bearer " + complianceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /compliance/screenings: should return 403 for OPERATIONS user")
    void getScreenings_operationsUser_returns403() throws Exception {
        mockMvc.perform(get("/compliance/screenings")
                        .header("Authorization", "Bearer " + operationsToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /compliance/screenings: should return 403 without token")
    void getScreenings_noToken_returns403() throws Exception {
        mockMvc.perform(get("/compliance/screenings"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /compliance/cases ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /compliance/cases: should return 200 and existing cases for COMPLIANCE user")
    void getCases_complianceUser_returns200() throws Exception {
        // Seed a screening and case
        SanctionsScreening screening = new SanctionsScreening();
        screening.setEntityName("Iran State Corp");
        screening.setEntityType(ScreeningEntityType.APPLICANT);
        screening.setTransactionType("LC");
        screening.setTransactionId("LC-TEST-001");
        screening.setMatchScore(new BigDecimal("89.50"));
        screening.setWatchlistSource("OFAC_SDN");
        screening.setStatus(SanctionsScreeningStatus.FLAGGED);
        screening.setComplianceNotes("Test flagged screening");
        screening = screeningRepository.save(screening);

        ComplianceCase compCase = new ComplianceCase();
        compCase.setScreening(screening);
        compCase.setCaseStatus(ComplianceCaseStatus.OPEN);
        compCase.setAssignedTo("complianceofficer");
        compCase.setResolutionNotes("Pending review");
        caseRepository.save(compCase);

        mockMvc.perform(get("/compliance/cases")
                        .header("Authorization", "Bearer " + complianceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ─── PUT /compliance/cases/{id}/resolve ───────────────────────────────────

    @Test
    @DisplayName("PUT /compliance/cases/{id}/resolve: should resolve case for COMPLIANCE user")
    void resolveCase_complianceUser_success() throws Exception {
        SanctionsScreening screening = new SanctionsScreening();
        screening.setEntityName("Safe Corp Ltd");
        screening.setEntityType(ScreeningEntityType.BENEFICIARY);
        screening.setTransactionType("BG");
        screening.setTransactionId("BG-TEST-001");
        screening.setMatchScore(new BigDecimal("89.50"));
        screening.setWatchlistSource("OFAC_SDN");
        screening.setStatus(SanctionsScreeningStatus.FLAGGED);
        screening.setComplianceNotes("Test case to resolve");
        screening = screeningRepository.save(screening);

        ComplianceCase compCase = new ComplianceCase();
        compCase.setScreening(screening);
        compCase.setCaseStatus(ComplianceCaseStatus.OPEN);
        compCase.setAssignedTo("complianceofficer");
        compCase.setResolutionNotes("Pending");
        compCase = caseRepository.save(compCase);

        Map<String, String> payload = Map.of(
                "status", "RESOLVED_CLEARED",
                "notes", "False positive — verified by external authority"
        );

        mockMvc.perform(put("/compliance/cases/" + compCase.getId() + "/resolve")
                        .header("Authorization", "Bearer " + complianceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseStatus").value("RESOLVED_CLEARED"));
    }

    @Test
    @DisplayName("PUT /compliance/cases/{id}/resolve: should return 403 for OPERATIONS user")
    void resolveCase_operationsUser_returns403() throws Exception {
        Map<String, String> payload = Map.of("status", "RESOLVED_CLEARED", "notes", "test");
        mockMvc.perform(put("/compliance/cases/1/resolve")
                        .header("Authorization", "Bearer " + operationsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }
}
