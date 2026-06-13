package com.tradevault.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.BGType;
import com.tradevault.entity.enums.BankGuaranteeStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "bank_guarantees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankGuarantee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private CorporateClient client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "credit_facility_id", nullable = false)
    private CreditFacility creditFacility;

    @Column(name = "bg_number", unique = true, nullable = false, length = 50)
    private String bgNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "bg_type", nullable = false, length = 30)
    private BGType bgType; // BID_BOND, PERFORMANCE_BOND, ADVANCE_PAYMENT, FINANCIAL

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency = "USD";

    @Column(name = "beneficiary_name", nullable = false, length = 150)
    private String beneficiaryName;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "claim_period_days")
    private Integer claimPeriodDays;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private BankGuaranteeStatus status = BankGuaranteeStatus.DRAFT; // DRAFT, PENDING_APPROVAL, ACTIVE, CLAIMED, EXPIRED, RELEASED

    @Column(name = "terms_conditions", columnDefinition = "TEXT")
    private String termsConditions;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();



}
