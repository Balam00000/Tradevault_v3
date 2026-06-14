package com.tradevault.service;

import com.tradevault.entity.*;
import com.tradevault.entity.enums.ExportBillStatus;
import com.tradevault.entity.enums.CollectionInstructionStatus;
import com.tradevault.exception.ResourceNotFoundException;
import com.tradevault.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ExportBillService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExportBillService Unit Tests")
class ExportBillServiceTest {

    @Mock private ExportBillRepository billRepository;
    @Mock private CollectionInstructionRepository instructionRepository;
    @Mock private CorporateClientRepository clientRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private SanctionsScreeningService sanctionsScreeningService;
    @Mock private SanctionsScreeningRepository sanctionsScreeningRepository;

    @InjectMocks
    private ExportBillService billService;

    private CorporateClient buildClient(Long id) {
        CorporateClient c = new CorporateClient();
        c.setId(id);
        c.setCompanyName("Export Corp");
        return c;
    }

    // ─── getAllBills ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllBills: should return all bills ordered by creation date")
    void getAllBills_returnsAll() {
        when(billRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(new ExportBill(), new ExportBill()));
        assertThat(billService.getAllBills()).hasSize(2);
    }

    // ─── getBillById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBillById: should throw ResourceNotFoundException when not found")
    void getBillById_notFound_throwsException() {
        when(billRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> billService.getBillById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── createBill ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBill: should create bill with INITIATED status and trigger sanctions screening")
    void createBill_success() {
        CorporateClient client = buildClient(1L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(sanctionsScreeningService.screenEntity(any(), any(), any(), any())).thenReturn(new SanctionsScreening());
        when(billRepository.save(any())).thenAnswer(inv -> {
            ExportBill saved = inv.getArgument(0);
            saved.setId(100L);
            if (saved.getBillNumber() == null) saved.setBillNumber("EXP-BILL-1001");
            return saved;
        });

        ExportBill bill = new ExportBill();
        bill.setAmount(new BigDecimal("150000"));
        bill.setCurrency("USD");
        bill.setDraweeName("Buyer Co Ltd");

        ExportBill result = billService.createBill(bill, 1L, "testuser");

        assertThat(result.getStatus()).isEqualTo(ExportBillStatus.INITIATED);
        assertThat(result.getTrackingStatus()).isEqualTo("DOCUMENTS_PREPARED");
        assertThat(result.getBillNumber()).isNotBlank();
        verify(sanctionsScreeningService).screenEntity(eq("Buyer Co Ltd"), eq("DRAWER"), eq("EXPORT_BILL"), any());
        verify(auditLogService).log(isNull(), eq("testuser"), eq("EXPORT_BILL_CREATED"), any(), isNull());
    }

    @Test
    @DisplayName("createBill: should throw ResourceNotFoundException when client not found")
    void createBill_clientNotFound_throwsException() {
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());
        ExportBill bill = new ExportBill();
        assertThatThrownBy(() -> billService.createBill(bill, 999L, "testuser"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── updateBillStatus ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateBillStatus: should update status and tracking status")
    void updateBillStatus_success() {
        ExportBill bill = new ExportBill();
        bill.setId(100L);
        bill.setBillNumber("EXP-BILL-1001");
        bill.setStatus(ExportBillStatus.INITIATED);

        when(billRepository.findById(100L)).thenReturn(Optional.of(bill));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus(any(), any())).thenReturn(List.of());
        when(billRepository.save(any())).thenReturn(bill);

        ExportBill result = billService.updateBillStatus(100L, "DOCUMENTS_SENT", "SENT_TO_BANK", "ops-user");

        assertThat(result.getStatus()).isEqualTo(ExportBillStatus.DOCUMENTS_SENT);
        assertThat(result.getTrackingStatus()).isEqualTo("SENT_TO_BANK");
        verify(auditLogService).log(isNull(), eq("ops-user"), eq("EXPORT_BILL_STATUS_UPDATE"), any(), isNull());
    }

    @Test
    @DisplayName("updateBillStatus: should throw IllegalStateException when compliance hold exists")
    void updateBillStatus_complianceHold_throws() {
        ExportBill bill = new ExportBill();
        bill.setId(100L);
        bill.setBillNumber("EXP-BILL-FLAGGED");
        when(billRepository.findById(100L)).thenReturn(Optional.of(bill));
        SanctionsScreening flagged = new SanctionsScreening();
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus(any(), eq("FLAGGED"))).thenReturn(List.of(flagged));

        assertThatThrownBy(() -> billService.updateBillStatus(100L, "ACCEPTED", null, "ops-user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLIANCE_HOLD");
    }

    // ─── createInstruction ────────────────────────────────────────────────────

    @Test
    @DisplayName("createInstruction: should create instruction with PENDING status")
    void createInstruction_success() {
        CorporateClient client = buildClient(1L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(sanctionsScreeningService.screenEntity(any(), any(), any(), any())).thenReturn(new SanctionsScreening());
        when(instructionRepository.save(any())).thenAnswer(inv -> {
            CollectionInstruction saved = inv.getArgument(0);
            saved.setId(50L);
            if (saved.getInstructionRef() == null) saved.setInstructionRef("COL-INST-001");
            return saved;
        });

        CollectionInstruction instruction = new CollectionInstruction();
        instruction.setAmount(new BigDecimal("80000"));
        instruction.setDraweeName("Overseas Buyer");
        instruction.setTenureType("SIGHT");

        CollectionInstruction result = billService.createInstruction(instruction, 1L, "testuser");

        assertThat(result.getStatus()).isEqualTo(CollectionInstructionStatus.PENDING);
        assertThat(result.getInstructionRef()).isNotBlank();
        verify(sanctionsScreeningService).screenEntity(eq("Overseas Buyer"), eq("DRAWEE"), eq("COLLECTION"), any());
    }

    // ─── updateInstructionStatus ──────────────────────────────────────────────

    @Test
    @DisplayName("updateInstructionStatus: should update instruction status successfully")
    void updateInstructionStatus_success() {
        CollectionInstruction instruction = new CollectionInstruction();
        instruction.setId(50L);
        instruction.setInstructionRef("COL-INST-001");
        instruction.setStatus(CollectionInstructionStatus.PENDING);
 
        when(instructionRepository.findById(50L)).thenReturn(Optional.of(instruction));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus(any(), any())).thenReturn(List.of());
        when(instructionRepository.save(any())).thenReturn(instruction);
 
        CollectionInstruction result = billService.updateInstructionStatus(50L, "ACKNOWLEDGED_BY_BANK", "ops-user");
 
        assertThat(result.getStatus()).isEqualTo(CollectionInstructionStatus.ACKNOWLEDGED_BY_BANK);
        verify(auditLogService).log(isNull(), eq("ops-user"), eq("COLLECTION_INSTRUCTION_UPDATE"), any(), isNull());
    }

    @Test
    @DisplayName("getBillsByRelationshipManagerId: should return bills belonging to RM clients")
    void getBillsByRelationshipManagerId_returnsBills() {
        when(billRepository.findByClientRelationshipManagerId(101L)).thenReturn(List.of(new ExportBill(), new ExportBill()));
        List<ExportBill> result = billService.getBillsByRelationshipManagerId(101L);
        assertThat(result).hasSize(2);
        verify(billRepository).findByClientRelationshipManagerId(101L);
    }

    @Test
    @DisplayName("getInstructionsByRelationshipManagerId: should return instructions belonging to RM clients")
    void getInstructionsByRelationshipManagerId_returnsInstructions() {
        when(instructionRepository.findByClientRelationshipManagerId(101L)).thenReturn(List.of(new CollectionInstruction(), new CollectionInstruction()));
        List<CollectionInstruction> result = billService.getInstructionsByRelationshipManagerId(101L);
        assertThat(result).hasSize(2);
        verify(instructionRepository).findByClientRelationshipManagerId(101L);
    }
}
