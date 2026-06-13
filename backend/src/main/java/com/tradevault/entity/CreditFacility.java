package com.tradevault.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.CreditFacilityType;
import com.tradevault.entity.enums.CreditFacilityStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "credit_facilities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private CorporateClient client;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "facility_type", nullable = false, length = 50)
    private CreditFacilityType facilityType; // TERM_LOAN, REVOLVING_LINE, LETTER_OF_CREDIT_FACILITY, GUARANTEE_FACILITY

    @Column(name = "limit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "utilized_amount", precision = 15, scale = 2)
    private BigDecimal utilizedAmount = BigDecimal.ZERO;

    @Column(length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private CreditFacilityStatus status = CreditFacilityStatus.ACTIVE;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "collateral_type", length = 100)
    private String collateralType;

    @Column(name = "collateral_value", precision = 15, scale = 2)
    private BigDecimal collateralValue;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

   


}
