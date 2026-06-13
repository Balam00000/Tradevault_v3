package com.tradevault.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.BGClaimStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "bg_claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BGClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bg_id", nullable = false)
    private BankGuarantee bg;

    @Column(name = "claim_ref", unique = true, nullable = false, length = 50)
    private String claimRef;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "claim_date", nullable = false)
    private LocalDate claimDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private BGClaimStatus status = BGClaimStatus.PENDING; // PENDING, APPROVED, REJECTED

    @Column(name = "claimant_details", length = 150)
    private String claimantDetails;

    @Column(name = "reviewed_by_id")
    private Long reviewedById;

    @Column(name = "payment_details", columnDefinition = "TEXT")
    private String paymentDetails;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();



}
