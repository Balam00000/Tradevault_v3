package com.tradevault.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.LCType;
import com.tradevault.entity.enums.LetterOfCreditStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "letters_of_credit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LetterOfCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private CorporateClient client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "credit_facility_id", nullable = false)
    private CreditFacility creditFacility;

    @Column(name = "lc_number", unique = true, nullable = false, length = 50)
    private String lcNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "lc_type", nullable = false, length = 30)
    private LCType lcType; // SIGHT, USANCE

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency = "USD";

    @Column(name = "applicant_name", nullable = false, length = 150)
    private String applicantName;

    @Column(name = "beneficiary_name", nullable = false, length = 150)
    private String beneficiaryName;

    @Column(name = "beneficiary_country", length = 100)
    private String beneficiaryCountry;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private LetterOfCreditStatus status = LetterOfCreditStatus.DRAFT; // DRAFT, IN_REVIEW, APPROVED, REJECTED, ACTIVE, AMENDED, DRAWN, EXPIRED, CLOSED

    @Column(name = "tolerance_percentage", precision = 5, scale = 2)
    private BigDecimal tolerancePercentage = BigDecimal.ZERO;

    @Column(name = "port_of_loading", length = 100)
    private String portOfLoading;

    @Column(name = "port_of_discharge", length = 100)
    private String portOfDischarge;

    @Column(name = "latest_shipment_date")
    private LocalDate latestShipmentDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();



}
