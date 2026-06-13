package com.tradevault.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.LCAmendmentStatus;
import com.tradevault.entity.enums.LCAmendmentType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "lc_amendments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LCAmendment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lc_id", nullable = false)
    private LetterOfCredit lc;

    @Column(name = "amendment_number", nullable = false)
    private Integer amendmentNumber;

    @Column(name = "previous_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal previousAmount;

    @Column(name = "new_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal newAmount;

    @Column(name = "previous_expiry_date", nullable = false)
    private LocalDate previousExpiryDate;

    @Column(name = "new_expiry_date", nullable = false)
    private LocalDate newExpiryDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private LCAmendmentStatus status = LCAmendmentStatus.PENDING_APPROVAL; // PENDING_APPROVAL, APPROVED, REJECTED

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "amendment_type", length = 30)
    private LCAmendmentType amendmentType = LCAmendmentType.OTHER;

    @Column(name = "requested_by_id")
    private Long requestedById;

    @Column(name = "approved_by_id")
    private Long approvedById;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();



}
