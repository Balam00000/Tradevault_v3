package com.tradevault.service;

import com.tradevault.entity.ExportBill;
import com.tradevault.entity.CollectionInstruction;
import java.util.List;

public interface ExportBillService {
    List<ExportBill> getAllBills();
    List<ExportBill> getBillsByClientId(Long clientId);
    List<ExportBill> getBillsByRelationshipManagerId(Long rmId);
    ExportBill getBillById(Long id);
    ExportBill createBill(ExportBill bill, Long clientId, String username);
    ExportBill updateBillStatus(Long id, String status, String trackingStatus, String username);
    
    List<CollectionInstruction> getAllInstructions();
    List<CollectionInstruction> getInstructionsByClientId(Long clientId);
    List<CollectionInstruction> getInstructionsByRelationshipManagerId(Long rmId);
    CollectionInstruction createInstruction(CollectionInstruction instruction, Long clientId, String username);
    CollectionInstruction updateInstructionStatus(Long id, String status, String username);
}
