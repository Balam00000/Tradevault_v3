package com.tradevault.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Postman Payloads Integration Tests")
@Transactional
class PostmanPayloadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private void createAdminUser() {
        if (!userRepository.existsByUsername("admin_postman_test")) {
            User user = new User("admin_postman_test", passwordEncoder.encode("password123"), "admin_postman_test@test.com", "Postman Admin", "ADMIN");
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
    }

    @Test
    @DisplayName("Test Postman Payloads End-to-End")
    void testPostmanPayloads() throws Exception {
        // 1. Seed Admin User
        createAdminUser();

        // 2. Perform Login with seeded Admin credentials
        String loginPayload = "{\n  \"username\": \"admin_postman_test\",\n  \"password\": \"password123\"\n}";
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseStr = loginResult.getResponse().getContentAsString();
        System.out.println("=== LOGIN RESPONSE ===\n" + loginResponseStr + "\n======================");

        JsonNode loginJson = objectMapper.readTree(loginResponseStr);
        String token = loginJson.get("data").get("token").asText();
        assertThat(token).isNotBlank();

        // 3. Create Corporate Client (using Postman payload format)
        String clientPayload = "{\n  \"companyName\": \"Global Trading Co\",\n  \"country\": \"US\",\n  \"taxId\": \"TAX-US-12345\",\n  \"creditLimit\": 5000000.00,\n  \"status\": \"ACTIVE\"\n}";
        MvcResult clientResult = mockMvc.perform(post("/corporates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientPayload))
                .andExpect(status().isOk())
                .andReturn();

        String clientResponseStr = clientResult.getResponse().getContentAsString();
        System.out.println("=== CREATE CLIENT RESPONSE ===\n" + clientResponseStr + "\n==============================");

        JsonNode clientJson = objectMapper.readTree(clientResponseStr);
        Long clientId = clientJson.get("data").get("id").asLong();
        assertThat(clientId).isNotNull();

        // 4. Create Credit Facility (using Postman payload format)
        String facilityPayload = "{\n  \"facilityType\": \"LETTER_OF_CREDIT_FACILITY\",\n  \"limitAmount\": 1500000.00,\n  \"currency\": \"USD\",\n  \"expiryDate\": \"2027-12-31\",\n  \"status\": \"ACTIVE\"\n}";
        MvcResult facilityResult = mockMvc.perform(post("/corporates/facilities")
                        .header("Authorization", "Bearer " + token)
                        .param("clientId", String.valueOf(clientId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facilityPayload))
                .andExpect(status().isOk())
                .andReturn();

        String facilityResponseStr = facilityResult.getResponse().getContentAsString();
        System.out.println("=== CREATE FACILITY RESPONSE ===\n" + facilityResponseStr + "\n=================================");

        JsonNode facilityJson = objectMapper.readTree(facilityResponseStr);
        Long facilityId = facilityJson.get("data").get("id").asLong();
        assertThat(facilityId).isNotNull();

        // 5. Create Letter of Credit (using Postman payload format)
        String lcPayload = "{\n  \"lcType\": \"SIGHT\",\n  \"amount\": 250000.00,\n  \"currency\": \"USD\",\n  \"applicantName\": \"Global Trading Co\",\n  \"beneficiaryName\": \"Shanghai Components Ltd\",\n  \"beneficiaryCountry\": \"CN\",\n  \"issueDate\": \"2026-06-14\",\n  \"expiryDate\": \"2026-12-14\",\n  \"tolerancePercentage\": 5.00,\n  \"portOfLoading\": \"Shanghai Port\",\n  \"portOfDischarge\": \"Los Angeles Port\"\n}";
        MvcResult lcResult = mockMvc.perform(post("/lcs")
                        .header("Authorization", "Bearer " + token)
                        .param("clientId", String.valueOf(clientId))
                        .param("facilityId", String.valueOf(facilityId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lcPayload))
                .andExpect(status().isOk())
                .andReturn();

        String lcResponseStr = lcResult.getResponse().getContentAsString();
        System.out.println("=== CREATE LC RESPONSE ===\n" + lcResponseStr + "\n==========================");

        // 6. Create Bank Guarantee (using Postman payload format)
        String bgPayload = "{\n  \"bgType\": \"PERFORMANCE_BOND\",\n  \"amount\": 80000.00,\n  \"currency\": \"USD\",\n  \"beneficiaryName\": \"National Power Grid Authority\",\n  \"issueDate\": \"2026-06-14\",\n  \"expiryDate\": \"2027-06-14\",\n  \"claimPeriodDays\": 30,\n  \"termsConditions\": \"Standard performance guarantee terms and conditions.\"\n}";
        MvcResult bgResult = mockMvc.perform(post("/bgs")
                        .header("Authorization", "Bearer " + token)
                        .param("clientId", String.valueOf(clientId))
                        .param("facilityId", String.valueOf(facilityId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bgPayload))
                .andExpect(status().isOk())
                .andReturn();

        String bgResponseStr = bgResult.getResponse().getContentAsString();
        System.out.println("=== CREATE BG RESPONSE ===\n" + bgResponseStr + "\n==========================");

        // 7. Create Export Bill (using Postman payload format)
        String billPayload = "{\n  \"amount\": 120000.00,\n  \"currency\": \"USD\",\n  \"billType\": \"DocumentaryCollection\",\n  \"billDate\": \"2026-06-14\",\n  \"drawerName\": \"Global Trading Co\",\n  \"draweeName\": \"Euro Distribution Center\",\n  \"buyerCountry\": \"DE\",\n  \"maturityDate\": \"2026-09-14\",\n  \"collectionBank\": \"Deutsche Bank AG\"\n}";
        MvcResult billResult = mockMvc.perform(post("/bills")
                        .header("Authorization", "Bearer " + token)
                        .param("clientId", String.valueOf(clientId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(billPayload))
                .andExpect(status().isOk())
                .andReturn();

        String billResponseStr = billResult.getResponse().getContentAsString();
        System.out.println("=== CREATE EXPORT BILL RESPONSE ===\n" + billResponseStr + "\n===================================");
    }
}
