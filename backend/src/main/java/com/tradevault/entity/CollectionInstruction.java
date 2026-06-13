package com.tradevault.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.CollectionInstructionStatus;
import com.tradevault.entity.enums.CollectionInstructionType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "collection_instructions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CollectionInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private CorporateClient client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bill_id")
    private ExportBill exportBill;

    @Column(name = "instruction_ref", unique = true, nullable = false, length = 50)
    private String instructionRef;

    @Column(name = "collecting_bank_ref", length = 50)
    private String collectingBankRef;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency = "USD";

    @Column(name = "tenure_type", nullable = false, length = 30)
    private String tenureType; // SIGHT, USANCE

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "instruction_type", length = 30)
    private CollectionInstructionType instructionType = CollectionInstructionType.DP; // DAT, DAP, DP

    @Column(name = "drawee_name", nullable = false, length = 150)
    private String draweeName;

    @Column(name = "instruction_date")
    private LocalDate instructionDate;

    @Column(name = "response_date")
    private LocalDate responseDate;

    @Column(name = "remittance_amount", precision = 15, scale = 2)
    private BigDecimal remittanceAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private CollectionInstructionStatus status = CollectionInstructionStatus.PENDING; // PENDING, PROCESSING, COLLECTED, RETURNED

    @Column(name = "instruction_details", columnDefinition = "TEXT")
    private String instructionDetails;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

   


}
