package com.tradevault.service;

import com.tradevault.entity.*;
import com.tradevault.entity.enums.*;
import com.tradevault.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExportBillServiceTest {

    @Mock
    private ExportBillRepository billRepository;

    @Mock
    private CollectionInstructionRepository instructionRepository;

    @Mock
    private CorporateClientRepository clientRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SanctionsScreeningService sanctionsScreeningService;

    @Mock
    private SanctionsScreeningRepository sanctionsScreeningRepository;

    @InjectMocks
    private ExportBillServiceImpl billService;

    private CorporateClient client;
    private ExportBill bill;
    private CollectionInstruction instruction;

    @BeforeEach
    void setUp() {
        client = new CorporateClient();
        client.setId(1L);
        client.setCompanyName("Exporter Corp");

        bill = new ExportBill();
        bill.setId(10L);
        bill.setClient(client);
        bill.setBillNumber("EXP-BILL-100");
        bill.setAmount(new BigDecimal("50000.00"));
        bill.setStatus(ExportBillStatus.INITIATED);
        bill.setDraweeName("Overseas Drawee");
        bill.setMaturityDate(LocalDate.now().plusDays(30));
        bill.setCollectionBank("Global Bank");

        instruction = new CollectionInstruction();
        instruction.setId(20L);
        instruction.setClient(client);
        instruction.setInstructionRef("COL-INST-200");
        instruction.setAmount(new BigDecimal("75000.00"));
        instruction.setDraweeName("Overseas Drawee");
        instruction.setStatus(CollectionInstructionStatus.PENDING);
    }

    @Test
    void createBill_success() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(billRepository.save(any(ExportBill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExportBill created = billService.createBill(bill, 1L, "testuser");

        assertNotNull(created);
        assertEquals(ExportBillStatus.INITIATED, created.getStatus());
        verify(sanctionsScreeningService).screenEntity("Overseas Drawee", "DRAWER", "EXPORT_BILL", created.getBillNumber());
        verify(auditLogService).log(isNull(), eq("testuser"), eq("EXPORT_BILL_CREATED"), anyString(), isNull());
    }

    @Test
    void updateBillStatus_complianceHold() {
        when(billRepository.findById(10L)).thenReturn(Optional.of(bill));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus("EXP-BILL-100", "FLAGGED"))
                .thenReturn(Collections.singletonList(new SanctionsScreening()));

        assertThrows(IllegalStateException.class, () -> billService.updateBillStatus(10L, ExportBillStatus.ACCEPTED, "DOCS_ACCEPTED", "testuser"));
    }

    @Test
    void updateBillStatus_success() {
        when(billRepository.findById(10L)).thenReturn(Optional.of(bill));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus("EXP-BILL-100", "FLAGGED"))
                .thenReturn(Collections.emptyList());
        when(billRepository.save(any(ExportBill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExportBill updated = billService.updateBillStatus(10L, ExportBillStatus.ACCEPTED, "DOCS_ACCEPTED", "testuser");

        assertEquals(ExportBillStatus.ACCEPTED, updated.getStatus());
        assertEquals("DOCS_ACCEPTED", updated.getTrackingStatus());
        verify(auditLogService).log(isNull(), eq("testuser"), eq("EXPORT_BILL_STATUS_UPDATE"), anyString(), isNull());
    }

    @Test
    void createInstruction_success() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(instructionRepository.save(any(CollectionInstruction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CollectionInstruction created = billService.createInstruction(instruction, 1L, "testuser");

        assertNotNull(created);
        assertEquals(CollectionInstructionStatus.PENDING, created.getStatus());
        verify(sanctionsScreeningService).screenEntity("Overseas Drawee", "DRAWEE", "COLLECTION", created.getInstructionRef());
        verify(auditLogService).log(isNull(), eq("testuser"), eq("COLLECTION_INSTRUCTION_CREATED"), anyString(), isNull());
    }

    @Test
    void updateInstructionStatus_complianceHold() {
        when(instructionRepository.findById(20L)).thenReturn(Optional.of(instruction));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus("COL-INST-200", "FLAGGED"))
                .thenReturn(Collections.singletonList(new SanctionsScreening()));

        assertThrows(IllegalStateException.class, () -> billService.updateInstructionStatus(20L, CollectionInstructionStatus.SETTLED, "testuser"));
    }

    @Test
    void updateInstructionStatus_success() {
        when(instructionRepository.findById(20L)).thenReturn(Optional.of(instruction));
        when(sanctionsScreeningRepository.findByTransactionIdAndStatus("COL-INST-200", "FLAGGED"))
                .thenReturn(Collections.emptyList());
        when(instructionRepository.save(any(CollectionInstruction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CollectionInstruction updated = billService.updateInstructionStatus(20L, CollectionInstructionStatus.SETTLED, "testuser");

        assertEquals(CollectionInstructionStatus.SETTLED, updated.getStatus());
        verify(auditLogService).log(isNull(), eq("testuser"), eq("COLLECTION_INSTRUCTION_UPDATE"), anyString(), isNull());
    }
}
