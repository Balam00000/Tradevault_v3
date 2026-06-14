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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Relationship Manager Scope Integration Tests")
@Transactional
class RelationshipManagerScopeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CorporateClientRepository clientRepository;
    @Autowired private CreditFacilityRepository facilityRepository;
    @Autowired private LetterOfCreditRepository lcRepository;
    @Autowired private BankGuaranteeRepository bgRepository;
    @Autowired private ExportBillRepository billRepository;
    @Autowired private CollectionInstructionRepository instructionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User rmUser1;
    private User rmUser2;
    private String rm1Token;
    private String rm2Token;

    private CorporateClient clientA;
    private CorporateClient clientB;

    private CreditFacility facilityA;
    private CreditFacility facilityB;

    private LetterOfCredit lcA;
    private LetterOfCredit lcB;

    private BankGuarantee bgA;
    private BankGuarantee bgB;

    private ExportBill billA;
    private ExportBill billB;

    private CollectionInstruction instructionA;
    private CollectionInstruction instructionB;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Seed RM Users
        rmUser1 = new User("rm_test_1", passwordEncoder.encode("password"), "rm1@test.com", "RM One", "RELATIONSHIP_MANAGER");
        rmUser1.setStatus(UserStatus.ACTIVE);
        rmUser1 = userRepository.save(rmUser1);

        rmUser2 = new User("rm_test_2", passwordEncoder.encode("password"), "rm2@test.com", "RM Two", "RELATIONSHIP_MANAGER");
        rmUser2.setStatus(UserStatus.ACTIVE);
        rmUser2 = userRepository.save(rmUser2);

        rm1Token = getToken("rm_test_1", "password");
        rm2Token = getToken("rm_test_2", "password");

        // 2. Seed Corporate Clients
        clientA = new CorporateClient();
        clientA.setCompanyName("Client A (RM1)");
        clientA.setCountry("US");
        clientA.setTaxId("TAX-A-" + System.currentTimeMillis());
        clientA.setCreditLimit(new BigDecimal("5000000"));
        clientA.setStatus(CorporateClientStatus.ACTIVE);
        clientA.setRelationshipManagerId(rmUser1.getId());
        clientA = clientRepository.save(clientA);

        clientB = new CorporateClient();
        clientB.setCompanyName("Client B (RM2)");
        clientB.setCountry("GB");
        clientB.setTaxId("TAX-B-" + System.currentTimeMillis());
        clientB.setCreditLimit(new BigDecimal("2000000"));
        clientB.setStatus(CorporateClientStatus.ACTIVE);
        clientB.setRelationshipManagerId(rmUser2.getId());
        clientB = clientRepository.save(clientB);

        // 3. Seed Credit Facilities
        facilityA = new CreditFacility();
        facilityA.setClient(clientA);
        facilityA.setFacilityType(CreditFacilityType.LETTER_OF_CREDIT_FACILITY);
        facilityA.setLimitAmount(new BigDecimal("1000000"));
        facilityA.setUtilizedAmount(new BigDecimal("300000"));
        facilityA.setCurrency("USD");
        facilityA.setExpiryDate(LocalDate.now().plusYears(1));
        facilityA.setStatus(CreditFacilityStatus.ACTIVE);
        facilityA = facilityRepository.save(facilityA);

        facilityB = new CreditFacility();
        facilityB.setClient(clientB);
        facilityB.setFacilityType(CreditFacilityType.LETTER_OF_CREDIT_FACILITY);
        facilityB.setLimitAmount(new BigDecimal("800000"));
        facilityB.setUtilizedAmount(new BigDecimal("200000"));
        facilityB.setCurrency("USD");
        facilityB.setExpiryDate(LocalDate.now().plusYears(1));
        facilityB.setStatus(CreditFacilityStatus.ACTIVE);
        facilityB = facilityRepository.save(facilityB);

        // 4. Seed Letters of Credit
        lcA = new LetterOfCredit();
        lcA.setClient(clientA);
        lcA.setCreditFacility(facilityA);
        lcA.setLcNumber("LC-RM1-" + System.currentTimeMillis());
        lcA.setLcType(LCType.SIGHT);
        lcA.setAmount(new BigDecimal("300000"));
        lcA.setCurrency("USD");
        lcA.setApplicantName("Client A Applicant");
        lcA.setBeneficiaryName("Client A Beneficiary");
        lcA.setIssueDate(LocalDate.now());
        lcA.setExpiryDate(LocalDate.now().plusMonths(6));
        lcA.setStatus(LetterOfCreditStatus.ACTIVE);
        lcA = lcRepository.save(lcA);

        lcB = new LetterOfCredit();
        lcB.setClient(clientB);
        lcB.setCreditFacility(facilityB);
        lcB.setLcNumber("LC-RM2-" + System.currentTimeMillis());
        lcB.setLcType(LCType.SIGHT);
        lcB.setAmount(new BigDecimal("200000"));
        lcB.setCurrency("USD");
        lcB.setApplicantName("Client B Applicant");
        lcB.setBeneficiaryName("Client B Beneficiary");
        lcB.setIssueDate(LocalDate.now());
        lcB.setExpiryDate(LocalDate.now().plusMonths(6));
        lcB.setStatus(LetterOfCreditStatus.ACTIVE);
        lcB = lcRepository.save(lcB);

        // 5. Seed Bank Guarantees
        bgA = new BankGuarantee();
        bgA.setClient(clientA);
        bgA.setCreditFacility(facilityA);
        bgA.setBgNumber("BG-RM1-" + System.currentTimeMillis());
        bgA.setBgType(BGType.PERFORMANCE_BOND);
        bgA.setAmount(new BigDecimal("150000"));
        bgA.setCurrency("USD");
        bgA.setBeneficiaryName("BG A Beneficiary");
        bgA.setIssueDate(LocalDate.now());
        bgA.setExpiryDate(LocalDate.now().plusMonths(12));
        bgA.setStatus(BankGuaranteeStatus.ACTIVE);
        bgA = bgRepository.save(bgA);

        bgB = new BankGuarantee();
        bgB.setClient(clientB);
        bgB.setCreditFacility(facilityB);
        bgB.setBgNumber("BG-RM2-" + System.currentTimeMillis());
        bgB.setBgType(BGType.PERFORMANCE_BOND);
        bgB.setAmount(new BigDecimal("100000"));
        bgB.setCurrency("USD");
        bgB.setBeneficiaryName("BG B Beneficiary");
        bgB.setIssueDate(LocalDate.now());
        bgB.setExpiryDate(LocalDate.now().plusMonths(12));
        bgB.setStatus(BankGuaranteeStatus.ACTIVE);
        bgB = bgRepository.save(bgB);

        // 6. Seed Export Bills
        billA = new ExportBill();
        billA.setClient(clientA);
        billA.setBillNumber("EXP-RM1-" + System.currentTimeMillis());
        billA.setAmount(new BigDecimal("80000"));
        billA.setCurrency("USD");
        billA.setStatus(ExportBillStatus.DOCUMENTS_SENT);
        billA.setBillType(ExportBillType.DocumentaryCollection);
        billA.setBillDate(LocalDate.now());
        billA.setDrawerName("Client A");
        billA.setDraweeName("Overseas Buyer A");
        billA.setMaturityDate(LocalDate.now().plusMonths(3));
        billA.setCollectionBank("Global Bank A");
        billA = billRepository.save(billA);

        billB = new ExportBill();
        billB.setClient(clientB);
        billB.setBillNumber("EXP-RM2-" + System.currentTimeMillis());
        billB.setAmount(new BigDecimal("60000"));
        billB.setCurrency("USD");
        billB.setStatus(ExportBillStatus.DOCUMENTS_SENT);
        billB.setBillType(ExportBillType.DocumentaryCollection);
        billB.setBillDate(LocalDate.now());
        billB.setDrawerName("Client B");
        billB.setDraweeName("Overseas Buyer B");
        billB.setMaturityDate(LocalDate.now().plusMonths(3));
        billB.setCollectionBank("Global Bank B");
        billB = billRepository.save(billB);

        // 7. Seed Collection Instructions
        instructionA = new CollectionInstruction();
        instructionA.setClient(clientA);
        instructionA.setInstructionRef("COL-RM1-" + System.currentTimeMillis());
        instructionA.setAmount(new BigDecimal("50000"));
        instructionA.setCurrency("USD");
        instructionA.setTenureType("SIGHT");
        instructionA.setInstructionType(CollectionInstructionType.DP);
        instructionA.setDraweeName("Drawee A");
        instructionA.setInstructionDate(LocalDate.now());
        instructionA.setStatus(CollectionInstructionStatus.PENDING);
        instructionA = instructionRepository.save(instructionA);

        instructionB = new CollectionInstruction();
        instructionB.setClient(clientB);
        instructionB.setInstructionRef("COL-RM2-" + System.currentTimeMillis());
        instructionB.setAmount(new BigDecimal("40000"));
        instructionB.setCurrency("USD");
        instructionB.setTenureType("SIGHT");
        instructionB.setInstructionType(CollectionInstructionType.DP);
        instructionB.setDraweeName("Drawee B");
        instructionB.setInstructionDate(LocalDate.now());
        instructionB.setStatus(CollectionInstructionStatus.PENDING);
        instructionB = instructionRepository.save(instructionB);
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

    @Test
    @DisplayName("GET /corporates: RM1 should only retrieve Client A")
    void getCorporates_filtersByRelationshipManager() throws Exception {
        mockMvc.perform(get("/corporates")
                        .header("Authorization", "Bearer " + rm1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == " + clientA.getId() + ")]").exists())
                .andExpect(jsonPath("$.data[?(@.id == " + clientB.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("GET /corporates/{id}: RM1 should be denied access to Client B")
    void getCorporateById_RM1_deniedForClientB() throws Exception {
        mockMvc.perform(get("/corporates/" + clientB.getId())
                        .header("Authorization", "Bearer " + rm1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /corporates/facilities: RM1 should only retrieve facilities belonging to Client A")
    void getFacilities_filtersByRelationshipManager() throws Exception {
        mockMvc.perform(get("/corporates/facilities")
                        .header("Authorization", "Bearer " + rm1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == " + facilityA.getId() + ")]").exists())
                .andExpect(jsonPath("$.data[?(@.id == " + facilityB.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("GET /lcs: RM1 should only retrieve LCs belonging to Client A")
    void getLCs_filtersByRelationshipManager() throws Exception {
        mockMvc.perform(get("/lcs")
                        .header("Authorization", "Bearer " + rm1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == " + lcA.getId() + ")]").exists())
                .andExpect(jsonPath("$.data[?(@.id == " + lcB.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("GET /lcs/{id}: RM1 should be denied access to Client B's LC")
    void getLcById_RM1_deniedForLcB() throws Exception {
        mockMvc.perform(get("/lcs/" + lcB.getId())
                        .header("Authorization", "Bearer " + rm1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /lcs: RM1 should be denied creating LC for Client B")
    void createLc_RM1_deniedForClientB() throws Exception {
        LetterOfCredit newLc = new LetterOfCredit();
        newLc.setLcType(LCType.SIGHT);
        newLc.setAmount(new BigDecimal("10000"));
        newLc.setApplicantName("Unauthorized Applicant");
        newLc.setBeneficiaryName("Unauthorized Beneficiary");
        newLc.setIssueDate(LocalDate.now());
        newLc.setExpiryDate(LocalDate.now().plusMonths(3));

        mockMvc.perform(post("/lcs")
                        .header("Authorization", "Bearer " + rm1Token)
                        .param("clientId", clientB.getId().toString())
                        .param("facilityId", facilityB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLc)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /bgs: RM1 should only retrieve BGs belonging to Client A")
    void getBGs_filtersByRelationshipManager() throws Exception {
        mockMvc.perform(get("/bgs")
                        .header("Authorization", "Bearer " + rm1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == " + bgA.getId() + ")]").exists())
                .andExpect(jsonPath("$.data[?(@.id == " + bgB.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("GET /bgs/{id}: RM1 should be denied access to Client B's BG")
    void getBgById_RM1_deniedForBgB() throws Exception {
        mockMvc.perform(get("/bgs/" + bgB.getId())
                        .header("Authorization", "Bearer " + rm1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /bills: RM1 should only retrieve bills belonging to Client A")
    void getBills_filtersByRelationshipManager() throws Exception {
        mockMvc.perform(get("/bills")
                        .header("Authorization", "Bearer " + rm1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == " + billA.getId() + ")]").exists())
                .andExpect(jsonPath("$.data[?(@.id == " + billB.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("GET /bills/collections: RM1 should only retrieve collections belonging to Client A")
    void getCollections_filtersByRelationshipManager() throws Exception {
        mockMvc.perform(get("/bills/collections")
                        .header("Authorization", "Bearer " + rm1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == " + instructionA.getId() + ")]").exists())
                .andExpect(jsonPath("$.data[?(@.id == " + instructionB.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("GET /analytics/summary: RM1 should receive client A statistics only")
    void getAnalyticsSummary_RM1_returnsClientAStats() throws Exception {
        // totalExposure RM1: lcA (300000) + bgA (150000) + billA (80000) = 530000
        // totalLimit RM1: facilityA limit (1000000)
        // totalUtilized RM1: facilityA utilized (300000)
        // utilizationRate RM1: 30.00%
        // activeLcsCount RM1: 1
        // activeBgsCount RM1: 1

        mockMvc.perform(get("/analytics/summary")
                        .header("Authorization", "Bearer " + rm1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalExposure").value(530000.00))
                .andExpect(jsonPath("$.data.lcExposure").value(300000.00))
                .andExpect(jsonPath("$.data.bgExposure").value(150000.00))
                .andExpect(jsonPath("$.data.billExposure").value(80000.00))
                .andExpect(jsonPath("$.data.totalLimit").value(1000000.00))
                .andExpect(jsonPath("$.data.totalUtilized").value(300000.00))
                .andExpect(jsonPath("$.data.utilizationRate").value(30.00))
                .andExpect(jsonPath("$.data.activeLcsCount").value(1))
                .andExpect(jsonPath("$.data.activeBgsCount").value(1));
    }
}
