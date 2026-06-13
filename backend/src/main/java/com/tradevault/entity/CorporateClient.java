package com.tradevault.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.CorporateClientStatus;
import com.tradevault.entity.enums.KYCStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "corporate_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CorporateClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "tax_id", unique = true, nullable = false, length = 50)
    private String taxId;

    @Column(nullable = false, length = 50)
    private String country;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private CorporateClientStatus status = CorporateClientStatus.ACTIVE;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(length = 100)
    private String industry;

    @Column(name = "relationship_manager_id")
    private Long relationshipManagerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "kyc_status", length = 30)
    private KYCStatus kycStatus = KYCStatus.PENDING;

    @Column(name = "credit_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

   
}
